package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockReaderNBT;

public class ContainerBlockReaderNBT extends ContainerTile
{
    private final TileEntityBlockReaderNBT ter;
    private int lengthLast = -1;

    public ContainerBlockReaderNBT(EntityPlayer player, TileEntityBlockReaderNBT te)
    {
        super(player, te);
        this.ter = te;

        this.reAddSlots();
    }

    private void reAddSlots()
    {
        super.reAddSlots(8, 131);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), this.inventory.getSlots());

        SlotPlacer.create(8, 43, this.inventory, this).setMaxSlotsPerRow(8).place();
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient)
        {
            return;
        }

        int maxLength = this.ter.getMaxLength();

        for (int i = 0; i < this.listeners.size(); i++)
        {
            if (maxLength != this.lengthLast)
            {
                this.listeners.get(i).sendWindowProperty(this, 0, maxLength);
            }
        }

        if (maxLength != this.lengthLast)
        {
            this.reAddSlots();
        }

        this.lengthLast = maxLength;

        super.detectAndSendChanges();
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        super.updateProgressBar(id, data);

        switch (id)
        {
            case 0:
                this.ter.setMaxLength(data);
                this.reAddSlots();
                break;
        }
    }
}
