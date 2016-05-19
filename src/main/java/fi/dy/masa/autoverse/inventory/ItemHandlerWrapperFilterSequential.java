package fi.dy.masa.autoverse.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class ItemHandlerWrapperFilterSequential extends ItemHandlerWrapperFilter
{
    protected int filterPosition;

    public ItemHandlerWrapperFilterSequential(IItemHandler resetItems, IItemHandler filterItems,
            IItemHandler filteredOut, IItemHandler othersOut, TileEntityFilter te)
    {
        super(resetItems, filterItems, filteredOut, othersOut, te);
    }

    public int getFilterPosition()
    {
        return this.filterPosition;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag)
    {
        super.deserializeNBT(tag);

        this.filterPosition = tag.getByte("FilterPos");
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = super.serializeNBT();
        tag.setByte("FilterPos", (byte)this.filterPosition);
        return tag;
    }

    @Override
    protected ItemStack sortItem(ItemStack stack, boolean simulate)
    {
        int sizeOrig = stack.stackSize;

        if (simulate == false)
        {
            this.checkForSequenceMatch(stack, this.resetSequenceBuffer, this.resetItems);
        }

        if (InventoryUtils.areItemStacksEqual(stack, this.filterItems.getStackInSlot(this.filterPosition)) == true)
        {
            // Only accept one item at a time, so that the sequence is preserved
            if (stack.stackSize > 1)
            {
                ItemStack stackTmp = stack.copy();
                stackTmp.stackSize = 1;
                stackTmp = InventoryUtils.tryInsertItemStackToInventory(this.filteredOut, stackTmp, simulate);

                if (stackTmp == null)
                {
                    stack = stack.copy();
                    stack.stackSize--;
                }
            }
            else
            {
                stack = InventoryUtils.tryInsertItemStackToInventory(this.filteredOut, stack, simulate);
            }

            if (simulate == false && (stack == null || stack.stackSize < sizeOrig))
            {
                if (++this.filterPosition >= this.filterItems.getSlots())
                {
                    this.filterPosition = 0;
                }
            }

            return stack;
        }

        return InventoryUtils.tryInsertItemStackToInventory(this.othersOut, stack, simulate);
    }

    @Override
    protected void reset(IItemHandlerModifiable invSequenceBuffer, IItemHandler invReferenceSequence)
    {
        //System.out.printf("=== RESET ===\n");
        // Dump the reset sequence inventory and the filter item inventory into the output inventory
        InventoryUtils.tryMoveAllItems(invReferenceSequence, this.othersOut);
        InventoryUtils.tryMoveAllItems(this.filterItems, this.othersOut);
        this.mode = EnumMode.RESET;
        this.seqBufWrite = 0;

        for (int i = 0; i < invSequenceBuffer.getSlots(); i++)
        {
            invSequenceBuffer.setStackInSlot(i, null);
        }

        this.filterPosition = 0;
    }
}
