package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ICrafting;
import fi.dy.masa.autoverse.inventory.slot.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;

public class ContainerBufferFifo extends ContainerCustomSlotClick
{
    private final TileEntityBufferFifo tefifo;
    public int insertPos;
    public int extractPos;

    public ContainerBufferFifo(EntityPlayer player, TileEntityBufferFifo te)
    {
        super(player, te);

        this.tefifo = te;
        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(48, 177);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int posX = 12;
        int posY = 13;

        for (int row = 0; row <= 8; row++)
        {
            for (int col = 0; col <= 12; col++)
            {
                this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, row * 13 + col, posX + col * 18, posY + row * 18));
            }
        }

        this.customInventorySlots = new MergeSlotRange(0, this.inventorySlots.size());
    }

    @Override
    public void onCraftGuiOpened(ICrafting listener)
    {
        super.onCraftGuiOpened(listener);

        listener.sendProgressBarUpdate(this, 0, this.insertPos);
        listener.sendProgressBarUpdate(this, 1, this.extractPos);
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        for (int i = 0; i < this.listeners.size(); ++i)
        {
            ICrafting listener = this.listeners.get(i);

            if (this.tefifo.getInsertSlot() != this.insertPos)
            {
                listener.sendProgressBarUpdate(this, 0, this.tefifo.getInsertSlot());
            }

            if (this.tefifo.getExtractSlot() != this.extractPos)
            {
                listener.sendProgressBarUpdate(this, 1, this.tefifo.getExtractSlot());
            }
        }

        this.insertPos = this.tefifo.getInsertSlot();
        this.extractPos = this.tefifo.getExtractSlot();
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        super.updateProgressBar(id, data);

        switch (id)
        {
            case 0: this.insertPos = data; break;
            case 1: this.extractPos = data; break;
            default:
        }
    }
}
