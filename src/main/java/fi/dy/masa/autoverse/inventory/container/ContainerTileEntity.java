package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverseInventory;

public class ContainerTileEntity extends ContainerAutoverse
{
    protected TileEntityAutoverseInventory te;

    public ContainerTileEntity(EntityPlayer player, TileEntityAutoverseInventory te)
    {
        super(player, te.getWrappedInventoryForContainer());
        this.te = te;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        return super.canInteractWith(player) && this.te.isInvalid() == false;
    }
}
