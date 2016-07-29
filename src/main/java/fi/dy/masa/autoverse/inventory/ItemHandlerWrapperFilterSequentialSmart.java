package fi.dy.masa.autoverse.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class ItemHandlerWrapperFilterSequentialSmart extends ItemHandlerWrapperFilterSequential
{
    protected int firstEmptySlot;
    protected int lastEmptySlot;

    public ItemHandlerWrapperFilterSequentialSmart(IItemHandler resetItems, IItemHandler filterItems,
            IItemHandler filteredOut, IItemHandler othersOut, TileEntityFilter te)
    {
        super(resetItems, filterItems, filteredOut, othersOut, te);

        this.resetSlotIndices();
        this.updateSlotIndices();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        if (this.mode == EnumMode.OUTPUT_ITEMS)
        {
            if (InventoryUtils.isInventoryEmpty(this.filteredOut))
            {
                this.mode = EnumMode.SORT_ITEMS;
            }
            else
            {
                if (simulate == false)
                {
                    this.te.scheduleBlockTick(1, false);
                }

                return stack;
            }
        }

        return super.insertItem(slot, stack, simulate);
    }

    @Override
    protected ItemStack sortItem(ItemStack stack, boolean simulate)
    {
        int sizeOrig = stack.stackSize;

        if (simulate == false)
        {
            this.checkForSequenceMatch(stack);
        }

        int slot = this.getValidPositionForItem(stack);

        if (slot >= 0)
        {
            // Only accept one item at a time, so that the sequence is preserved
            if (stack.stackSize > 1)
            {
                ItemStack stackTmp = stack.copy();
                stackTmp.stackSize = 1;
                stackTmp = this.filteredOut.insertItem(slot, stackTmp, simulate);

                if (stackTmp == null)
                {
                    stack = stack.copy();
                    stack.stackSize--;
                }
            }
            else
            {
                stack = this.filteredOut.insertItem(slot, stack, simulate);
            }

            if (simulate == false && (stack == null || stack.stackSize < sizeOrig))
            {
                this.updateSlotIndices();
            }

            return stack;
        }

        return InventoryUtils.tryInsertItemStackToInventoryStackFirst(this.othersOut, stack, simulate);
    }

    protected int getValidPositionForItem(ItemStack stack)
    {
        for (int slot = this.firstEmptySlot; slot <= this.lastEmptySlot; slot++)
        {
            if (this.filteredOut.getStackInSlot(slot) == null &&
                InventoryUtils.areItemStacksEqual(stack, this.filterItems.getStackInSlot(slot)) == true)
            {
                return slot;
            }
        }

        return -1;
    }

    protected void updateSlotIndices()
    {
        int first = this.filterItems.getSlots() - 1;
        int last = 0;
        int numEmpty = 0;

        for (int slot = this.firstEmptySlot; slot <= this.lastEmptySlot; slot++)
        {
            if (this.filteredOut.getStackInSlot(slot) == null)
            {
                if (slot <= first)
                {
                    first = slot;
                }

                if (slot >= last)
                {
                    last = slot;
                }

                numEmpty++;
            }
        }

        //System.out.printf("updateSlotIndices: first: %d last: %d numEmpty: %d size: %d\n", first, last, numEmpty, this.filterItems.getSlots());

        if (numEmpty == 0)
        {
            this.mode = EnumMode.OUTPUT_ITEMS;
            this.resetSlotIndices();
            return;
        }

        this.firstEmptySlot = first;
        this.lastEmptySlot = last;
    }

    protected void resetSlotIndices()
    {
        this.firstEmptySlot = 0;
        this.lastEmptySlot = this.filterItems.getSlots() - 1;
    }

    @Override
    protected void reset()
    {
        super.reset();

        this.resetSlotIndices();
    }
}
