package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTileLargeStacks;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityBarrel;

public class ContainerBarrel extends ContainerTileLargeStacks
{
    private final TileEntityBarrel te;
    private boolean isCreative;

    public ContainerBarrel(EntityPlayer player, TileEntityBarrel te)
    {
        super(player, te.getWrappedInventoryForContainer(player), te);

        this.te = te;
        this.inventoryNonWrapped = te.getBaseItemHandler();

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
            boolean isCreative = this.te.isCreative();

            if (this.isCreative != isCreative)
            {
                this.syncProperty(0, (byte) (isCreative ? 1 : 0));
                this.isCreative = isCreative;
            }
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        if (id == 0)
        {
            this.te.setIsCreative(value == 1);
        }
        else
        {
            super.receiveProperty(id, value);
        }
    }
}
