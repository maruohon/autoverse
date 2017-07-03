package fi.dy.masa.autoverse;

import java.util.Random;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.datafix.FixTypes;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.commands.CommandLoadConfigs;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.gui.AutoverseGuiHandler;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.proxy.CommonProxy;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.PlacementProperties;
import fi.dy.masa.autoverse.util.datafixer.TileEntityID;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION,
    guiFactory = "fi.dy.masa.autoverse.config.AutoverseGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/autoverse/master/update.json",
    acceptedMinecraftVersions = "1.11.2")
public class Autoverse
{
    public static final int DATA_FIXER_VERSION = 922;

    @Instance(Reference.MOD_ID)
    public static Autoverse instance;

    @SidedProxy(clientSide = "fi.dy.masa.autoverse.proxy.ClientProxy", serverSide = "fi.dy.masa.autoverse.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger;

    public static final Random RAND = new Random(System.currentTimeMillis());

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        instance = this;
        logger = event.getModLog();
        Configs.loadConfigsFromFile(event.getSuggestedConfigurationFile());
        AutoverseBlocks.init();
        PacketHandler.init();

        proxy.registerEventHandlers();
        proxy.registerModels();
        proxy.registerTileEntities();

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new AutoverseGuiHandler());
    }

    @Mod.EventHandler
    public void onServerAboutToStartEvent(FMLServerAboutToStartEvent event)
    {
        // Register data fixers
        ModFixs dataFixer = proxy.getDataFixer();
        TileEntityID renames = new TileEntityID();
        dataFixer.registerFix(FixTypes.BLOCK_ENTITY, renames);
        dataFixer.registerFix(FixTypes.ITEM_INSTANCE, renames);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        Configs.reLoadAllConfigs(true);
        PlacementProperties.getInstance().readFromDisk();
        event.registerServerCommand(new CommandLoadConfigs());
    }

    /*
    @Mod.EventHandler
    public void onMissingMappingEvent(FMLMissingMappingsEvent event)
    {
        List<MissingMapping> list = event.get();
        Map<String, String> renameMap = TileEntityID.getMap();

        for (MissingMapping mapping : list)
        {
            if (mapping.type == GameRegistry.Type.BLOCK)
            {
                ResourceLocation oldLoc = mapping.resourceLocation;

                if (oldLoc.getResourceDomain().equals(Reference.MOD_ID))
                {
                    String newName = renameMap.get(oldLoc.toString());

                    if (newName != null)
                    {
                        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(newName));

                        if (block != null && block != Blocks.AIR)
                        {
                            mapping.remap(block);
                            logger.info("Re-mapped block '{}' to '{}'", oldLoc, newName);
                        }
                    }
                }
            }
            else if (mapping.type == GameRegistry.Type.ITEM)
            {
                ResourceLocation oldLoc = mapping.resourceLocation;

                if (oldLoc.getResourceDomain().equals(Reference.MOD_ID))
                {
                    String newName = renameMap.get(oldLoc.toString());

                    if (newName != null)
                    {
                        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(newName));

                        if (item != null && item != Items.AIR)
                        {
                            mapping.remap(item);
                            logger.info("Re-mapped item '{}' to '{}'", oldLoc, newName);
                        }
                    }
                }
            }
        }
    }
    */
}
