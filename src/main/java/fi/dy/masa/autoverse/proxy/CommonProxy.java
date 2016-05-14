package fi.dy.masa.autoverse.proxy;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.registry.GameRegistry;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifoPulsed;

public class CommonProxy
{
    public boolean isShiftKeyDown()
    {
        return false;
    }

    public void registerEventHandlers() { }

    public void registerTileEntities()
    {
        this.registerTileEntity(TileEntityBufferFifo.class,             ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);
        this.registerTileEntity(TileEntityBufferFifoPulsed.class,       ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_PULSED);
    }

    public void registerModels() { }

    private void registerTileEntity(Class<? extends TileEntity> clazz, String id)
    {
        GameRegistry.registerTileEntity(clazz, ReferenceNames.getPrefixedName(id));
    }
}
