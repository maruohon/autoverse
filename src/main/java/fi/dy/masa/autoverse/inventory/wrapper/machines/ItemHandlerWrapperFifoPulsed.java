package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public class ItemHandlerWrapperFifoPulsed extends ItemHandlerWrapperFifo
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
    public ItemStack getStackInSlot(int slot)
    {
        return super.getStackInSlot(this.extractSlot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return super.insertItem(this.insertSlot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return super.extractItem(this.extractSlot, amount, simulate);
    }
}
