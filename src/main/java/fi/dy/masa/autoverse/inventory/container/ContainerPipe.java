package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.tileentity.TileEntityPipe;

public class ContainerPipe extends ContainerTile
{
    private final TileEntityPipe tepipe;
    private int delay = -1;
    private int maxStack = -1;

    public ContainerPipe(EntityPlayer player, TileEntityPipe te)
    {
        super(player, te);

        this.tepipe = te;
        this.reAddSlots(8, 110);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 6);
        SlotPlacer.create(35, 62, this.tepipe.getBaseItemHandler(), this).place();
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            int maxStack = this.tepipe.getBaseItemHandler().getSlotLimit(0);
            int delay = this.tepipe.getDelay();

            if (maxStack != this.maxStack)
            {
                this.syncProperty(0, (byte) maxStack);
                this.maxStack = maxStack;
            }

            if (delay != this.delay)
            {
                this.syncProperty(1, (byte) delay);
                this.delay = delay;
            }
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        switch (id)
        {
            case 0:
                this.tepipe.setMaxStackSize(value);
                break;

            case 1:
                this.tepipe.setDelayFromByte((byte) value);
                break;

            default:
                super.receiveProperty(id, value);
        }
    }
}
