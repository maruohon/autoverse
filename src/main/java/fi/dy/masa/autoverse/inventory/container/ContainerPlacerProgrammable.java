package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
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

        this.reAddSlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 1);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 16, this.inventory, this.tepp.getInventoryIn());

        ItemHandlerWrapperPlacerProgrammable placer = this.tepp.getPlacerHandler();

        // Add the reset sequence slots
        SlotPlacerSequence.create(98, 16, placer.getResetSequence(), this).place();

        // Add the end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(placer.getEndMarkerInventory(), 0, 26, 16));

        // Add the high-bit marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(placer.getHighBitMarkerInventory(), 0, 26, 34));

        SlotPlacerSequence.create( 8, 65, placer.getTriggerSequence(), this).place();

        // Add all the property sequence slots
        SlotPlacerSequence.create(98,  65, placer.getPropertySequence(0), this).setAddMatchedSlots(false).setMaxSlotsPerRow(4).place();
        SlotPlacerSequence.create( 8, 114, placer.getPropertySequence(1), this).setAddMatchedSlots(false).setMaxSlotsPerRow(4).place();
        SlotPlacerSequence.create(98, 114, placer.getPropertySequence(2), this).setAddMatchedSlots(false).setMaxSlotsPerRow(4).place();

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tepp.getInventoryOut(), 0, 152, 153));
    }
}
