package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencerProgrammable;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockSequencerProgrammable extends BlockAutoverseInventory
{
    public BlockSequencerProgrammable(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, DEFAULT_FACING));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return 0;
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World worldIn, IBlockState state)
    {
        return new TileEntitySequencerProgrammable();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        TileEntitySequencerProgrammable te = getTileEntitySafely(world, pos, TileEntitySequencerProgrammable.class);

        if (te != null)
        {
            te.dropInventories();
            world.updateComparatorOutputLevel(pos, this);
        }

        world.removeTileEntity(pos);
    }
}
