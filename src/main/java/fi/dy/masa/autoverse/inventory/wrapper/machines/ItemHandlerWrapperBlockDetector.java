package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockDetector;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperBlockDetector extends ItemHandlerWrapperSequenceBase implements ISequenceCallback
{
    public static final int MAX_INV_SIZE = 18;
    private final TileEntityBlockDetector te;
    private final SequenceMatcher sequenceBitMarker;
    private final SequenceMatcherVariable sequenceDistance;
    private final SequenceMatcherVariable sequenceAngle;
    private final SequenceMatcherVariable sequenceDelay;
    private final SequenceMatcherVariable sequenceDetection;
    private final ItemStackHandlerLockable inventoryDetector;
    private int subState;

    public ItemHandlerWrapperBlockDetector(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            TileEntityBlockDetector te)
    {
        super(4, inventoryInput, inventoryOutput);

        this.te = te;
        this.sequenceBitMarker  = new SequenceMatcher(1, "SequenceBitMarker");
        this.sequenceDistance   = (new SequenceMatcherVariable(4, "SequenceDistance")).setAllowEmptySequence(true);
        this.sequenceAngle      = (new SequenceMatcherVariable(4, "SequenceAngle")).setAllowEmptySequence(true);
        this.sequenceDelay      = (new SequenceMatcherVariable(8, "SequenceDelay")).setAllowEmptySequence(true);
        this.sequenceDetection  = (new SequenceMatcherVariable(MAX_INV_SIZE, "SequenceDetection")).setAllowEmptySequence(true);
        this.sequenceDetection.setCallback(this, 0);

        this.getSequenceManager().add(this.sequenceBitMarker, 1);
        this.getSequenceManager().add(this.sequenceDistance);
        this.getSequenceManager().add(this.sequenceAngle);
        this.getSequenceManager().add(this.sequenceDelay);
        this.getSequenceManager().add(this.sequenceDetection);

        this.inventoryDetector = new ItemStackHandlerLockable(2, MAX_INV_SIZE, 64, false, "ItemsDetector", te);
        this.inventoryDetector.setInventorySize(0);
    }

    @Override
    public void onConfigureSequenceSlot(int callbackId, int slot, boolean finished)
    {
        this.inventoryDetector.setInventorySize(slot + 1);
        this.inventoryDetector.setTemplateStackInSlot(slot, this.sequenceDetection.getStackInSlot(slot));
        this.inventoryDetector.setSlotLocked(slot, true);
    }

    @Override
    protected void onFullyConfigured()
    {
        ItemStack stackHighBit = this.sequenceBitMarker.getSequence().get(0);

        this.te.setDistance(this.sequenceDistance.parseValueFromSequence(stackHighBit));
        this.te.setAngle(this.sequenceAngle.parseValueFromSequence(stackHighBit));
        this.te.setDelay(this.sequenceDelay.parseValueFromSequence(stackHighBit));

        this.te.startDetector();
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.te.stopDetector();
    }

    @Override
    protected void onResetFlushComplete()
    {
        this.subState = 1;
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        if (this.moveInputItemToInventory(this.inventoryDetector))
        {
            return true;
        }
        else
        {
            return this.moveInputItemToOutput();
        }
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
            boolean success = InventoryUtils.tryMoveAllItems(this.inventoryDetector, this.getOutputInventory()) != InvResult.MOVED_NOTHING;

            if (InventoryUtils.isInventoryEmpty(this.inventoryDetector))
            {
                this.inventoryDetector.clearTemplateStacks();
                this.inventoryDetector.clearLockedStatus();
                this.inventoryDetector.setInventorySize(0);
                this.subState = 0;
                this.setState(State.CONFIGURE);
            }

            return success;
        }
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
        return slot == 0 ? this.getOutputInventory().getStackInSlot(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return slot == 0 ? this.getOutputInventory().extractItem(0, amount, simulate) : ItemStack.EMPTY;
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
        return this.inventoryDetector;
    }

    public int getCurrentDetectionSetSize()
    {
        return this.inventoryDetector.getSlots();
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("SubState", (byte) this.subState);
        tag.merge(this.inventoryDetector.serializeNBT());

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.subState = tag.getByte("SubState");
        this.inventoryDetector.deserializeNBT(tag);
    }
}
