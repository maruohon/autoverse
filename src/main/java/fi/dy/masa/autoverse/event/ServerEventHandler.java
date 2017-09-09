package fi.dy.masa.autoverse.event;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.event.tasks.scheduler.PlayerTaskScheduler;
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
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        PlayerTaskScheduler.getInstance().removeTask(event.player, null);
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if (event.getEntityPlayer().capabilities.isCreativeMode && event.getEntityPlayer().isSneaking())
        {
            Block block = event.getWorld().getBlockState(event.getPos()).getBlock();

            if (block == AutoverseBlocks.PIPE || block == AutoverseBlocks.CIRCUIT)
            {
                block.onBlockClicked(event.getWorld(), event.getPos(), event.getEntityPlayer());
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event)
    {
        EntityPlayer player = event.player;

        if (event.side == Side.CLIENT || player.getEntityWorld().isRemote)
        {
            return;
        }

        PlayerTaskScheduler.getInstance().runTasks(player.getEntityWorld(), player);
    }
}
