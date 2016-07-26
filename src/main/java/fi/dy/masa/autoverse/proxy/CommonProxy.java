package fi.dy.masa.autoverse.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.GameRegistry;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityBarrel;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifoPulsed;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequentialSmart;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;

public class CommonProxy
{
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case SERVER:
                return ctx.getServerHandler().playerEntity;
            default:
                Autoverse.logger.warn("Invalid side in getPlayerFromMessageContext(): " + ctx.side);
                return null;
        }
    }

    public boolean isShiftKeyDown()
    {
        return false;
    }

    public void registerEventHandlers() { }

    public void registerTileEntities()
    {
        this.registerTileEntity(TileEntityBarrel.class,                 ReferenceNames.NAME_TILE_ENTITY_BARREL);
        this.registerTileEntity(TileEntityBufferFifo.class,             ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);
        this.registerTileEntity(TileEntityBufferFifoPulsed.class,       ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_PULSED);
        this.registerTileEntity(TileEntityFilter.class,                 ReferenceNames.NAME_TILE_ENTITY_FILTER);
        this.registerTileEntity(TileEntityFilterSequential.class,       ReferenceNames.NAME_TILE_ENTITY_FILTER_SEQUENTIAL);
        this.registerTileEntity(TileEntityFilterSequentialSmart.class,  ReferenceNames.NAME_TILE_ENTITY_FILTER_SEQ_SMART);
        this.registerTileEntity(TileEntitySequencer.class,              ReferenceNames.NAME_TILE_ENTITY_SEQUENCER);
    }

    public void registerModels() { }

    private void registerTileEntity(Class<? extends TileEntity> clazz, String id)
    {
        GameRegistry.registerTileEntity(clazz, ReferenceNames.getPrefixedName(id));
    }
}
