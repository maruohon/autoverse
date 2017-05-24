package fi.dy.masa.autoverse.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.autoverse.reference.Reference;

public class Configs
{
    public static boolean disableSounds;
    public static boolean fifoBufferOffsetSlots;

    public static File configurationFile;
    public static Configuration config;
    
    public static final String CATEGORY_GENERIC = "Generic";

    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (Reference.MOD_ID.equals(event.getModID()))
        {
            reLoadAllConfigs(false);
        }
    }

    public static void loadConfigsFromFile(File configFile)
    {
        configurationFile = configFile;
        config = new Configuration(configurationFile, null, true);
        config.load();
        reLoadAllConfigs(false);
    }

    public static Configuration loadConfigsFromFile()
    {
        //config.load();
        return config;
    }

    public static void reLoadAllConfigs(boolean reloadFromFile)
    {
        if (reloadFromFile)
        {
            config.load();
        }

        loadConfigsGeneric(config);

        if (config.hasChanged())
        {
            config.save();
        }
    }

    private static void loadConfigsGeneric(Configuration conf)
    {
        Property prop;

        prop = conf.get(CATEGORY_GENERIC, "disableSounds", false);
        prop.setComment("Disable all sounds");
        disableSounds = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "fifoBufferOffsetSlots", false);
        prop.setComment("If true, then FIFO Buffer slots are offset from their absolute inventory indices\n" +
                        "so that the current extract position is always at the top left of the GUI/slots.");
        fifoBufferOffsetSlots = prop.getBoolean();

        if (conf.hasChanged())
        {
            conf.save();
        }
    }
}
