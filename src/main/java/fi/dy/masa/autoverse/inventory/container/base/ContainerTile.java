package fi.dy.masa.autoverse.inventory.container.base;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class ContainerTile extends ContainerCustomSlotClick
{
    protected final TileEntityAutoverseInventory te;

    public ContainerTile(EntityPlayer player, TileEntityAutoverseInventory te)
    {
        super(player, te.getWrappedInventoryForContainer(player));

        this.te = te;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        return super.canInteractWith(player) && this.te.isInvalid() == false;
    }
}
