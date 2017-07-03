package fi.dy.masa.autoverse.inventory.container;

import java.util.BitSet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.SlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperDetector;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockDetector;

public class ContainerDetector extends ContainerTile
{
    private final TileEntityBlockDetector ted;
    private final ItemHandlerWrapperDetector detector;
    private SlotRange slotRangeDetectionInventory;
    private int invSize = -1;
    private final BitSet lockedLast = new BitSet(ItemHandlerWrapperDetector.MAX_INV_SIZE);
    private final NonNullList<ItemStack> templateStacksLast =
            NonNullList.withSize(ItemHandlerWrapperDetector.MAX_INV_SIZE, ItemStack.EMPTY);

    public ContainerDetector(EntityPlayer player, TileEntityBlockDetector te)
    {
        super(player, te);

        this.ted = te;
        this.detector = te.getDetector();
        this.slotRangeDetectionInventory = new SlotRange(0, 0);
    }

    private void reAddSlots()
    {
        this.inventorySlots.clear();
        this.inventoryItemStacks.clear();

        this.getSpecialSlots().clear();
        this.getSpecialSlotStacks().clear();

        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        ItemHandlerWrapperDetector detector = this.ted.getDetector();

        if (this.isClient)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.ted.getInventoryInput(), 0, 8, 16));
        }
        else
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, 0, 8, 16));
        }

        // Add the sequence end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.detector.getEndMarkerInventory(), 0, 26, 16));

        // Add the high bit marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.detector.getBitMarkerInventory(), 0, 26, 34));

        int posX = 98;
        int posY = 16;
        IItemHandler inv = detector.getResetSequence().getSequenceInventory(false);

        // Add the reset sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 34;
        // Use a basic inventory to hold the items on the client side
        inv = this.isClient ? new ItemStackHandlerBasic(inv.getSlots()) : detector.getResetSequence().getSequenceInventory(true);

        // Add the reset sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posX = 8;
        posY = 65;
        inv = detector.getSequenceDistance().getSequenceInventory(false);

        // Add the distance config sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 83;
        inv = detector.getSequenceAngle().getSequenceInventory(false);

        // Add the angle config sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posX = 98;
        posY = 65;
        inv = detector.getSequenceDelay().getSequenceInventory(false);

        // Add the delay config sequence slots
        for (int slot = 0, x = posX; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, x, posY));

            if (slot == 3)
            {
                x = posX;
                posY += 18;
            }
            else
            {
                x += 18;
            }
        }

        posX = 8;
        posY = 114;
        inv = this.detector.getDetectionInventory();
        final int invSize = inv.getSlots();
        this.slotRangeDetectionInventory = new SlotRange(this.getSpecialSlots().size(), invSize);

        // Add the detection slots
        for (int slot = 0, x = posX; slot < invSize; slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, x, posY));

            if (slot % 9 == 8)
            {
                x = posX;
                posY += 18;
            }
            else
            {
                x += 18;
            }
        }

        // Add the normal items output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.ted.getInventoryOutNormal(), 0, 8, 153));

        // Add the detection items output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.ted.getInventoryOutDetection(), 0, 152, 153));
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

    public int getDetectionInvSize()
    {
        return this.detector.getDetectionInventory().getSlots();
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
