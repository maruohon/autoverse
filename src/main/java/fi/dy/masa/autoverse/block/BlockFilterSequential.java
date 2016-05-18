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
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;

public class BlockFilterSequential extends BlockAutoverseInventory
{
    public static final PropertyInteger TIER = PropertyInteger.create("tier", 0, 1);

    public BlockFilterSequential(String name, float hardness, int harvestLevel, Material material)
    {
        super(name, hardness, harvestLevel, material);

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
    public String[] getUnlocalizedNames()
    {
        return new String[] {
                ReferenceNames.NAME_TILE_ENTITY_FILTER_SEQUENTTIAL + "_0",
                ReferenceNames.NAME_TILE_ENTITY_FILTER_SEQUENTTIAL + "_1"
        };
    }

    @Override
    public String[] getItemBlockVariantStrings()
    {
        return new String[] { "0", "1" };
    }

    @Override
    public TileEntity createTileEntity(World worldIn, IBlockState state)
    {
        /*EnumMachineType type = state.getValue(TYPE);
        switch (type)
        {
            case FILTER: return new TileEntityFilter();
        }*/

        return new TileEntityFilterSequential();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TIER, meta & 0x1);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TIER) & 0x1;
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

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list)
    {
        for (int meta = 0; meta < 2; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        /*EnumMachineType type = blockState.getValue(TYPE);
        if (type == EnumMachineType.FILTER)
        {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityFilter)
            {
                return InventoryUtils.calcRedstoneFromInventory(((TileEntityFilter)te).getBaseItemHandler());
            }

            return 0;
        }*/

        return super.getComparatorInputOverride(blockState, worldIn, pos);
    }
}
