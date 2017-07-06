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

    public static boolean disableBlockBarrel;
    public static boolean disableBlockBlockBreaker;
    public static boolean disableBlockBlockDetector;
    public static boolean disableBlockBlockPlacer;
    public static boolean disableBlockBlockReader;
    public static boolean disableBlockBuffer;
    public static boolean disableBlockCrafter;
    public static boolean disableBlockFilter;
    public static boolean disableBlockInventoryReader;
    public static boolean disableBlockRedstoneEmitter;
    public static boolean disableBlockSequenceDetector;
    public static boolean disableBlockSequencer;
    public static boolean disableBlockSplitter;

    public static File configurationFile;
    public static Configuration config;
    
    public static final String CATEGORY_CLIENT = "Client";
    public static final String CATEGORY_GENERIC = "Generic";
    public static final String CATEGORY_BLOCK_DISABLE = "DisableBlocks";

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
        loadConfigsBlockDisable(config);

        if (config.hasChanged())
        {
            config.save();
        }
    }

    private static void loadConfigsGeneric(Configuration conf)
    {
        Property prop;

        prop = conf.get(CATEGORY_CLIENT, "fifoBufferOffsetSlots", false);
        prop.setComment("If true, then FIFO Buffer slots are offset from their absolute inventory indices\n" +
                        "so that the current extract position is always at the top left of the GUI/slots.");
        fifoBufferOffsetSlots = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "disableSounds", false);
        prop.setComment("Disable all sounds");
        disableSounds = prop.getBoolean();

        if (conf.hasChanged())
        {
            conf.save();
        }
    }

    private static void loadConfigsBlockDisable(Configuration conf)
    {
        conf.getCategory(CATEGORY_BLOCK_DISABLE).setRequiresMcRestart(true);

        disableBlockBarrel                  = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockBarrel", false).getBoolean();
        disableBlockBlockBreaker            = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockBlockBreaker", false).getBoolean();
        disableBlockBlockDetector           = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockBlockDetector", false).getBoolean();
        disableBlockBlockPlacer             = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockBlockPlacer", false).getBoolean();
        disableBlockBlockReader             = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockBlockReader", false).getBoolean();
        disableBlockBuffer                  = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockBuffer", false).getBoolean();
        disableBlockCrafter                 = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockCrafter", false).getBoolean();
        disableBlockFilter                  = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockFilter", false).getBoolean();
        disableBlockInventoryReader         = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockInventoryReader", false).getBoolean();
        disableBlockRedstoneEmitter         = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockRedstoneEmitter", false).getBoolean();
        disableBlockSequenceDetector        = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockSequenceDetector", false).getBoolean();
        disableBlockSequencer               = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockSequencer", false).getBoolean();
        disableBlockSplitter                = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockSplitter", false).getBoolean();

        if (conf.hasChanged())
        {
            conf.save();
        }
    }
}
