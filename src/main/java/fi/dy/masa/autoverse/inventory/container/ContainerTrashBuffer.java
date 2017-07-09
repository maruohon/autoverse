package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.tileentity.TileEntityTrashBin;

public class ContainerTrashBuffer extends ContainerTrashBin
{
    public ContainerTrashBuffer(EntityPlayer player, TileEntityTrashBin te)
    {
        super(player, te);

        this.reAddSlots(8, 137);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 36);
        SlotPlacer.create(8, 22, this.inventory, this).place();
    }
}
