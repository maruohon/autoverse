package fi.dy.masa.autoverse.inventory.wrapper;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class ItemHandlerWrapperCreative extends ItemHandlerWrapperSelective
{
    private final TileEntityAutoverseInventory te;

    public ItemHandlerWrapperCreative(IItemHandler baseHandler, TileEntityAutoverseInventory te)
    {
        super(baseHandler);

        this.te = te;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        if (this.te.isCreative())
        {
            return stack;
        }

        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (this.te.isCreative())
        {
            return super.extractItem(slot, amount, true);
        }

        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        return super.isItemValidForSlot(slot, stack) && this.te.isCreative() == false;
    }
}
