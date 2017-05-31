package fi.dy.masa.autoverse.inventory.wrapper;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.NonNullList;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class ItemHandlerWrapperCraftingOutput extends ItemStackHandlerBasic
{
    private InventoryCraftingWrapper inventoryCrafting;

    public ItemHandlerWrapperCraftingOutput()
    {
        super(1);
    }

    public void setCraftingInventory(InventoryCraftingWrapper inventoryCrafting)
    {
        this.inventoryCrafting = inventoryCrafting;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        return false;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        ItemStack stack = super.extractItem(slot, amount, simulate);

        if (simulate == false)
        {
            this.onCraft();
        }

        return stack;
    }

    private void onCraft()
    {
        NonNullList<ItemStack> remainingItems = CraftingManager.getInstance()
                .getRemainingItems(this.inventoryCrafting, this.inventoryCrafting.getWorld());

        for (int i = 0; i < remainingItems.size(); i++)
        {
            ItemStack stackInSlot = this.inventoryCrafting.getStackInSlot(i);
            ItemStack remainingItemsInSlot = remainingItems.get(i);

            if (stackInSlot.isEmpty() == false)
            {
                this.inventoryCrafting.decrStackSize(i, 1);
                stackInSlot = this.inventoryCrafting.getStackInSlot(i);
            }

            if (remainingItemsInSlot.isEmpty() == false)
            {
                if (stackInSlot.isEmpty())
                {
                    this.inventoryCrafting.setInventorySlotContents(i, remainingItemsInSlot);
                }
                else if (InventoryUtils.areItemStacksEqual(stackInSlot, remainingItemsInSlot))
                {
                    remainingItemsInSlot.grow(stackInSlot.getCount());
                    this.inventoryCrafting.setInventorySlotContents(i, remainingItemsInSlot);
                }
                else
                {
                    //EntityUtils.dropItemStacksInWorld(this.inventoryCrafting.getWorld(), pos, remainingItemsInSlot, -1, true);
                }
            }
        }
    }
}
