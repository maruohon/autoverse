package fi.dy.masa.autoverse.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.event.BlockBreakDropsHandler;
import fi.dy.masa.autoverse.event.ServerEventHandler;

public class CommonProxy
{
    public EntityPlayer getClientPlayer()
    {
        return null;
    }

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

    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new BlockBreakDropsHandler());
        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
    }
}
