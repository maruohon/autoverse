package fi.dy.masa.autoverse.inventory.container;

import java.util.BitSet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.container.base.SlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequencerProgrammable;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencerProgrammable;

public class ContainerSequencerProgrammable extends ContainerTile
{
    private final TileEntitySequencerProgrammable tesp;
    private final ItemHandlerWrapperSequencerProgrammable sequencer;
    private final BitSet lockedLast = new BitSet(ItemHandlerWrapperSequencerProgrammable.MAX_INV_SIZE);
    private final NonNullList<ItemStack> templateStacksLast =
            NonNullList.withSize(ItemHandlerWrapperSequencerProgrammable.MAX_INV_SIZE, ItemStack.EMPTY);
    private SlotRange slotRangeSequenceInventory;
    private int outputSlot = -1;
    private int invSize = -1;

    public ContainerSequencerProgrammable(EntityPlayer player, TileEntitySequencerProgrammable te)
    {
        super(player, te);
        this.tesp = te;
        this.sequencer = te.getSequencerHandler();
        this.slotRangeSequenceInventory = new SlotRange(0, 0);

        this.reAddSlots();
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
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 1);

        if (this.isClient)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInventoryIn(), 0, 8, 16));
        }
        else
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, 0, 8, 16));
        }

        // Add the sequence end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.sequencer.getMarkerInventory(), 0, 8, 34));

        IItemHandler inv = this.sequencer.getResetSequence().getSequenceInventory(false);

        int posX = 98;
        int posY = 16;

        // Add the reset sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 34;
        // Use a basic inventory to hold the items on the client side
        inv = this.isClient ? new ItemStackHandlerBasic(inv.getSlots()) : this.sequencer.getResetSequence().getSequenceInventory(true);

        // Add the reset sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posX = 8;
        posY = 56;
        inv = this.sequencer.getSequenceInventory();
        final int invSize = inv.getSlots();
        this.slotRangeSequenceInventory = new SlotRange(this.getSpecialSlots().size(), invSize);

        // Add the sequence slots
        for (int slot = 0, x = posX; slot < invSize; slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, x, posY));

            x += 18;

            if (slot % 9 == 8)
            {
                x = posX;
                posY += 18;
            }
        }

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInventoryOut(), 0, 152, 151));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient)
        {
            return;
        }

        int outputSlot = this.sequencer.getExtractPosition();
        int invSize = this.sequencer.getSequenceInventory().getSlots();

        if (outputSlot != this.outputSlot)
        {
            this.syncProperty(0, (byte) outputSlot);
            this.outputSlot = outputSlot;
        }

        if (invSize != this.invSize)
        {
            this.syncProperty(1, (byte) invSize);
            this.reAddSlots();
            this.invSize = invSize;
        }

        this.syncLockableSlots(this.sequencer.getSequenceInventory(), 0, 2, this.lockedLast, this.templateStacksLast);

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        switch (id)
        {
            case 0:
                this.outputSlot = value;
                break;

            case 1:
                this.sequencer.getSequenceInventory().setInventorySize(value);
                this.reAddSlots();
                break;

            case 2:
                this.sequencer.getSequenceInventory().setSlotLocked(value & 0xFF, (value & 0x8000) != 0);
                break;
        }
    }

    @Override
    public void putCustomStack(int typeId, int slotNum, ItemStack stack)
    {
        if (typeId == 0)
        {
            this.sequencer.getSequenceInventory().setTemplateStackInSlot(slotNum, stack);
        }
        else
        {
            super.putCustomStack(typeId, slotNum, stack);
        }
    }

    public int getOutputSlot()
    {
        return this.outputSlot;
    }

    public ItemStackHandlerLockable getSequenceInventory()
    {
        return this.sequencer.getSequenceInventory();
    }

    public SlotRange getSequenceInventorySlotRange()
    {
        return this.slotRangeSequenceInventory;
    }
}
