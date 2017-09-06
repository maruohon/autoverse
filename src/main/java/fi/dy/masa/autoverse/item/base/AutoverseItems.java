package fi.dy.masa.autoverse.item.base;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.item.ItemBlockPlacerConfigurator;
import fi.dy.masa.autoverse.item.ItemWand;
import fi.dy.masa.autoverse.reference.Reference;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class AutoverseItems
{
    public static final ItemAutoverse CONFIGURATOR = new ItemBlockPlacerConfigurator();
    public static final ItemAutoverse WAND = new ItemWand();

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event)
    {
        IForgeRegistry<Item> registry = event.getRegistry();

        registerItem(registry, CONFIGURATOR,    Configs.disableItemBlockPlacerConfigurator);
        registerItem(registry, WAND,            Configs.disableItemWand);
    }

    private static void registerItem(IForgeRegistry<Item> registry, ItemAutoverse item, boolean isDisabled)
    {
        if (isDisabled == false)
        {
            item.setRegistryName(Reference.MOD_ID + ":" + item.getItemName());
            registry.register(item);
        }
        else
        {
            item.setEnabled(false);
        }
    }
}
