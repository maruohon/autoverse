package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperPlacerProgrammable;
import fi.dy.masa.autoverse.tileentity.TileEntityPlacerProgrammable;

public class ContainerPlacerProgrammable extends ContainerTile
{
    private final TileEntityPlacerProgrammable tepp;

    public ContainerPlacerProgrammable(EntityPlayer player, TileEntityPlacerProgrammable te)
    {
        super(player, te);
        this.tepp = te;

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
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.tepp.getInventoryIn(), 0, 8, 16));
        }
        else
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, 0, 8, 16));
        }

        ItemHandlerWrapperPlacerProgrammable placer = this.tepp.getPlacerHandler();
        IItemHandler inv = placer.getResetSequence().getSequenceInventory(false);

        int posX = 98;
        int posY = 16;

        // Add the reset sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 34;
        // Use a basic inventory to hold the items on the client side
        inv = this.isClient ? new ItemStackHandlerBasic(inv.getSlots()) : placer.getResetSequence().getSequenceInventory(true);

        // Add the reset sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        // Add the end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(placer.getEndMarkerInventory(), 0, 26, 16));

        // Add the high-bit marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(placer.getHighBitMarkerInventory(), 0, 26, 34));

        posX = 8;
        posY = 65;
        // Add the trigger sequence slots
        inv = placer.getTriggerSequence().getSequenceInventory(false);

        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 83;
        // Use a basic inventory to hold the items on the client side
        inv = this.isClient ? new ItemStackHandlerBasic(inv.getSlots()) : placer.getTriggerSequence().getSequenceInventory(true);

        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        // Add all the property sequence slots

        this.addPropertySequenceSlots(0, 98,  65, placer);
        this.addPropertySequenceSlots(1,  8, 114, placer);
        this.addPropertySequenceSlots(2, 98, 114, placer);

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tepp.getInventoryOut(), 0, 152, 153));
    }

    private void addPropertySequenceSlots(int id, int posX, int posY, ItemHandlerWrapperPlacerProgrammable placer)
    {
        IItemHandler inv = placer.getPropertySequence(id).getSequenceInventory(false);

        // Add the property sequence slots
        for (int slot = 0, x = posX; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, x, posY));
            x += 18;

            if (slot == 3)
            {
                x = posX;
                posY += 18;
            }
        }
    }
}
