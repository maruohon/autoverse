package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSplitter;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSplitterSelectable;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class ContainerSplitter extends ContainerTile
{
    private final TileEntitySplitter tesp;
    private final boolean selectable;
    public boolean secondaryOutput;

    public ContainerSplitter(EntityPlayer player, TileEntitySplitter te)
    {
        super(player, te);

        this.tesp = te;
        this.selectable = te.isSelectable();
        this.reAddSlots(8, this.selectable ? 156 : 125);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        ItemHandlerWrapperSplitter splitter = this.tesp.getSplitter();

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 30, this.inventory, this.tesp.getInventoryIn());

        // Add the reset sequence slots
        SlotPlacerSequence.create(98, 30, splitter.getResetSequence(), this).place();

        int posX, posY;

        if (this.selectable)
        {
            ItemHandlerWrapperSplitterSelectable splitterSelectable = (ItemHandlerWrapperSplitterSelectable) splitter;

            // Add the selection sequence 1 slots
            SlotPlacerSequence.create( 8, 83, splitterSelectable.getSwitchSequence1(), this).place();

            // Add the selection sequence 2 slots
            SlotPlacerSequence.create(98, 83, splitterSelectable.getSwitchSequence2(), this).place();

            posX = 152;
            posY = 128;
        }
        else
        {
            // Add the toggle sequence slots
            SlotPlacerSequence.create( 8, 83, splitter.getSwitchSequence1(), this).place();

            posX = 44;
            posY = 83;
        }

        // Add the output buffer 1 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInventoryOut1(), 0, 8, posY));

        // Add the output buffer 2 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInventoryOut2(), 0, posX, posY));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            boolean isSecondary = this.tesp.outputIsSecondaryCached();

            for (int i = 0; i < this.listeners.size(); i++)
            {
                if (this.secondaryOutput != isSecondary)
                {
                    this.listeners.get(i).sendWindowProperty(this, 0, isSecondary ? 0x1 : 0x0);
                }
            }

            this.secondaryOutput = isSecondary;
        }

        super.detectAndSendChanges();
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        switch (id)
        {
            case 0:
                this.secondaryOutput = data != 0;
                break;
        }

        super.updateProgressBar(id, data);
    }
}
