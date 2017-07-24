package fi.dy.masa.autoverse.inventory.container;

import java.util.BitSet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.SlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperBlockDetector;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockDetector;

public class ContainerBlockDetector extends ContainerTile
{
    private final TileEntityBlockDetector ted;
    private final ItemHandlerWrapperBlockDetector detector;
    private SlotRange slotRangeDetectionInventory;
    private SlotRange slotRangeOthersBufferInventory;
    private int invSizeDetection = -1;
    private int invSizeOthers = -1;
    private boolean useIndicators;
    private final BitSet lockedLastDetection = new BitSet(ItemHandlerWrapperBlockDetector.MAX_INV_SIZE);
    private final BitSet lockedLastOthers = new BitSet(1);
    private final NonNullList<ItemStack> templateStacksLastDetection = NonNullList.withSize(ItemHandlerWrapperBlockDetector.MAX_INV_SIZE, ItemStack.EMPTY);
    private final NonNullList<ItemStack> templateStacksLastOthers = NonNullList.withSize(1, ItemStack.EMPTY);

    public ContainerBlockDetector(EntityPlayer player, TileEntityBlockDetector te)
    {
        super(player, te);

        this.ted = te;
        this.detector = te.getDetector();

        this.reAddSlots();
    }

    private void reAddSlots()
    {
        this.reAddSlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 16, this.inventory, this.ted.getInventoryInput());

        ItemHandlerWrapperBlockDetector detector = this.ted.getDetector();

        // Add the sequence end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.detector.getEndMarkerInventory(), 0, 26, 16));

        // Add the high bit marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.detector.getBitMarkerInventory(), 0, 26, 34));

        // Add the reset sequence slots
        this.addSequenceSlots(98, 16, detector.getResetSequence()).place();

        // Add the distance config sequence slots
        this.addSequenceSlots( 8, 65, detector.getSequenceDistance()).setAddMatchedSlots(false).place();

        // Add the angle config sequence slots
        this.addSequenceSlots( 8, 83, detector.getSequenceAngle()).setAddMatchedSlots(false).place();

        // Add the delay config sequence slots
        this.addSequenceSlots(98, 65, detector.getSequenceDelay()).setAddMatchedSlots(false).setMaxSlotsPerRow(4).place();

        // Add the detection slots
        this.slotRangeDetectionInventory = new SlotRange(this.getSpecialSlots().size(), detector.getDetectionInventory().getSlots());
        SlotPlacer.create(8, 114, detector.getDetectionInventory(), this).setSlotType(SlotType.SPECIAL).place();

        // Add the "other blocks detection buffer" slots
        this.slotRangeOthersBufferInventory = new SlotRange(this.getSpecialSlots().size(), detector.getOthersBufferInventory().getSlots());
        SlotPlacer.create(8, 151, detector.getOthersBufferInventory(), this).setSlotType(SlotType.SPECIAL).place();

        // Add the normal items output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.ted.getInventoryOutNormal(), 0, 116, 151));

        // Add the detection items output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.ted.getInventoryOutDetection(), 0, 152, 151));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient)
        {
            return;
        }

        int invSize = this.detector.getDetectionInventory().getSlots();
        boolean useIndicators = this.ted.getUseIndicators();
        boolean sizeChanged = false;

        if (useIndicators != this.useIndicators)
        {
            this.syncProperty(2, (byte) (useIndicators ? 1 : 0));
            this.useIndicators = useIndicators;
        }

        // Force sync all slots before shrinking the inventory size,
        // so that the client side inventory doesn't get left out-of-sync

        if (invSize < this.invSizeDetection)
        {
            this.syncLockableSlots(this.detector.getDetectionInventory(), 0, 1, this.lockedLastDetection, this.templateStacksLastDetection);

            this.forceSyncAll = true;
            super.detectAndSendChanges();
            this.forceSyncAll = false;
        }

        if (invSize != this.invSizeDetection)
        {
            this.syncProperty(0, (byte) invSize);
            this.invSizeDetection = invSize;
            sizeChanged = true;
        }

        invSize = this.detector.getOthersBufferInventory().getSlots();

        if (invSize != this.invSizeOthers)
        {
            this.syncProperty(3, (byte) invSize);
            this.invSizeOthers = invSize;
            sizeChanged = true;
        }

        if (sizeChanged)
        {
            this.reAddSlots();
        }

        this.syncLockableSlots(this.detector.getDetectionInventory(), 0, 1, this.lockedLastDetection, this.templateStacksLastDetection);
        this.syncLockableSlots(this.detector.getOthersBufferInventory(), 1, 4, this.lockedLastOthers, this.templateStacksLastOthers);

        if (sizeChanged)
        {
            this.forceSyncAll = true;
            super.detectAndSendChanges();
            this.forceSyncAll = false;
        }
        else
        {
            super.detectAndSendChanges();
        }
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        switch (id)
        {
            case 0:
                this.detector.getDetectionInventory().setInventorySize(value);
                this.reAddSlots();
                break;

            case 1:
                this.detector.getDetectionInventory().setSlotLocked(value & 0xFF, (value & 0x8000) != 0);
                break;

            case 2:
                this.ted.setUseIndicators(value == 1);
                break;

            case 3:
                this.detector.getOthersBufferInventory().setInventorySize(value);
                this.reAddSlots();
                break;

            case 4:
                this.detector.getOthersBufferInventory().setSlotLocked(value & 0xFF, (value & 0x8000) != 0);
                break;
        }
    }

    @Override
    public void putCustomStack(int typeId, int slotNum, ItemStack stack)
    {
        if (typeId == 0)
        {
            this.detector.getDetectionInventory().setTemplateStackInSlot(slotNum, stack);
        }
        else if (typeId == 1)
        {
            this.detector.getOthersBufferInventory().setTemplateStackInSlot(slotNum, stack);
        }
        else
        {
            super.putCustomStack(typeId, slotNum, stack);
        }
    }

    public ItemStackHandlerLockable getDetectionInventory()
    {
        return this.detector.getDetectionInventory();
    }

    public ItemStackHandlerLockable getOthersBufferInventory()
    {
        return this.detector.getOthersBufferInventory();
    }

    public SlotRange getDetectionInventorySlotRange()
    {
        return this.slotRangeDetectionInventory;
    }

    public SlotRange getOthersBufferInventorySlotRange()
    {
        return this.slotRangeOthersBufferInventory;
    }
}
