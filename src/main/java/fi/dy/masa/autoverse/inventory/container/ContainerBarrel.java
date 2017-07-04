package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTileLargeStacks;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityBarrel;

public class ContainerBarrel extends ContainerTileLargeStacks
{
    public ContainerBarrel(EntityPlayer player, TileEntityBarrel te)
    {
        super(player, te.getWrappedInventoryForContainer(player), te);

        this.inventoryNonWrapped = te.getBaseItemHandler();

        this.reAddSlots(8, 51);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.te.getBaseItemHandler(), 0, 80, 19));

        this.customInventorySlots = new MergeSlotRange(0, 1);
    }
}
