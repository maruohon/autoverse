package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperFilterSequential extends ItemHandlerWrapperFilter
{
    private final ItemStackHandlerTileEntity inventoryFilteredBuffer;
    private int filterLength;
    private int outputPosition;
    private int subState = 0;

    public ItemHandlerWrapperFilterSequential(
            ItemStackHandlerTileEntity inventoryInput,
            ItemStackHandlerTileEntity inventoryOutFiltered,
            ItemStackHandlerTileEntity inventoryOutNormal,
            ItemStackHandlerTileEntity inventoryFilteredBuffer)
    {
        super(inventoryInput, inventoryOutFiltered, inventoryOutNormal);

        this.inventoryFilteredBuffer = inventoryFilteredBuffer;
    }

    @Override
    protected void onFullyConfigured()
    {
        super.onFullyConfigured();

        this.filterLength = this.getFilterSequence().getCurrentSequenceLength();
        this.inventoryFilteredBuffer.setInventorySize(this.filterLength);
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.outputPosition = 0;
    }

    @Override
    protected void onResetFlushComplete()
    {
        this.subState = 1; // Change to flush the filtered items buffer
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        return this.subState == 0 ? this.sortItem(stack) : this.outputItems();
    }

    @Override
    protected boolean onScheduledTick()
    {
        return this.subState == 1 ? this.outputItems() : false;
    }

    @Override
    protected boolean resetFlushItems()
    {
        if (this.subState == 0)
        {
            return super.resetFlushItems();
        }
        else
        {
            return this.flushFilterBufferItems();
        }
    }

    private boolean sortItem(ItemStack stack)
    {
        List<Integer> matchingSlots = this.getMatchingSlots(stack);

        if (matchingSlots != null)
        {
            for (int slot : matchingSlots)
            {
                if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryFilteredBuffer, slot) == InvResult.MOVED_ALL)
                {
                    if (++this.outputPosition >= this.filterLength)
                    {
                        this.outputPosition = 0;
                        this.subState = 1;
                    }

                    return true;
                }
            }
        }

        // If the item didn't fit or belong to the filtered buffer, move it to the normal output
        return this.moveInputItemToOutput();
    }

    /**
     * Move items from a buffer to the appropriate outputs, if any, before
     * returning to the sort mode and continuing to handle more input items.
     * @return
     */
    private boolean outputItems()
    {
        if (this.inventoryFilteredBuffer.getStackInSlot(this.outputPosition).isEmpty() ||
            InventoryUtils.tryMoveEntireStackOnly(this.inventoryFilteredBuffer, this.outputPosition, this.inventoryFilteredOut, 0) == InvResult.MOVED_ALL)
        {
            // All items moved, return back to the sorting mode
            if (++this.outputPosition >= this.filterLength)
            {
                this.outputPosition = 0;
                this.subState = 0;
            }

            return true;
        }

        return false;
    }

    /**
     * Move all items from internal buffers to the output, before returning
     * to the programming phase for the next operation cycle.
     * @return
     */
    private boolean flushFilterBufferItems()
    {
        boolean success = false;

        while (this.outputPosition < this.filterLength)
        {
            if (this.inventoryFilteredBuffer.getStackInSlot(this.outputPosition).isEmpty())
            {
                this.outputPosition++;
            }
            else if (InventoryUtils.tryMoveEntireStackOnly(this.inventoryFilteredBuffer, this.outputPosition, this.getOutputInventory(), 0) == InvResult.MOVED_ALL)
            {
                this.outputPosition++;
                success = true;
                break;
            }
            else
            {
                break;
            }
        }

        // All items moved, return back to the programming phase
        if (this.outputPosition >= this.filterLength)
        {
            this.outputPosition = 0;
            this.subState = 0;
            this.setState(State.CONFIGURE);
            return true;
        }

        return success;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.subState = tag.getByte("SubState");
        this.outputPosition = tag.getByte("OutPos");
        this.filterLength = tag.getByte("FilterLength");
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("SubState", (byte) this.subState);
        tag.setByte("OutPos", (byte) this.outputPosition);
        tag.setByte("FilterLength", (byte) this.filterLength);

        return tag;
    }
}
