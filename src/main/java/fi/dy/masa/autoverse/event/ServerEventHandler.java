package fi.dy.masa.autoverse.event;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import fi.dy.masa.autoverse.util.PlacementProperties;

public class ServerEventHandler
{
    @SubscribeEvent
    public void onWorldSaveEvent(WorldEvent.Save event)
    {
        PlacementProperties.getInstance().writeToDisk();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.player instanceof EntityPlayerMP)
        {
            PlacementProperties.getInstance().syncAllDataForPlayer((EntityPlayerMP) event.player);
        }
    }
}
