package fi.dy.masa.autoverse;

import java.util.Random;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import fi.dy.masa.autoverse.commands.CommandLoadConfigs;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.gui.AutoverseGuiHandler;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.proxy.CommonProxy;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.PlacementProperties;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION,
    guiFactory = "fi.dy.masa.autoverse.config.AutoverseGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/autoverse/master/update.json",
    acceptedMinecraftVersions = "1.12")
public class Autoverse
{
    @Mod.Instance(Reference.MOD_ID)
    public static Autoverse instance;

    @SidedProxy(clientSide = "fi.dy.masa.autoverse.proxy.ClientProxy", serverSide = "fi.dy.masa.autoverse.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger;

    public static final Random RAND = new Random(System.currentTimeMillis());

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        Configs.loadConfigsFromFile(event.getSuggestedConfigurationFile());
        PacketHandler.init();

        proxy.registerEventHandlers();

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new AutoverseGuiHandler());
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        // Should these be moved to the ServerAboutToStartEvent?
        Configs.reLoadAllConfigs(true);
        PlacementProperties.getInstance().readFromDisk();

        event.registerServerCommand(new CommandLoadConfigs());
    }
}
