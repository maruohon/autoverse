package fi.dy.masa.autoverse;

import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
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

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION, certificateFingerprint = Reference.FINGERPRINT,
    guiFactory = "fi.dy.masa.autoverse.config.AutoverseGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/autoverse/master/update.json",
    acceptedMinecraftVersions = "1.12")
public class Autoverse
{
    @Mod.Instance(Reference.MOD_ID)
    public static Autoverse instance;

    @SidedProxy(clientSide = "fi.dy.masa.autoverse.proxy.ClientProxy", serverSide = "fi.dy.masa.autoverse.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);
    public static final Random RAND = new Random(System.currentTimeMillis());

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
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

    @Mod.EventHandler
    public void onFingerPrintViolation(FMLFingerprintViolationEvent event)
    {
        // Not running in a dev environment
        if (event.isDirectory() == false)
        {
            logger.warn("*********************************************************************************************");
            logger.warn("*****                                    WARNING                                        *****");
            logger.warn("*****                                                                                   *****");
            logger.warn("*****   The signature of the mod file '{}' does not match the expected fingerprint!     *****", event.getSource().getName());
            logger.warn("*****   This might mean that the mod file has been tampered with!                       *****");
            logger.warn("*****   If you did not download the mod {} directly from Curse/CurseForge,       *****", Reference.MOD_NAME);
            logger.warn("*****   or using one of the well known launchers, and you did not                       *****");
            logger.warn("*****   modify the mod file at all yourself, then it's possible,                        *****");
            logger.warn("*****   that it may contain malware or other unwanted things!                           *****");
            logger.warn("*********************************************************************************************");
        }
    }
}
