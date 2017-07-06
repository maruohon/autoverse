package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSplitter;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class ContainerSplitter extends ContainerTile
{
    private final TileEntitySplitter tesp;
    public boolean secondaryOutput;

    public ContainerSplitter(EntityPlayer player, TileEntitySplitter te)
    {
        super(player, te);

        this.tesp = te;
        this.reAddSlots(8, 156);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        ItemHandlerWrapperSplitter splitter = this.tesp.getSplitter();

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 30, this.inventory, this.tesp.getInventoryIn());

        // Add the end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(splitter.getEndMarkerInventory(), 0, 8, 48));

        // Add the reset sequence slots
        this.addSequenceSlots(98, 30, splitter.getResetSequence()).place();

        // Add the selection sequence 1 slots
        this.addSequenceSlots( 8, 83, splitter.getSwitchSequence1()).place();

        // Add the selection sequence 2 slots
        this.addSequenceSlots(98, 83, splitter.getSwitchSequence2()).place();

        // Add the output buffer 1 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInventoryOut1(), 0,   8, 128));

        // Add the output buffer 2 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInventoryOut2(), 0, 152, 128));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            boolean isSecondary = this.tesp.outputIsSecondaryCached();

            if (this.secondaryOutput != isSecondary)
            {
                this.syncProperty(0, (byte) (isSecondary ? 0x1 : 0x0));
            }

            this.secondaryOutput = isSecondary;
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        if (id == 0)
        {
            this.secondaryOutput = value != 0;
        }
    }
}
