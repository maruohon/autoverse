package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.slot.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverseInventory;

public class ContainerBarrel extends ContainerLargeStacks
{
    public ContainerBarrel(EntityPlayer player, TileEntityAutoverseInventory te)
    {
        super(player, te);

        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 45);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.te.getBaseItemHandler(), 0, 80, 17));

        this.customInventorySlots = new MergeSlotRange(0, 1);
    }
}
