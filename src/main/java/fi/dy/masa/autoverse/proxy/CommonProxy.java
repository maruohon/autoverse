package fi.dy.masa.autoverse.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.GameRegistry;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.*;

public class CommonProxy
{
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case SERVER:
                return ctx.getServerHandler().player;
            default:
                Autoverse.logger.warn("Invalid side in getPlayerFromMessageContext(): " + ctx.side);
                return null;
        }
    }

    public ModFixs getDataFixer()
    {
        // On a server, the DataFixer gets created for and is stored inside MinecraftServer,
        // but in single player the DataFixer is stored in the client Minecraft class
        // over world reloads.
        return FMLCommonHandler.instance().getDataFixer().init(Reference.MOD_ID, Autoverse.DATA_FIXER_VERSION);
    }

    public boolean isShiftKeyDown()
    {
        return false;
    }

    public boolean isControlKeyDown()
    {
        return false;
    }

    public boolean isAltKeyDown()
    {
        return false;
    }

    public void registerColorHandlers() { }

    public void registerEventHandlers() { }

    public void registerKeyBindings() { }

    public void registerModels() { }

    public void registerTileEntities()
    {
        this.registerTileEntity(TileEntityBarrel.class,                 ReferenceNames.NAME_TILE_ENTITY_BARREL);
        this.registerTileEntity(TileEntityBreaker.class,                ReferenceNames.NAME_BLOCK_BREAKER);
        this.registerTileEntity(TileEntityBufferFifo.class,             ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);
        this.registerTileEntity(TileEntityBufferFifoPulsed.class,       ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_PULSED);
        this.registerTileEntity(TileEntityCrafter.class,                ReferenceNames.NAME_BLOCK_CRAFTER);
        this.registerTileEntity(TileEntityFilter.class,                 ReferenceNames.NAME_TILE_ENTITY_FILTER);
        this.registerTileEntity(TileEntityFilterSequential.class,       ReferenceNames.NAME_TILE_ENTITY_FILTER_SEQUENTIAL);
        this.registerTileEntity(TileEntityPlacer.class,                 ReferenceNames.NAME_BLOCK_PLACER);
        this.registerTileEntity(TileEntitySequencer.class,              ReferenceNames.NAME_TILE_ENTITY_SEQUENCER);
        this.registerTileEntity(TileEntitySplitter.class,               ReferenceNames.NAME_TILE_ENTITY_SPLITTER);
    }

    private void registerTileEntity(Class<? extends TileEntity> clazz, String id)
    {
        GameRegistry.registerTileEntity(clazz, Reference.MOD_ID + ":" + id);
    }
}
