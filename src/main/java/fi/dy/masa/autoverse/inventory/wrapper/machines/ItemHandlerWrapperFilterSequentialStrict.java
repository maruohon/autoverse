package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequentialStrict;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperFilterSequentialStrict extends ItemHandlerWrapperFilter
{
    private final ItemStackHandlerTileEntity inventoryFilteredBuffer;
    private int filterLength;
    private int position;
    private int subState;

    public ItemHandlerWrapperFilterSequentialStrict(
            int maxFilterLength,
            ItemStackHandlerTileEntity inventoryInput,
            ItemStackHandlerTileEntity inventoryOutFiltered,
            ItemStackHandlerTileEntity inventoryOutNormal,
            TileEntityFilterSequentialStrict te)
    {
        super(maxFilterLength, inventoryInput, inventoryOutFiltered, inventoryOutNormal);

        this.inventoryFilteredBuffer = new ItemStackHandlerTileEntity(3, maxFilterLength, 1, false, "ItemsFilteredBuffer", te);
    }

    @Override
    protected void onFullyConfigured()
    {
        this.filterLength = this.getFilterSequence().getCurrentSequenceLength();
        this.inventoryFilteredBuffer.setInventorySize(this.filterLength);
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        // Move to the reset flush state directly, because we need to make sure
        // that the item that caused the reset gets handled in the correct order,
        // depending on what we currently have buffered in the filter buffer
        this.setState(State.RESET_FLUSH);

        // Prepare the sequences for reset.
        // This is normally called in the RESET state by the ItemHandlerWrapperSequenceBase class.
        this.onResetFlushStart();

        // Move in the reset command's last item, so it gets handled in the proper order
        this.inventoryFilteredBuffer.setStackInSlot(this.position, this.getInputInventory().extractItem(0, 1, false));
        this.position = 0;
        this.subState = 3; // Filter-specific reset state
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
            return this.flushMatchBuffer(this.getOutputInventory());
        }
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        // Matching arriving items against the filter sequence
        if (this.subState == 0)
        {
            if (this.checkInputItem(stack))
            {
                return true;
            }
        }

        return this.moveBufferedItems();
    }

    @Override
    protected boolean onScheduledTick()
    {
        return this.moveBufferedItems();
    }

    private boolean checkInputItem(ItemStack stack)
    {
        boolean matchesCurrent = this.getFilterSequence().inputItemMatchesCurrentPosition(stack);
        boolean fullMatch = this.getFilterSequence().checkInputItem(stack);

        if (matchesCurrent || this.position > 0)
        {
            // Move the input item to the buffer, because that item has already been examined and handled
            // by the main sequence, so we must also handle it (match or shift out)
            this.inventoryFilteredBuffer.setStackInSlot(this.position, this.getInputInventory().extractItem(0, 1, false));
            this.position++;
        }

        // Input item matches the current item in the switch sequence
        if (matchesCurrent)
        {
            if (fullMatch)
            {
                this.subState = 1;
                this.position = 0;
            }

            return true;
        }
        // Input item does not match the current filter sequence, and there were currently matched/buffered items,
        // move to the subState to shift the buffered items out until it matches again, or becomes empty.
        else if (this.position > 0)
        {
            this.subState = 2;
        }
        // Input item does not match the current filter sequence, and there were no previously matched/buffered items
        else
        {
            return this.moveInputItemToOutput();
        }

        return false;
    }

    private boolean moveBufferedItems()
    {
        switch (this.subState)
        {
            // Flush the entire match buffer after a successful full sequence match
            case 1:
                return this.flushMatchBuffer(this.inventoryFilteredOut);

            // Shift the match buffer after a sequence mismatch
            case 2:
                return this.shiftMatchBuffer();

            // Filter-specific reset state
            case 3:
                boolean ret = this.flushMatchBuffer(this.getOutputInventory());

                // Flushing the match buffer is now complete
                if (this.subState == 0)
                {
                    this.setState(State.RESET);
                }

                return ret;

            default:
                return false;
        }
    }

    private boolean flushMatchBuffer(IItemHandler inv)
    {
        int slotSrc = this.position;

        if (InventoryUtils.tryMoveStackToOtherInventory(this.inventoryFilteredBuffer, inv, slotSrc, false) == InvResult.MOVED_ALL)
        {
            // Moved all items
            if (++this.position >= this.inventoryFilteredBuffer.getSlots() ||
                this.inventoryFilteredBuffer.getStackInSlot(this.position).isEmpty())
            {
                this.position = 0;
                this.subState = 0;
            }

            return true;
        }

        return false;
    }

    private boolean shiftMatchBuffer()
    {
        IItemHandler inv = this.getOutputInventory();

        if (InventoryUtils.tryMoveStackToOtherInventory(this.inventoryFilteredBuffer, inv, 0, false) == InvResult.MOVED_ALL)
        {
            for (int i = 1; i < this.position; i++)
            {
                if (this.inventoryFilteredBuffer.getStackInSlot(i).isEmpty())
                {
                    break;
                }

                this.inventoryFilteredBuffer.setStackInSlot(i - 1, this.inventoryFilteredBuffer.getStackInSlot(i));
            }

            this.inventoryFilteredBuffer.setStackInSlot(this.position - 1, ItemStack.EMPTY);

            // This tracks the remaining sequence length in this case
            if (--this.position <= 0)
            {
                this.subState = 0;
                return true;
            }

            // Check if there is a new sequence match after the shift
            for (int i = 0; i < this.position; i++)
            {
                ItemStack stack = this.inventoryFilteredBuffer.getStackInSlot(i);

                // Mismatch, skip the subState reset below and continue shifting the sequence on the next scheduled tick
                if (InventoryUtils.areItemStacksEqual(stack, this.getFilterSequence().getStackInSlot(i)) == false)
                {
                    return true;
                }
            }

            this.subState = 0;

            return true;
        }

        return false;
    }

    @Override
    public void dropAllItems(World world, BlockPos pos)
    {
        super.dropAllItems(world, pos);

        InventoryUtils.dropInventoryContentsInWorld(world, pos, this.inventoryFilteredBuffer);
    }

    public int getMatchedLength()
    {
        return this.getState() == State.NORMAL ? this.position : 0;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.subState = tag.getByte("SubState");
        this.position = tag.getByte("Position");
        this.filterLength = tag.getByte("FilterLength");

        this.inventoryFilteredBuffer.deserializeNBT(tag);
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("SubState", (byte) this.subState);
        tag.setByte("Position", (byte) this.position);
        tag.setByte("FilterLength", (byte) this.filterLength);

        tag.merge(this.inventoryFilteredBuffer.serializeNBT());

        return tag;
    }
}
