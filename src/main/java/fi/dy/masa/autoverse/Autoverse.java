package fi.dy.masa.autoverse;

import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.gui.AutoverseGuiHandler;
import fi.dy.masa.autoverse.proxy.CommonProxy;
import fi.dy.masa.autoverse.reference.Reference;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION,
    guiFactory = "fi.dy.masa.autoverse.config.AutoverseGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/autoverse/master/update.json",
    clientSideOnly=true, acceptedMinecraftVersions = "1.9")
public class Autoverse
{
    @Instance(Reference.MOD_ID)
    public static Autoverse instance;

    @SidedProxy(clientSide = "fi.dy.masa.autoverse.proxy.ClientProxy", serverSide = "fi.dy.masa.autoverse.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        instance = this;
        logger = event.getModLog();
        Configs.loadConfigsFromFile(event.getSuggestedConfigurationFile());
        AutoverseBlocks.init();

        proxy.registerEventHandlers();
        proxy.registerModels();
        proxy.registerTileEntities();

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new AutoverseGuiHandler());
    }
}
