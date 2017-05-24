package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFifo;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityBufferFifoPulsed extends TileEntityBufferFifo
{
    private ItemHandlerWrapperFifoPulsed itemHandlerFifo;

    public TileEntityBufferFifoPulsed()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_PULSED);

        this.spawnItemsInWorld = false;
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, NUM_SLOTS, 1, false, "Items", this);
        this.itemHandlerFifo = new ItemHandlerWrapperFifoPulsed(this.itemHandlerBase);
        this.itemHandlerExternal = this.itemHandlerFifo;
    }

    @Override
    public ItemHandlerWrapperFifoPulsed getFifoInventory()
    {
        return this.itemHandlerFifo;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        super.onScheduledBlockUpdate(world, pos, state, rand);

        this.itemHandlerFifo.advanceInsertPosition();
    }

    @Override
    protected boolean pushItemsToAdjacentInventory(IItemHandler invSrc, int slot, BlockPos pos, EnumFacing side, boolean spawnInWorld)
    {
        // First try to move out the item from the extract position, if there is one
        //if (this.itemHandlerBase.getStackInSlot(this.itemHandlerFifo.getExtractSlot()).isEmpty())
        if (this.itemHandlerFifo.getStackInSlot(0).isEmpty() ||
            super.pushItemsToAdjacentInventory(invSrc, slot, pos, side, spawnInWorld))
        {
            this.itemHandlerFifo.advanceExtractPosition();
            return true;
        }

        return false;
    }

    private class ItemHandlerWrapperFifoPulsed extends ItemHandlerWrapperFifo
    {
        public ItemHandlerWrapperFifoPulsed(ItemStackHandlerTileEntity baseHandler)
        {
            super(baseHandler);

            // Start extracting from slot 1, so that there must be one full cycle through
            // the inventory before items start to leave.
            this.extractSlot = Math.min(baseHandler.getSlots(), 1);
        }

        private void advanceExtractPosition()
        {
            // The extract position can never catch up to the insert position,
            // because the insert position will _always_ successfully move,
            // unless THAT ONE catches up to the extract position.
            this.extractSlot = this.getNextPosition(this.extractSlot);
            TileEntityBufferFifoPulsed.this.markDirty();
        }

        private void advanceInsertPosition()
        {
            int next = this.getNextPosition(this.insertSlot);

            if (next != this.extractSlot)
            {
                this.insertSlot = next;
                TileEntityBufferFifoPulsed.this.markDirty();
            }
        }

        private int getNextPosition(int position)
        {
            if (++position >= this.baseHandler.getSlots())
            {
                position = 0;
            }

            return position;
        }

        @Override
        protected void onSuccessfulInsert()
        {
            // NO-OP
        }

        @Override
        protected void onSuccessfulExtract()
        {
            // NO-OP
        }
    }
}
