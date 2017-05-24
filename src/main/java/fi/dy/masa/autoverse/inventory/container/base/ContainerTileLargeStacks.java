package fi.dy.masa.autoverse.inventory.container.base;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class ContainerTileLargeStacks extends ContainerLargeStacks
{
    protected final TileEntityAutoverseInventory te;

    public ContainerTileLargeStacks(EntityPlayer player, IItemHandler inventory, TileEntityAutoverseInventory te)
    {
        super(player, inventory);

        this.te = te;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        return super.canInteractWith(player) && this.te.isInvalid() == false;
    }
}
