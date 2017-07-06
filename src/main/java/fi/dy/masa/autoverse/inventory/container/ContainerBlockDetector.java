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
    private int invSize = -1;
    private final BitSet lockedLast = new BitSet(ItemHandlerWrapperBlockDetector.MAX_INV_SIZE);
    private final NonNullList<ItemStack> templateStacksLast =
            NonNullList.withSize(ItemHandlerWrapperBlockDetector.MAX_INV_SIZE, ItemStack.EMPTY);

    public ContainerBlockDetector(EntityPlayer player, TileEntityBlockDetector te)
    {
        super(player, te);

        this.ted = te;
        this.detector = te.getDetector();
        this.slotRangeDetectionInventory = new SlotRange(0, 0);

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

        // Add the normal items output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.ted.getInventoryOutNormal(), 0, 8, 151));

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

        if (invSize != this.invSize)
        {
            // Force sync all slots, because otherwise the client side
            // detection inventory may be left out of sync when the inventory flushes
            // and is set to size 0 immediately after (if the GUI is kept open over the reset and flush).
            // The reAddSlots() clears the server side "previous stacks", so when the slots
            // are added again during the next programming phase, both the slot and that "previous stack"
            // are empty on the server, and thus the slot won't be synced to the client, although on the client,
            // the last slot that had items during the previous operation cycle, hasn't been synced from the server.
            if (invSize == 0)
            {
                this.syncLockableSlots(this.detector.getDetectionInventory(), 0, 1, this.lockedLast, this.templateStacksLast);
                this.forceSyncAll = true;
                super.detectAndSendChanges();
                this.forceSyncAll = false;
            }

            this.syncProperty(0, (byte) invSize);
            this.reAddSlots();
            this.invSize = invSize;
        }

        this.syncLockableSlots(this.detector.getDetectionInventory(), 0, 1, this.lockedLast, this.templateStacksLast);

        super.detectAndSendChanges();
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
        }
    }

    @Override
    public void putCustomStack(int typeId, int slotNum, ItemStack stack)
    {
        if (typeId == 0)
        {
            this.detector.getDetectionInventory().setTemplateStackInSlot(slotNum, stack);
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

    public SlotRange getDetectionInventorySlotRange()
    {
        return this.slotRangeDetectionInventory;
    }
}
