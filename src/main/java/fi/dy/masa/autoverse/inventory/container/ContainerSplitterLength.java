package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSplitterLength;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class ContainerSplitterLength extends ContainerTile
{
    private final TileEntitySplitter tes;
    public boolean secondary;

    public ContainerSplitterLength(EntityPlayer player, TileEntitySplitter te)
    {
        super(player, te);

        this.tes = te;
        this.reAddSlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 16, this.inventory, this.tes.getInventoryIn());

        ItemHandlerWrapperSplitterLength splitter = this.tes.getSplitterLength();

        // Add the sequence end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(splitter.getEndMarkerInventory(), 0, 26, 16));

        // Add the high bit marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(splitter.getBitMarkerInventory(), 0, 26, 34));

        // Add the reset sequence slots
        this.addSequenceSlots(98,  16, splitter.getResetSequence()).place();

        // Add the side 1 length config slots
        this.addSequenceSlots( 8,  65, splitter.getSequenceLength1()).setAddMatchedSlots(false).setMaxSlotsPerRow(8).place();

        // Add the side 2 length config slots
        this.addSequenceSlots( 8, 114, splitter.getSequenceLength2()).setAddMatchedSlots(false).setMaxSlotsPerRow(8).place();

        // Add the output 1 buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tes.getInventoryOut1(), 0,   8, 151));

        // Add the output 2 buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tes.getInventoryOut2(), 0, 152, 151));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            boolean isSecondary = this.tes.outputIsSecondary();

            if (this.secondary != isSecondary)
            {
                this.syncProperty(0, (byte) (isSecondary ? 0x1 : 0x0));
            }

            this.secondary = isSecondary;
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        if (id == 0)
        {
            this.secondary = value != 0;
        }
    }
}
