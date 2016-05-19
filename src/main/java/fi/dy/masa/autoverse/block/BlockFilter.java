package fi.dy.masa.autoverse.block;

import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
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
    protected static final int NUM_TIERS = 3;
    protected static final PropertyInteger TIER = PropertyInteger.create("tier", 0, NUM_TIERS - 1);
    protected final Class <? extends TileEntityAutoverseInventory> teClass;

    public BlockFilter(String name, float hardness, int harvestLevel, Material material, Class <? extends TileEntityAutoverseInventory> teClass)
    {
        super(name, hardness, harvestLevel, material);

        this.teClass = teClass;

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TIER, 0)
                .withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { TIER, FACING });
    }

    @Override
    public void createUnlocalizedNames()
    {
        String[] names = new String[NUM_TIERS];

        for (int i = 0; i < NUM_TIERS; i++)
        {
            names[i] = this.blockName + "_" + i;
        }

        this.unlocalizedNames = names;
    }

    @Override
    public String[] getItemBlockVariantStrings()
    {
        String[] strings = new String[NUM_TIERS];

        for (int i = 0; i < NUM_TIERS; i++)
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
            state = state.withProperty(FACING, tefi.getFacing()).withProperty(TIER, tefi.getFilterTier());
        }

        return state;
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
            ((TileEntityFilter)te).setFilterTier(stack.getMetadata());
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list)
    {
        for (int meta = 0; meta < NUM_TIERS; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        return super.getComparatorInputOverride(blockState, worldIn, pos);
    }
}
