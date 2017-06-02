package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityBreaker;

public class ContainerBreaker extends ContainerTile
{
    public ContainerBreaker(EntityPlayer player, TileEntityBreaker te)
    {
        super(player, te);

        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 84);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 9);
        IItemHandler inv = this.te.getBaseItemHandler();

        for (int row = 0; row < 3; ++row)
        {
            for (int column = 0; column < 3; ++column)
            {
                this.addSlotToContainer(new SlotItemHandlerGeneric(inv, row * 3 + column, 62 + column * 18, 17 + row * 18));
            }
        }
    }
}
