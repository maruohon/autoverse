package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.gui.client.GuiRedstoneEmitterAdvanced;
import fi.dy.masa.autoverse.inventory.container.ContainerRedstoneEmitterAdvanced;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperRedstoneEmitterAdvanced;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequenceBase;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityRedstoneEmitterAdvanced extends TileEntityRedstoneEmitter
{
    private ItemHandlerWrapperRedstoneEmitterAdvanced emitter;

    public TileEntityRedstoneEmitterAdvanced()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_REDSTONE_EMITTER_ADVANCED);
    }

    @Override
    protected void createEmitterInventory()
    {
        this.emitter = new ItemHandlerWrapperRedstoneEmitterAdvanced(this.getInventoryIn(), this.getInventoryOut(), this);
    }

    public ItemHandlerWrapperRedstoneEmitterAdvanced getEmitterHandlerAdvanced()
    {
        return this.emitter;
    }

    @Override
    public ItemHandlerWrapperSequenceBase getEmitterHandlerBase()
    {
        return this.emitter;
    }

    @Override
    public ContainerRedstoneEmitterAdvanced getContainer(EntityPlayer player)
    {
        return new ContainerRedstoneEmitterAdvanced(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiRedstoneEmitterAdvanced(this.getContainer(player), this);
    }
}
