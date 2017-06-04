package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockReaderNBT;

public class ContainerBlockReaderNBT extends ContainerTile
{
    private final TileEntityBlockReaderNBT ter;
    private int lengthLast = -1;

    public ContainerBlockReaderNBT(EntityPlayer player, TileEntityBlockReaderNBT te)
    {
        super(player, te);
        this.ter = te;

        this.detectAndSendChanges();
        this.reAddSlots();
    }

    private void reAddSlots()
    {
        this.inventorySlots.clear();
        this.inventoryItemStacks.clear();
        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 131);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int posX = 8;
        int posY = 43;
        final int slots = this.inventory.getSlots();

        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), slots);

        for (int slot = 0, x = posX; slot < slots; slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, slot, x, posY));
            x += 18;

            if (slot % 8 == 7)
            {
                x = posX;
                posY += 18;
            }
        }
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
                this.listeners.get(i).sendProgressBarUpdate(this, 0, maxLength);
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
