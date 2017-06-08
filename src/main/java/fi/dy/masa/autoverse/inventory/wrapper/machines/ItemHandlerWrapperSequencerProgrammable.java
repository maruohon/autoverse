package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockableVariable;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencerProgrammable;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperSequencerProgrammable extends ItemHandlerWrapperSequenceBase
{
    public static final int MAX_INV_SIZE = 45;
    private final SequenceMatcher sequenceMarkerItem;
    private final ItemStackHandlerLockableVariable sequenceInventory;
    private final IItemHandler markerInventory;
    private final IItemHandler inventoryOutput;
    private Mode mode = Mode.CONFIGURE_RESET;
    private int position;

    public ItemHandlerWrapperSequencerProgrammable(
            int resetSequenceLength,
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            TileEntitySequencerProgrammable te)
    {
        super(resetSequenceLength, inventoryInput);

        this.inventoryOutput = inventoryOutput;
        this.sequenceMarkerItem = new SequenceMatcher(1, "SequenceMarker");
        this.sequenceInventory  = new ItemStackHandlerLockableVariable(2, MAX_INV_SIZE, 64, false, "ItemsSequence", te);
        this.sequenceInventory.setInventorySize(0);

        this.markerInventory = this.sequenceMarkerItem.getSequenceInventory(false);
    }

    @Override
    protected void handleInputItem(ItemStack inputStack)
    {
        switch (this.getMode())
        {
            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_MARKER);
                }
                break;

            case CONFIGURE_MARKER:
                if (this.sequenceMarkerItem.configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_SEQUENCE);
                    this.position = 0;
                }
                break;

            case CONFIGURE_SEQUENCE:
                if (InventoryUtils.areItemStacksEqual(inputStack, this.markerInventory.getStackInSlot(0)))
                {
                    this.position = 0;
                    this.setMode(Mode.NORMAL_OPERATION);
                }
                else
                {
                    this.sequenceInventory.setTemplateStackInSlot(this.position, inputStack);
                    this.sequenceInventory.setSlotLocked(this.position, true);
                    this.sequenceInventory.setInventorySize(this.position + 1);

                    if (++this.position >= MAX_INV_SIZE)
                    {
                        this.position = 0;
                        this.setMode(Mode.NORMAL_OPERATION);
                    }
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.getResetSequence().reset();
                    this.sequenceMarkerItem.reset();
                    this.position = 0;
                    this.setMode(Mode.RESET_FLUSH_SEQUENCE);
                }
                break;

            case RESET_FLUSH_SEQUENCE:
                break;

            default:
                break;
        }
    }

    @Override
    public boolean moveItems()
    {
        switch (this.getMode())
        {
            case CONFIGURE_RESET:
            case CONFIGURE_MARKER:
            case CONFIGURE_SEQUENCE:
                return InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutput, 0) != InvResult.MOVED_NOTHING;

            case NORMAL_OPERATION:
                if (this.getInputInventory().getStackInSlot(0).isEmpty() == false)
                {
                    if (InventoryUtils.tryMoveStackToOtherInventory(this.getInputInventory(), this.sequenceInventory, 0, false) != InvResult.MOVED_NOTHING)
                    {
                        return true;
                    }
                    else
                    {
                        return InventoryUtils.tryMoveStackToOtherInventory(this.getInputInventory(), this.inventoryOutput, 0, false) != InvResult.MOVED_NOTHING;
                    }
                }
                break;

            case RESET_FLUSH_SEQUENCE:
                boolean success = InventoryUtils.tryMoveAllItems(this.sequenceInventory, this.inventoryOutput) != InvResult.MOVED_NOTHING;

                if (InventoryUtils.isInventoryEmpty(this.sequenceInventory))
                {
                    this.sequenceInventory.clearTemplateStacks();
                    this.sequenceInventory.clearLockedStatus();
                    this.sequenceInventory.setInventorySize(0);
                    this.position = 0;
                    this.setMode(Mode.CONFIGURE_RESET);
                }

                return success;
        }

        return false;
    }

    @Override
    public int getSlots()
    {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        // The first "virtual slot" is for extraction, the second is for insertion (and thus always empty)
        if (slot == 0 && this.getMode() == Mode.NORMAL_OPERATION)
        {
            return this.sequenceInventory.getStackInSlot(this.position);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (slot == 0 && this.getMode() == Mode.NORMAL_OPERATION)
        {
            ItemStack stack = this.sequenceInventory.extractItem(this.position, amount, simulate);

            if (simulate == false && stack.isEmpty() == false)
            {
                if (++this.position >= this.sequenceInventory.getSlots())
                {
                    this.position = 0;
                }
            }

            return stack;
        }

        return ItemStack.EMPTY;
    }

    public IItemHandler getMarkerInventory()
    {
        return this.markerInventory;
    }

    public ItemStackHandlerLockableVariable getSequenceInventory()
    {
        return this.sequenceInventory;
    }

    public int getExtractPosition()
    {
        return this.getMode() == Mode.NORMAL_OPERATION ? this.position : 0;
    }

    protected Mode getMode()
    {
        return this.mode;
    }

    protected void setMode(Mode mode)
    {
        this.mode = mode;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("State", (byte) this.mode.getId());
        tag.setByte("Position", (byte) this.position);

        this.sequenceMarkerItem.writeToNBT(tag);
        tag.merge(this.sequenceInventory.serializeNBT());

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.setMode(Mode.fromId(tag.getByte("State")));
        this.position = tag.getByte("Position");

        this.sequenceMarkerItem.readFromNBT(tag);
        this.sequenceInventory.deserializeNBT(tag);
    }

    public enum Mode
    {
        CONFIGURE_RESET         (0),
        CONFIGURE_MARKER        (1),
        CONFIGURE_SEQUENCE      (2),
        NORMAL_OPERATION        (3),
        RESET_FLUSH_SEQUENCE    (5);

        private final int id;

        private Mode (int id)
        {
            this.id = id;
        }

        public int getId()
        {
            return this.id;
        }

        public static Mode fromId(int id)
        {
            return values()[id % values().length];
        }
    }
}
