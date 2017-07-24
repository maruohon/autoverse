package fi.dy.masa.autoverse.event;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
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

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if (event.getEntityPlayer().capabilities.isCreativeMode && event.getEntityPlayer().isSneaking())
        {
            Block block = event.getWorld().getBlockState(event.getPos()).getBlock();

            if (block == AutoverseBlocks.PIPE)
            {
                block.onBlockClicked(event.getWorld(), event.getPos(), event.getEntityPlayer());
                event.setCanceled(true);
            }
        }
    }
}
