package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockFilter extends BlockAutoverseInventory
{
    private static final int NUM_TIERS = 5;
    protected static final PropertyDirection FACING_FILTER = PropertyDirection.create("facing_filter");
    private static final PropertyInteger TIER = PropertyInteger.create("tier", 0, NUM_TIERS - 1);
    protected int tiers;

    public BlockFilter(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        this(name, hardness, resistance, harvestLevel, material, NUM_TIERS);
    }

    protected BlockFilter(String name, float hardness, float resistance, int harvestLevel, Material material, int tiers)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.tiers = tiers;
        this.setDefaultState();

        // This needs to happen after setting the tiers count
        this.unlocalizedNames = this.generateUnlocalizedNames();
    }

    protected PropertyInteger getTierProperty()
    {
        return TIER;
    }

    protected void setDefaultState()
    {
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(FACING_FILTER, EnumFacing.EAST)
                .withProperty(this.getTierProperty(), 0));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING_FILTER, this.getTierProperty() });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(this.getTierProperty(), meta);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(this.getTierProperty());
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World worldIn, IBlockState state)
    {
        TileEntityFilter te = new TileEntityFilter();
        te.setFilterTier(state.getValue(this.getTierProperty()));
        return te;
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        String[] names = new String[this.tiers];

        for (int i = 0; i < this.tiers; i++)
        {
            names[i] = this.blockName + "_" + i;
        }

        return names;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileEntityFilter te = getTileEntitySafely(world, pos, TileEntityFilter.class);

        if (te != null)
        {
            state = state.withProperty(FACING, te.getFacing())
                         .withProperty(FACING_FILTER, te.getFilterOutRelativeFacing());
        }

        return state;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntityFilter te = getTileEntitySafely(world, pos, TileEntityFilter.class);

        if (te != null)
        {
            EnumFacing filterFacing = EnumFacing.getDirectionFromEntityLiving(pos, placer);

            if (filterFacing.getAxis().isVertical())
            {
                filterFacing = placer.getHorizontalFacing().rotateY();
            }
            else
            {
                filterFacing = filterFacing.rotateYCCW();
            }

            te.setFilterOutputSide(filterFacing);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (int meta = 0; meta < this.tiers; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        // TODO
        return super.getComparatorInputOverride(blockState, worldIn, pos);
    }
}
