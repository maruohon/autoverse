package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperFilterSequential extends ItemHandlerWrapperFilter
{
    private final ItemStackHandlerTileEntity inventoryFilteredBuffer;
    private final IItemHandler inventoryFilteredOut;
    private int filterLength;
    private int outputPosition;

    public ItemHandlerWrapperFilterSequential(
            ItemStackHandlerTileEntity inventoryInput,
            ItemStackHandlerTileEntity inventoryOutFiltered,
            ItemStackHandlerTileEntity inventoryOutNormal,
            ItemStackHandlerTileEntity inventoryFilteredBuffer)
    {
        super(inventoryInput, inventoryOutFiltered, inventoryOutNormal);

        this.inventoryFilteredOut = inventoryOutFiltered;
        this.inventoryFilteredBuffer = inventoryFilteredBuffer;
        this.filterLength = this.getFilterSequence().getMaxLength();
    }

    @Override
    protected void onFilterConfigured()
    {
        super.onFilterConfigured();

        this.filterLength = this.getFilterSequence().getCurrentSequenceLength();
        this.inventoryFilteredBuffer.setInventorySize(this.filterLength);
    }

    @Override
    protected boolean sortItem()
    {
        if (this.matchingSlots != null)
        {
            for (int slot : this.matchingSlots)
            {
                if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0,
                        this.inventoryFilteredBuffer, slot) == InvResult.MOVED_ALL)
                {
                    this.matchingSlots = null;

                    if (++this.outputPosition >= this.filterLength)
                    {
                        this.outputPosition = 0;
                        this.setMode(Mode.OUTPUT_ITEMS);
                    }

                    return true;
                }
            }
        }

        // If the item didn't fit or belong to the filtered buffer, move it to the normal output
        if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0,
                this.getInventoryNormalOut(), 0) == InvResult.MOVED_ALL)
        {
            return true;
        }

        return false;
    }

    @Override
    protected boolean outputItems()
    {
        if (this.inventoryFilteredBuffer.getStackInSlot(this.outputPosition).isEmpty() ||
            InventoryUtils.tryMoveEntireStackOnly(this.inventoryFilteredBuffer, this.outputPosition,
                this.inventoryFilteredOut, 0) == InvResult.MOVED_ALL)
        {
            // All items moved, return back to the sorting mode
            if (++this.outputPosition >= this.filterLength)
            {
                this.outputPosition = 0;
                this.setMode(Mode.SORT_ITEMS);
            }

            return true;
        }

        return false;
    }

    @Override
    protected boolean flushItems()
    {
        boolean success = false;

        while (this.outputPosition < this.filterLength)
        {
            if (this.inventoryFilteredBuffer.getStackInSlot(this.outputPosition).isEmpty())
            {
                this.outputPosition++;
            }
            else if (InventoryUtils.tryMoveEntireStackOnly(this.inventoryFilteredBuffer, this.outputPosition,
                        this.getInventoryNormalOut(), 0) == InvResult.MOVED_ALL)
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
            this.setMode(Mode.CONFIGURE_END_MARKER);
            return true;
        }

        return success;
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.outputPosition = 0;
        this.setMode(Mode.RESET);
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.outputPosition = tag.getByte("OutPos");
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("OutPos", (byte) this.outputPosition);

        return tag;
    }
}
