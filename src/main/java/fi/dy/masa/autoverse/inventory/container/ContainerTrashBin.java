package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityTrashBin;

public class ContainerTrashBin extends ContainerTile
{
    private final TileEntityTrashBin te;
    private long count = -1;

    public ContainerTrashBin(EntityPlayer player, TileEntityTrashBin te)
    {
        super(player, te);

        this.te = te;
        this.reAddSlots(8, 86);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 1);
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, 0, 80, 24));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            long count = this.te.getTrashedCount();

            if (this.count != count)
            {
                this.syncProperty(0, count);
                this.count = count;
            }
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receivePropertyLong(int id, long value)
    {
        if (id == 0)
        {
            this.count = value;
        }
        else
        {
            super.receivePropertyLong(id, value);
        }
    }

    public long getTrashedCount()
    {
        return this.count;
    }
}
