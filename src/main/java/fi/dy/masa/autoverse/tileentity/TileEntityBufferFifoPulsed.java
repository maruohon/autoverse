package fi.dy.masa.autoverse.tileentity;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
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
    protected boolean pushItemsToAdjacentInventory(IItemHandler invSrc, int slot, BlockPos pos, EnumFacing side, boolean spawnInWorld)
    {
        boolean success = super.pushItemsToAdjacentInventory(invSrc, slot, pos, side, spawnInWorld);

        if (success)
        {
            this.itemHandlerFifo.advancePositions();
        }

        return success;
    }

    private class ItemHandlerWrapperFifoPulsed extends ItemHandlerWrapperFifo
    {
        public ItemHandlerWrapperFifoPulsed(ItemStackHandlerTileEntity baseHandler)
        {
            super(baseHandler);

            // Start extracting from slot 1, so that slot 0 will be extracted last (ie. after one full cycle through the inventory)
            this.extractSlot = Math.min(baseHandler.getSlots(), 1);
        }

        public void advancePositions()
        {
            if (++this.extractSlot >= this.baseHandler.getSlots())
            {
                this.extractSlot = 0;
            }

            if (++this.insertSlot >= this.baseHandler.getSlots())
            {
                this.insertSlot = 0;
            }
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
