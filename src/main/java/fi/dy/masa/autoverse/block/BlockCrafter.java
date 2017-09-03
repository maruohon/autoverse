package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockMachineSlimBase;
import fi.dy.masa.autoverse.tileentity.TileEntityCrafter;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockCrafter extends BlockMachineSlimBase
{
    public BlockCrafter(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(SLIM, false)
                .withProperty(FACING, DEFAULT_FACING));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, SLIM });
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return 0;
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World worldIn, IBlockState state)
    {
        return new TileEntityCrafter();
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        // TODO
        return super.getComparatorInputOverride(blockState, worldIn, pos);
    }
}
