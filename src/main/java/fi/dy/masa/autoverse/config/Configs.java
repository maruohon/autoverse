package fi.dy.masa.autoverse.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.autoverse.reference.Reference;

public class Configs
{
    public static File configurationFile;
    public static Configuration config;
    
    public static final String CATEGORY_GENERIC = "Generic";

    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (Reference.MOD_ID.equals(event.getModID()) == true)
        {
            loadConfigs(config);
        }
    }

    public static void loadConfigsFromFile(File configFile)
    {
        configurationFile = configFile;
        config = new Configuration(configFile, "0.1.0", true);
        config.load();

        loadConfigs(config);
    }

    public static void loadConfigs(Configuration conf)
    {
        //Property prop;

        //prop = conf.get(CATEGORY_GENERIC, "fontColor", 0xE0E0E0);
        //prop.setComment("Font color (default: 0xE0E0E0 = 14737632)");
        //fontColor = prop.getInt();

        if (conf.hasChanged() == true)
        {
            conf.save();
        }
    }
}
