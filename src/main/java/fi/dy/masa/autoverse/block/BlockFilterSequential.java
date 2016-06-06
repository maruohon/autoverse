package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;

public class BlockFilterSequential extends BlockFilter
{
    protected static final int NUM_TIERS = 3;
    public static final PropertyInteger TIER = PropertyInteger.create("tier", 0, NUM_TIERS - 1);

    public BlockFilterSequential(String name, float hardness, int harvestLevel, Material material, Class <? extends TileEntityAutoverseInventory> teClass)
    {
        super(name, hardness, harvestLevel, material, teClass, NUM_TIERS);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(TIER, 0));
    }

    protected void setFilterDefaultState()
    {
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(TIER, 0));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, TIER });
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
}
