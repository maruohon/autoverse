package fi.dy.masa.autoverse.event;

import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.util.InventoryUtils;

@Mod.EventBusSubscriber
public class BlockBreakDropsHandler
{
    private static WeakReference<IItemHandler> inventory = new WeakReference<IItemHandler>(null);
    private static boolean breakingBlock = false;

    public static void setHarvestingInventory(@Nullable IItemHandler inv)
    {
        inventory = new WeakReference<IItemHandler>(inv);
        breakingBlock = inv != null;
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (breakingBlock && event.getEntity() instanceof EntityItem)
        {
            EntityItem item = (EntityItem) event.getEntity();
            ItemStack stack = item.getEntityItem();
            IItemHandler inv = inventory.get();

            if (inv != null && stack.isEmpty() == false)
            {
                stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);

                if (stack.isEmpty())
                {
                    item.setEntityItemStack(ItemStack.EMPTY);
                    event.setCanceled(true);
                }
                else
                {
                    item.setEntityItemStack(stack);
                }
            }
        }
    }
}
