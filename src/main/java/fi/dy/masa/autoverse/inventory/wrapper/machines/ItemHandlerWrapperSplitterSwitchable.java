package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperSplitterSwitchable extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcherVariable sequenceSwitch1;
    private final SequenceMatcherVariable sequenceSwitch2;
    private final IItemHandler inventoryOutput2;
    private final ItemStackHandlerTileEntity inventoryMatchBuffer;
    private boolean outputIsSecondary;
    private int position;
    private int subState;

    public ItemHandlerWrapperSplitterSwitchable(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput1,
            IItemHandler inventoryOutput2,
            TileEntitySplitter te)
    {
        super(4, inventoryInput, inventoryOutput1);

        this.inventoryOutput2 = inventoryOutput2;
        this.inventoryMatchBuffer = new ItemStackHandlerTileEntity(3, 4, 1, false, "ItemsMatchBuffer", te);

        this.sequenceSwitch1 = new SequenceMatcherVariable(4, "SequenceSwitch1");
        this.sequenceSwitch2 = new SequenceMatcherVariable(4, "SequenceSwitch2");

        this.getSequenceManager().add(this.sequenceSwitch1);
        this.getSequenceManager().add(this.sequenceSwitch2);
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        // Move to the reset flush state directly, because we need to make sure
        // that the item that caused the reset gets handled in the correct order,
        // depending on what we currently have buffered in the switch sequence buffer
        this.setState(State.RESET_FLUSH);

        // Prepare the sequences for reset.
        // This is normally called in the RESET state by the ItemHandlerWrapperSequenceBase class.
        this.onResetFlushStart();

        // Move in the reset command's last item, so it gets handled in the proper order
        this.inventoryMatchBuffer.setStackInSlot(this.position, this.getInputInventory().extractItem(0, 1, false));
        this.position = 0;
        this.subState = 3; // Splitter-specific reset state
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
            boolean ret = this.flushMatchBuffer();

            // Flushing the match buffer is now complete
            if (this.subState == 0)
            {
                // Reset the output side after the internal match buffer (which may or may not
                // have held some of the reset command items) has been flushed
                this.setSecondaryOutputActive(false);
            }

            return ret;
        }
    }

    @Override
    protected boolean moveInputItemToOutput()
    {
        return this.moveInputItemToInventory(this.getActiveOutputInventory());
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        // Matching arriving items against the current switch sequence
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
        SequenceMatcherVariable activeSequence = this.getActiveSequence();
        boolean matchesCurrent = activeSequence.inputItemMatchesCurrentPosition(stack);
        boolean fullMatch = activeSequence.checkInputItem(stack);

        if (matchesCurrent || this.position > 0)
        {
            // Move the input item to the buffer, because that item has already been examined and handled
            // by the main sequence, so we must also handle it (match or shift out)
            this.inventoryMatchBuffer.setStackInSlot(this.position, this.getInputInventory().extractItem(0, 1, false));
            this.position++;
        }

        // Input item matches the current item in the switch sequence
        if (matchesCurrent)
        {
            if (fullMatch)
            {
                this.switchOutputAndResetPosition(false);
            }

            return true;
        }
        // Input item does not match the current switch sequence, and there were currently matched/buffered items,
        // move to the subState to shift the buffered sequence out until it matches again, or becomes empty.
        else if (this.position > 0)
        {
            this.subState = 2;
        }
        // Input item does not match the current switch sequence, and there were no previously matched/buffered items
        else
        {
            return this.moveInputItemToOutput();
        }

        return false;
    }

    public void switchOutputAndResetPosition(boolean resetSequencePosition)
    {
        if (resetSequencePosition)
        {
            this.getActiveSequence().resetPosition();
        }

        this.setSecondaryOutputActive(this.secondaryOutputActive() == false);
        this.subState = 1;
        this.position = 0;
    }

    private boolean moveBufferedItems()
    {
        switch (this.subState)
        {
            // Flush the entire match buffer after a successful switch sequence match
            case 1:
                return this.flushMatchBuffer();

            // Shift the match buffer after a sequence mismatch
            case 2:
                return this.shiftMatchBuffer();

            // Splitter-specific reset state
            case 3:
                boolean ret = this.flushMatchBuffer();

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

    private boolean flushMatchBuffer()
    {
        IItemHandler inv = this.getActiveOutputInventory();
        int slotSrc = this.position;

        if (InventoryUtils.tryMoveStackToOtherInventory(this.inventoryMatchBuffer, inv, slotSrc, false) == InvResult.MOVED_ALL)
        {
            // Moved all items
            if (++this.position >= this.inventoryMatchBuffer.getSlots() ||
                this.inventoryMatchBuffer.getStackInSlot(this.position).isEmpty())
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
        IItemHandler inv = this.getActiveOutputInventory();

        if (InventoryUtils.tryMoveStackToOtherInventory(this.inventoryMatchBuffer, inv, 0, false) == InvResult.MOVED_ALL)
        {
            for (int i = 1; i < this.position; i++)
            {
                if (this.inventoryMatchBuffer.getStackInSlot(i).isEmpty())
                {
                    break;
                }

                this.inventoryMatchBuffer.setStackInSlot(i - 1, this.inventoryMatchBuffer.getStackInSlot(i));
            }

            this.inventoryMatchBuffer.setStackInSlot(this.position - 1, ItemStack.EMPTY);

            // This tracks the remaining sequence length in this case
            if (--this.position <= 0)
            {
                this.subState = 0;
                return true;
            }

            // Check if there is a new sequence match after the shift
            for (int i = 0; i < this.position; i++)
            {
                ItemStack stack = this.inventoryMatchBuffer.getStackInSlot(i);

                // Mismatch, skip the subState reset below and continue shifting the sequence on the next scheduled tick
                if (InventoryUtils.areItemStacksEqual(stack, this.getActiveSequence().getStackInSlot(i)) == false)
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

        InventoryUtils.dropInventoryContentsInWorld(world, pos, this.inventoryMatchBuffer);
    }

    private IItemHandler getActiveOutputInventory()
    {
        return this.secondaryOutputActive() ? this.inventoryOutput2 : this.getOutputInventory();
    }

    private SequenceMatcherVariable getActiveSequence()
    {
        return this.secondaryOutputActive() ? this.getSwitchSequence1() : this.getSwitchSequence2();
    }

    protected void setSecondaryOutputActive(boolean secondaryActive)
    {
        this.outputIsSecondary = secondaryActive;
    }

    public boolean secondaryOutputActive()
    {
        return this.outputIsSecondary;
    }

    public SequenceMatcherVariable getSwitchSequence1()
    {
        return this.sequenceSwitch1;
    }

    public SequenceMatcherVariable getSwitchSequence2()
    {
        return this.sequenceSwitch2;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("Position", (byte) this.position);
        tag.setByte("SubState", (byte) this.subState);
        tag.setBoolean("Secondary", this.outputIsSecondary);

        tag.merge(this.inventoryMatchBuffer.serializeNBT());

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.position = tag.getByte("Position");
        this.subState = tag.getByte("SubState");
        this.setSecondaryOutputActive(tag.getBoolean("Secondary"));

        this.inventoryMatchBuffer.deserializeNBT(tag);
    }
}
