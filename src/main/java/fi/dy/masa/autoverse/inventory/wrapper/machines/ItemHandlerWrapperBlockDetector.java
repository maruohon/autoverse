package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockDetector;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperBlockDetector extends ItemHandlerWrapperSequenceBase
{
    public static final int MAX_INV_SIZE = 18;
    private final TileEntityBlockDetector te;
    private final SequenceMatcher sequenceBitMarker;
    private final SequenceMatcherVariable sequenceDistance;
    private final SequenceMatcherVariable sequenceAngle;
    private final SequenceMatcherVariable sequenceDelay;
    private final ItemStackHandlerLockable detectionInventory;
    private final IItemHandler inventoryOutput;
    private Mode mode = Mode.CONFIGURE_END_MARKER;
    private int position;

    public ItemHandlerWrapperBlockDetector(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            TileEntityBlockDetector te)
    {
        super(4, inventoryInput);

        this.te = te;
        this.sequenceBitMarker  = new SequenceMatcher(1, "SequenceBitMarker");
        this.sequenceDistance   = (new SequenceMatcherVariable(4, "SequenceDistance")).setAllowEmptySequence(true);
        this.sequenceAngle      = (new SequenceMatcherVariable(4, "SequenceAngle")).setAllowEmptySequence(true);
        this.sequenceDelay      = (new SequenceMatcherVariable(8, "SequenceDelay")).setAllowEmptySequence(true);

        this.detectionInventory = new ItemStackHandlerLockable(5, MAX_INV_SIZE, 64, false, "ItemsSequence", te);
        this.detectionInventory.setInventorySize(0);
        this.inventoryOutput = inventoryOutput;
    }

    @Override
    protected void handleInputItem(ItemStack inputStack)
    {
        switch (this.getMode())
        {
            case CONFIGURE_END_MARKER:
                if (this.getEndMarkerSequence().configureSequence(inputStack))
                {
                    this.getResetSequence().setSequenceEndMarker(inputStack);
                    this.sequenceDistance.setSequenceEndMarker(inputStack);
                    this.sequenceAngle.setSequenceEndMarker(inputStack);
                    this.sequenceDelay.setSequenceEndMarker(inputStack);
                    this.setMode(Mode.CONFIGURE_BIT_MARKER);
                }
                break;

            case CONFIGURE_BIT_MARKER:
                if (this.sequenceBitMarker.configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                break;

            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_DISTANCE);
                }
                break;

            case CONFIGURE_DISTANCE:
                if (this.sequenceDistance.configureSequence(inputStack))
                {
                    this.te.setDistance(this.sequenceDistance.parseValueFromSequence(this.sequenceBitMarker.getSequence().get(0)));
                    this.setMode(Mode.CONFIGURE_ANGLE);
                }
                break;

            case CONFIGURE_ANGLE:
                if (this.sequenceAngle.configureSequence(inputStack))
                {
                    this.te.setAngle(this.sequenceAngle.parseValueFromSequence(this.sequenceBitMarker.getSequence().get(0)));
                    this.setMode(Mode.CONFIGURE_DELAY);
                }
                break;

            case CONFIGURE_DELAY:
                if (this.sequenceDelay.configureSequence(inputStack))
                {
                    this.te.setDelay(this.sequenceDelay.parseValueFromSequence(this.sequenceBitMarker.getSequence().get(0)));
                    this.setMode(Mode.CONFIGURE_DETECTOR);
                    this.position = 0;
                }
                break;

            case CONFIGURE_DETECTOR:
                if (InventoryUtils.areItemStacksEqual(inputStack, this.getEndMarkerSequence().getStackInSlot(0)))
                {
                    this.position = 0;
                    this.setMode(Mode.NORMAL_OPERATION);
                    this.te.startDetector();
                }
                else
                {
                    this.detectionInventory.setTemplateStackInSlot(this.position, inputStack);
                    this.detectionInventory.setSlotLocked(this.position, true);
                    this.detectionInventory.setInventorySize(this.position + 1);

                    if (++this.position >= MAX_INV_SIZE)
                    {
                        this.position = 0;
                        this.setMode(Mode.NORMAL_OPERATION);
                        this.te.startDetector();
                    }
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.onReset();
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.sequenceBitMarker.reset();
        this.sequenceDistance.reset();
        this.sequenceAngle.reset();
        this.sequenceDelay.reset();

        this.position = 0;
        this.te.stopDetector();
        this.setMode(Mode.RESET_FLUSH_ITEMS);
    }

    @Override
    public boolean moveItems()
    {
        switch (this.getMode())
        {
            case NORMAL_OPERATION:
                if (this.getInputInventory().getStackInSlot(0).isEmpty() == false)
                {
                    if (InventoryUtils.tryMoveStackToOtherInventory(this.getInputInventory(), this.detectionInventory, 0, false) != InvResult.MOVED_NOTHING)
                    {
                        return true;
                    }
                    else
                    {
                        return InventoryUtils.tryMoveStackToOtherInventory(this.getInputInventory(), this.inventoryOutput, 0, false) != InvResult.MOVED_NOTHING;
                    }
                }
                break;

            case RESET_FLUSH_ITEMS:
                boolean success = InventoryUtils.tryMoveAllItems(this.detectionInventory, this.inventoryOutput) != InvResult.MOVED_NOTHING;

                if (InventoryUtils.isInventoryEmpty(this.detectionInventory))
                {
                    this.detectionInventory.clearTemplateStacks();
                    this.detectionInventory.clearLockedStatus();
                    this.detectionInventory.setInventorySize(0);
                    this.position = 0;
                    this.setMode(Mode.CONFIGURE_END_MARKER);
                }

                return success;

            default:
                return InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutput, 0) != InvResult.MOVED_NOTHING;
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
        // The first "virtual slot" is for extraction from the "generic output slot",
        // the second "virtual slot" is for insertion (and thus always empty)
        return slot == 0 ? this.inventoryOutput.getStackInSlot(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return slot == 0 ? this.inventoryOutput.extractItem(0, amount, simulate) : ItemStack.EMPTY;
    }

    public IItemHandler getBitMarkerInventory()
    {
        return this.sequenceBitMarker.getSequenceInventory(false);
    }

    public SequenceMatcher getSequenceDistance()
    {
        return this.sequenceDistance;
    }

    public SequenceMatcher getSequenceAngle()
    {
        return this.sequenceAngle;
    }

    public SequenceMatcher getSequenceDelay()
    {
        return this.sequenceDelay;
    }

    public ItemStackHandlerLockable getDetectionInventory()
    {
        return this.detectionInventory;
    }

    public int getCurrentDetectionSetSize()
    {
        return this.detectionInventory.getSlots();
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

        this.sequenceBitMarker.writeToNBT(tag);
        this.sequenceDistance.writeToNBT(tag);
        this.sequenceAngle.writeToNBT(tag);
        this.sequenceDelay.writeToNBT(tag);

        tag.merge(this.detectionInventory.serializeNBT());

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.setMode(Mode.fromId(tag.getByte("State")));
        this.position = tag.getByte("Position");

        this.sequenceBitMarker.readFromNBT(tag);
        this.sequenceDistance.readFromNBT(tag);
        this.sequenceAngle.readFromNBT(tag);
        this.sequenceDelay.readFromNBT(tag);

        this.detectionInventory.deserializeNBT(tag);
    }

    public enum Mode
    {
        CONFIGURE_RESET         (0),
        CONFIGURE_END_MARKER    (1),
        CONFIGURE_BIT_MARKER    (2),
        CONFIGURE_DISTANCE      (3),
        CONFIGURE_ANGLE         (4),
        CONFIGURE_DELAY         (5),
        CONFIGURE_DETECTOR      (6),
        NORMAL_OPERATION        (7),
        RESET_FLUSH_ITEMS       (8);

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
