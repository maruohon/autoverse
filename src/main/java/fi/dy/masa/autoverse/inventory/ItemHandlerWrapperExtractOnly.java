package fi.dy.masa.autoverse.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerWrapperExtractOnly implements IItemHandler
{
    protected final IItemHandler parent;

    public ItemHandlerWrapperExtractOnly(IItemHandler baseInventory)
    {
        this.parent = baseInventory;
    }

    @Override
    public int getSlots()
    {
        return this.parent.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return this.parent.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return this.parent.extractItem(slot, amount, simulate);
    }
}
