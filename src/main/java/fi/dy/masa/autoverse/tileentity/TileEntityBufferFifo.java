package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiBufferFifo;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperContainer;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.container.ContainerBufferFifo;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityBufferFifo extends TileEntityAutoverseInventory implements ITickable
{
    public TileEntityBufferFifo()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, 117, 1, false, "Items", this);
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer()
    {
        // TODO
        return new ItemHandlerWrapperContainer(this.getBaseItemHandler(), this.getBaseItemHandler());
    }

    @Override
    public void update()
    {
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return new ContainerBufferFifo(player, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiBufferFifo(this.getContainer(player), this);
    }
}
