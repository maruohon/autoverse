package fi.dy.masa.autoverse.inventory.wrapper;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerWrapperInsertOnly implements IItemHandler
{
    protected final IItemHandler parent;

    public ItemHandlerWrapperInsertOnly(IItemHandler baseInventory)
    {
        this.parent = baseInventory;
    }

    @Override
    public int getSlots()
    {
        return this.parent.getSlots();
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return this.parent.getSlotLimit(slot);
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return this.parent.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return this.parent.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return ItemStack.EMPTY;
    }
}
