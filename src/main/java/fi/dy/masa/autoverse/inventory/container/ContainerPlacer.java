package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityPlacer;

public class ContainerPlacer extends ContainerTile
{
    public ContainerPlacer(EntityPlayer player, TileEntityPlacer te)
    {
        super(player, te);

        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 110);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int sizeOrig = this.inventorySlots.size();
        IItemHandler inv = this.te.getBaseItemHandler();

        for (int row = 0; row < 4; ++row)
        {
            for (int column = 0; column < 8; ++column)
            {
                this.addSlotToContainer(new SlotItemHandlerGeneric(inv, row * 8 + column, 8 + column * 18, 22 + row * 18));
            }
        }

        this.customInventorySlots = new MergeSlotRange(sizeOrig, this.inventorySlots.size() - sizeOrig);
    }
}
