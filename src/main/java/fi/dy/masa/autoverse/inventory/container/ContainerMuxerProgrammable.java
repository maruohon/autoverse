package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperMuxer;
import fi.dy.masa.autoverse.tileentity.TileEntityMuxer;

public class ContainerMuxerProgrammable extends ContainerMuxerSimple
{
    public ContainerMuxerProgrammable(EntityPlayer player, TileEntityMuxer te)
    {
        super(player, te);
    }

    @Override
    protected void reAddSlots()
    {
        this.reAddSlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 16, this.inventory, this.tem.getInventoryInput1());

        ItemHandlerWrapperMuxer muxer = this.tem.getInventoryMuxer();

        // Add the sequence end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(muxer.getEndMarkerInventory(), 0, 26, 16));

        // Add the high bit marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(muxer.getBitMarkerInventory(), 0, 26, 34));

        // Add the reset sequence slots
        this.addSequenceSlots(98,  16, muxer.getResetSequence()).place();

        // Add the side 1 length config slots
        this.addSequenceSlots( 8,  65, muxer.getSequenceLength1()).setAddMatchedSlots(false).setMaxSlotsPerRow(8).place();

        // Add the side 2 length config slots
        this.addSequenceSlots( 8, 114, muxer.getSequenceLength2()).setAddMatchedSlots(false).setMaxSlotsPerRow(8).place();

        // Add the input 2 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tem.getInventoryInput2(), 0,   8, 151));

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tem.getInventoryOutput(), 0, 152, 151));
    }
}
