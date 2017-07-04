package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;

public class ContainerFilter extends ContainerTile
{
    private final TileEntityFilter tefi;

    public ContainerFilter(EntityPlayer player, TileEntityFilter te)
    {
        super(player, te);

        this.tefi = te;
        this.reAddSlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 16, this.inventory, this.tefi.getInventoryInput());

        ItemHandlerWrapperFilter filter = this.tefi.getInventoryFilter();

        // Add the Reset Sequence slots
        SlotPlacerSequence.create(98, 16, filter.getResetSequence(), this).place();

        // Add the Filter slots
        SlotPlacer.create(8, 63, filter.getFilterSequenceInventory(), this).setSlotType(SlotType.SPECIAL).place();

        if (this.tefi instanceof TileEntityFilterSequential)
        {
            TileEntityFilterSequential teseq = (TileEntityFilterSequential) this.tefi;
            // Add the filter buffer slots
            SlotPlacer.create(8, 110, teseq.getInventoryFilteredBuffer(), this).setSlotType(SlotType.SPECIAL).place();
        }

        // Add the normal output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tefi.getInventoryOutNormal(), 0,  8, 151));

        // Add the filtered output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tefi.getInventoryOutFiltered(), 0, 152, 151));
    }
}
