package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;

public class BlockFilter extends BlockAutoverseInventory
{
    public static final PropertyDirection FACING_FILTER = PropertyDirection.create("facing_filter");
    public static final PropertyInteger TIER = PropertyInteger.create("tier", 0, 4);
    protected final Class <? extends TileEntityAutoverseInventory> teClass;
    protected int tiers;

    public BlockFilter(String name, float hardness, int harvestLevel, Material material, Class <? extends TileEntityAutoverseInventory> teClass)
    {
        this(name, hardness, harvestLevel, material, teClass, 5);
    }

    protected BlockFilter(String name, float hardness, int harvestLevel, Material material, Class <? extends TileEntityAutoverseInventory> teClass, int tiers)
    {
        super(name, hardness, harvestLevel, material);

        this.teClass = teClass;
        this.tiers = tiers;
        this.unlocalizedNames = this.createUnlocalizedNames(); // Needs to happen after setting the tiers count

        this.setFilterDefaultState();
    }

    protected void setFilterDefaultState()
    {
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(FACING_FILTER, EnumFacing.EAST)
                .withProperty(TIER, 0));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING_FILTER, TIER });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TIER, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TIER);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFilter)
        {
            TileEntityFilter tefi = (TileEntityFilter)te;
            state = state
                    .withProperty(FACING,           tefi.getFacing())
                    .withProperty(FACING_FILTER,    tefi.getFilterOutRelativeFacing())
                    .withProperty(TIER,             tefi.getFilterTier());
        }

        return state;
    }

    @Override
    protected String[] createUnlocalizedNames()
    {
        String[] names = new String[this.tiers];

        for (int i = 0; i < this.tiers; i++)
        {
            names[i] = this.blockName + "_" + i;
        }

        return names;
    }

    @Override
    public String[] getItemBlockVariantStrings()
    {
        String[] strings = new String[this.tiers];

        for (int i = 0; i < this.tiers; i++)
        {
            strings[i] = String.valueOf(i);
        }

        return strings;
    }

    @Override
    public TileEntity createTileEntity(World worldIn, IBlockState state)
    {
        try
        {
            return this.teClass.newInstance();
        }
        catch (Throwable e)
        {
            Autoverse.logger.fatal("BlockFilter: Failed to create a TileEntity for " + this.teClass.getSimpleName());
        }

        return null;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState iBlockState)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFilter)
        {
            ((TileEntityFilter) te).dropInventories();
            worldIn.updateComparatorOutputLevel(pos, this);
        }

        worldIn.removeTileEntity(pos);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, EnumFacing side, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, side, state, placer, stack);

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFilter)
        {
            int tier = MathHelper.clamp(stack.getMetadata() & 0xF, 0, this.tiers - 1);
            ((TileEntityFilter) te).setFilterTier(tier);

            EnumFacing filterFacing = EnumFacing.getDirectionFromEntityLiving(pos, placer);
            if (filterFacing.getAxis().isVertical())
            {
                filterFacing = placer.getHorizontalFacing().rotateY();
            }
            else
            {
                filterFacing = filterFacing.rotateYCCW();
            }

            /*if (placer.isSneaking())
            {
                filterFacing = filterFacing.getOpposite();
            }*/

            ((TileEntityFilter) te).setFilterOutputSide(filterFacing);
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        ItemStack stack = playerIn.getHeldItem(hand);

        if (stack.isEmpty() == false && stack.getItem() == Items.STICK)
        {
            if (worldIn.isRemote == false)
            {
                TileEntity te = worldIn.getTileEntity(pos);
                if (te instanceof TileEntityFilter)
                {
                    ((TileEntityFilter) te).setFilterOutputSide(side);
                    worldIn.notifyBlockUpdate(pos, state, state, 3);
                }
            }

            return true;
        }

        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, side, hitX, hitY, hitZ);
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
