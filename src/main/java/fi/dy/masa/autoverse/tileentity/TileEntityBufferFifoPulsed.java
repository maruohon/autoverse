package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperSelective;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityBufferFifoPulsed extends TileEntityBufferFifo
{
    public TileEntityBufferFifoPulsed()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_PULSED);
        // Start extracting from slot 1, so that slot 0 will be extracted last (ie. after one full cycle through the inventory)
        this.extractSlot = 1;
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, NUM_SLOTS, 1, false, "Items", this);
        this.itemHandlerExternal = new ItemHandlerWrapperFifoPulsed(this.getBaseItemHandler());
    }

    @Override
    public void onBlockTick(IBlockState state, Random rand)
    {
        super.onBlockTick(state, rand);

        if (++this.insertSlot >= this.itemHandlerBase.getSlots())
        {
            this.insertSlot = 0;
        }

        if (++this.extractSlot >= this.itemHandlerBase.getSlots())
        {
            this.extractSlot = 0;
        }
    }

    private class ItemHandlerWrapperFifoPulsed extends ItemHandlerWrapperSelective
    {
        public ItemHandlerWrapperFifoPulsed(IItemHandler baseHandler)
        {
            super(baseHandler);
        }

        @Override
        public int getSlots()
        {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return super.getStackInSlot(TileEntityBufferFifoPulsed.this.extractSlot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            return super.insertItem(TileEntityBufferFifoPulsed.this.insertSlot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return super.extractItem(TileEntityBufferFifoPulsed.this.extractSlot, amount, simulate);
        }
    }
}
