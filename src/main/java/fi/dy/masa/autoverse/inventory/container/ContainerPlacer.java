package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockPlacer;

public class ContainerPlacer extends ContainerTile
{
    public ContainerPlacer(EntityPlayer player, TileEntityBlockPlacer te)
    {
        super(player, te);

        this.reAddSlots(8, 110);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 32);

        SlotPlacer.create(8, 22, this.te.getBaseItemHandler(), this).setMaxSlotsPerRow(8).place();
    }
}
