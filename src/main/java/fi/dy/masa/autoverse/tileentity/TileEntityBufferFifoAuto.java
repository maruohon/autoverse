package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityBufferFifoAuto extends TileEntityBufferFifo
{
    public TileEntityBufferFifoAuto()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_AUTO);

        this.spawnItemsInWorld = false;
        this.delay = 4;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        if (this.getRedstoneState() == false)
        {
            boolean success = this.pushItemsToAdjacentInventory(this.itemHandlerExternal, 0,
                    this.getFrontPosition(), this.getOppositeFacing(), this.spawnItemsInWorld);

            if (success)
            {
                this.scheduleBlockUpdate(this.delay, false);
            }
        }
    }

    @Override
    protected void onRedstoneChange(boolean state)
    {
        // NO-OP
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        if (this.getRedstoneState() == false)
        {
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        if (this.getRedstoneState() == false)
        {
            this.scheduleBlockUpdate(this.delay, false);
        }
    }
}
