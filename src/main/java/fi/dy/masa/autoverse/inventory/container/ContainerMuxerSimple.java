package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityMuxer;

public class ContainerMuxerSimple extends ContainerTile
{
    protected final TileEntityMuxer tem;
    public boolean secondaryInput;

    public ContainerMuxerSimple(EntityPlayer player, TileEntityMuxer te)
    {
        super(player, te);

        this.tem = te;
        this.reAddSlots();
    }

    protected void reAddSlots()
    {
        this.reAddSlots(8, 64);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the first input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        // Add the input buffer 1 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tem.getInventoryInput1(), 0,   8, 30));

        // Add the input buffer 2 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tem.getInventoryInput2(), 0,  62, 30));

        // Add the output buffer
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tem.getInventoryOutput(), 0, 116, 30));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            boolean isSecondary = this.tem.getCurrentInputIsSecondary();

            if (this.secondaryInput != isSecondary)
            {
                this.syncProperty(0, (byte) (isSecondary ? 0x1 : 0x0));
            }

            this.secondaryInput = isSecondary;
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        if (id == 0)
        {
            this.secondaryInput = value != 0;
        }
    }
}
