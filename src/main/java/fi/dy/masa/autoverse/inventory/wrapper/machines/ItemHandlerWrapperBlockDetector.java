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
    private final SequenceMatcherVariable sequenceOthers;
    private final ItemStackHandlerLockable inventoryDetector;
    private final ItemStackHandlerLockable inventoryOthersBuffer;
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

        this.sequenceOthers  = (new SequenceMatcherVariable(1, "SequenceDetectionOthers")).setAllowEmptySequence(true);
        this.sequenceOthers.setCallback(this, 1);

        this.getSequenceManager().add(this.sequenceBitMarker, 1);
        this.getSequenceManager().add(this.sequenceDistance);
        this.getSequenceManager().add(this.sequenceAngle);
        this.getSequenceManager().add(this.sequenceDelay);
        this.getSequenceManager().add(this.sequenceDetection);
        this.getSequenceManager().add(this.sequenceOthers);

        this.inventoryDetector = new ItemStackHandlerLockable(2, MAX_INV_SIZE, 64, false, "ItemsDetector", te);
        this.inventoryDetector.setInventorySize(0);

        this.inventoryOthersBuffer = new ItemStackHandlerLockable(3, 1, 256, true, "ItemsDetectorOthers", te);
    }

    @Override
    public void onConfigureSequenceSlot(int callbackId, int slot, boolean finished)
    {
        if (callbackId == 0)
        {
            this.inventoryDetector.setInventorySize(slot + 1);
            this.inventoryDetector.setTemplateStackInSlot(slot, this.sequenceDetection.getStackInSlot(slot));
            this.inventoryDetector.setSlotLocked(slot, true);
        }
        else if (callbackId == 1)
        {
            this.inventoryOthersBuffer.setInventorySize(slot + 1);
            this.inventoryOthersBuffer.setTemplateStackInSlot(slot, this.sequenceOthers.getStackInSlot(slot));
            this.inventoryOthersBuffer.setSlotLocked(slot, true);
        }
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
        else if (this.moveInputItemToInventory(this.inventoryOthersBuffer))
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
            ItemStackHandlerLockable inv = this.subState == 2 ? this.inventoryOthersBuffer : this.inventoryDetector;
            boolean success = InventoryUtils.tryMoveAllItems(inv, this.getOutputInventory()) != InvResult.MOVED_NOTHING;

            if (InventoryUtils.isInventoryEmpty(inv))
            {
                inv.clearTemplateStacks();
                inv.clearLockedStatus();
                inv.setInventorySize(0);

                if (this.subState >= 2)
                {
                    this.subState = 0;
                    this.setState(State.CONFIGURE);
                }
                else
                {
                    this.subState++;
                }
            }

            return success;
        }
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

    public ItemStackHandlerLockable getOthersBufferInventory()
    {
        return this.inventoryOthersBuffer;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("SubState", (byte) this.subState);
        tag.merge(this.inventoryDetector.serializeNBT());
        tag.merge(this.inventoryOthersBuffer.serializeNBT());

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.subState = tag.getByte("SubState");
        this.inventoryDetector.deserializeNBT(tag);
        this.inventoryOthersBuffer.deserializeNBT(tag);
    }
}
