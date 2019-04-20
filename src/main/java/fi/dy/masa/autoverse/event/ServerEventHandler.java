package fi.dy.masa.autoverse.event;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.event.tasks.scheduler.PlayerTaskScheduler;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
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
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getHand() == EnumHand.OFF_HAND && event.getEntityPlayer().isSneaking())
        {
            World world = event.getWorld();
            TileEntity te = world.getTileEntity(event.getPos());

            if (te != null && te instanceof TileEntityAutoverse)
            {
                BlockPos pos = event.getPos();
                EnumFacing side = event.getFace();
                EntityPlayer player = event.getEntityPlayer();
                EnumHand hand = event.getHand();

                if (((TileEntityAutoverse) te).onRightClickBlock(world, pos, side, player, hand))
                {
                    event.setCancellationResult(EnumActionResult.SUCCESS);
                    event.setCanceled(true);
                }
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
