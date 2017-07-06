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
    private int invSize = -1;

    public ContainerFilter(EntityPlayer player, TileEntityFilter te)
    {
        super(player, te);

        this.tefi = te;
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
        this.addSideDependentSlot(0, 8, 16, this.inventory, this.tefi.getInventoryInput());

        ItemHandlerWrapperFilter filter = this.tefi.getInventoryFilter();

        // Add the end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(filter.getEndMarkerInventory(), 0, 26, 16));

        // Add the Reset Sequence slots
        this.addSequenceSlots(98, 16, filter.getResetSequence()).place();

        // Add the Filter slots
        this.addSequenceSlots(8, 63, filter.getFilterSequence()).place();

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

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false && this.tefi instanceof TileEntityFilterSequential)
        {
            int invSize = ((TileEntityFilterSequential) this.tefi).getInventoryFilteredBuffer().getSlots();

            if (invSize != this.invSize)
            {
                this.syncProperty(0, (byte) invSize);
                this.reAddSlots();
                this.invSize = invSize;

                this.forceSyncAll = true;
                super.detectAndSendChanges();
                this.forceSyncAll = false;
            }
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        if (id == 0)
        {
            ((TileEntityFilterSequential) this.tefi).getInventoryFilteredBuffer().setInventorySize(value);
            this.reAddSlots();
        }
    }
}
