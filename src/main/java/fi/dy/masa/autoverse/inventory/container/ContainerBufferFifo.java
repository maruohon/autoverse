package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.slot.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;

public class ContainerBufferFifo extends ContainerCustomSlotClick
{
    public ContainerBufferFifo(EntityPlayer player, TileEntityBufferFifo te)
    {
        super(player, te);

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
}
