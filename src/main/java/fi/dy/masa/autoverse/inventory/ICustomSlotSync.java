package fi.dy.masa.autoverse.inventory;

import net.minecraft.item.ItemStack;

public interface ICustomSlotSync
{
    /**
     * Puts an ItemStack into a slot. The typeId indicates which type of inventory or slot
     * this transaction is for, in case there are several different types. 
     * @param typeId
     * @param slotNum
     * @param stack
     */
    public void putCustomStack(int typeId, int slotNum, ItemStack stack);
}
