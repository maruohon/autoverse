package fi.dy.masa.autoverse.config;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.autoverse.reference.Reference;

public class Configs
{
    public static boolean barrelsSpillContents;
    public static boolean disableSounds;
    public static boolean fifoBufferOffsetSlots;

    public static int barrelMaxTier;
    public static int pipeDefaultDelay;
    public static int pipeMaxStackSize;
    public static int pipeMinimumDelay;

    public static boolean disableBlockBarrel;
    public static boolean disableBlockBlockBreaker;
    public static boolean disableBlockBlockDetector;
    public static boolean disableBlockBlockPlacer;
    public static boolean disableBlockBlockReader;
    public static boolean disableBlockBuffer;
    public static boolean disableBlockCircuit;
    public static boolean disableBlockCrafter;
    public static boolean disableBlockFilter;
    public static boolean disableBlockInventoryReader;
    public static boolean disableBlockMachineFrame;
    public static boolean disableBlockMuxer;
    public static boolean disableBlockPipe;
    public static boolean disableBlockRedstoneEmitter;
    public static boolean disableBlockSensor;
    public static boolean disableBlockSequenceDetector;
    public static boolean disableBlockSequencer;
    public static boolean disableBlockSplitter;
    public static boolean disableBlockTrash;

    public static boolean disableItemBlockPlacerConfigurator;
    public static boolean disableItemWand;

    private static String[] blockPlacerIntegerPropertyWhitelistNames;
    private static final Set<ResourceLocation> BLOCK_PLACER_WHITELIST_INTEGER = new HashSet<>();

    public static BreakerPattern blockBreakerPattern = BreakerPattern.ADJACENT;

    public static File configurationFile;
    public static Configuration config;
    
    public static final String CATEGORY_CLIENT = "Client";
    public static final String CATEGORY_GENERIC = "Generic";
    public static final String CATEGORY_BLOCK_PLACER = "BlockPlacer";
    public static final String CATEGORY_BLOCK_DISABLE = "DisableBlocks";
    public static final String CATEGORY_ITEM_DISABLE = "DisableItems";

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
        reLoadAllConfigs(true);
    }

    public static void reLoadAllConfigs(boolean reloadFromFile)
    {
        if (reloadFromFile)
        {
            config.load();
        }

        loadConfigsGeneric(config);
        loadConfigsBlockDisable(config);
        loadConfigsItemDisable(config);
        loadConfigsBlockPlacer(config);

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

        prop = conf.get(CATEGORY_GENERIC, "barrelMaxTier", 15);
        prop.setComment("The maximum allowed tier of a Barrel. Note: The value here is 0 - 15, but the displayd value in-game is 1 - 16");
        barrelMaxTier = MathHelper.clamp(prop.getInt(), 0, 15);

        prop = conf.get(CATEGORY_GENERIC, "barrelsSpillContents", false);
        prop.setComment("If true, then broken Barrels will spill their contents instead of storing them in the item");
        barrelsSpillContents = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "blockBreakerPattern", 0);
        prop.setComment("The block break pattern of the Greedy variant of Block Breaker.\n" +
                        "0 = adjacent blocks only (default)\n" +
                        "1 = a 3x3 shape around the breaker (not behind itself though)\n" +
                        "2 = a 5x5 shape around the breaker (not behind itself though)");
        blockBreakerPattern = BreakerPattern.values()[MathHelper.clamp(prop.getInt(), 0, BreakerPattern.values().length - 1)];

        prop = conf.get(CATEGORY_GENERIC, "disableSounds", false);
        prop.setComment("Disable all sounds");
        disableSounds = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "pipeDefaultDelay", 4).setRequiresMcRestart(true);
        prop.setComment("The default delay of newly placed Pipes, and of the placement property which hasn't been changed yet. Range: 1 - 127");
        pipeDefaultDelay = MathHelper.clamp(prop.getInt(), 1, 127);

        prop = conf.get(CATEGORY_GENERIC, "pipeMaxStackSize", 64).setRequiresMcRestart(true);
        prop.setComment("The maximum stack size the pipes can take. Range: 1 - 64");
        pipeMaxStackSize = MathHelper.clamp(prop.getInt(), 1, 64);

        prop = conf.get(CATEGORY_GENERIC, "pipeMinimumDelay", 1).setRequiresMcRestart(true);
        prop.setComment("The minimum allowed delay of any Pipes. Range: 1 - 127");
        pipeMinimumDelay = MathHelper.clamp(prop.getInt(), 1, 127);

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
        disableBlockCircuit                 = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockCircuit", false).getBoolean();
        disableBlockCrafter                 = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockCrafter", false).getBoolean();
        disableBlockFilter                  = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockFilter", false).getBoolean();
        disableBlockInventoryReader         = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockInventoryReader", false).getBoolean();
        disableBlockMachineFrame            = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockMachineFrame", false).getBoolean();
        disableBlockMuxer                   = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockMuxer", false).getBoolean();
        disableBlockPipe                    = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockPipe", false).getBoolean();
        disableBlockRedstoneEmitter         = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockRedstoneEmitter", false).getBoolean();
        disableBlockSensor                  = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockSensor", false).getBoolean();
        disableBlockSequenceDetector        = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockSequenceDetector", false).getBoolean();
        disableBlockSequencer               = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockSequencer", false).getBoolean();
        disableBlockSplitter                = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockSplitter", false).getBoolean();
        disableBlockTrash                   = conf.get(CATEGORY_BLOCK_DISABLE, "disableBlockTrash", false).getBoolean();

        if (conf.hasChanged())
        {
            conf.save();
        }
    }

    private static void loadConfigsItemDisable(Configuration conf)
    {
        conf.getCategory(CATEGORY_ITEM_DISABLE).setRequiresMcRestart(true);

        disableItemBlockPlacerConfigurator  = conf.get(CATEGORY_ITEM_DISABLE, "disableItemBlockPlacerConfigurator", false).getBoolean();
        disableItemWand                     = conf.get(CATEGORY_ITEM_DISABLE, "disableItemWand", false).getBoolean();

        if (conf.hasChanged())
        {
            conf.save();
        }
    }

    private static void loadConfigsBlockPlacer(Configuration conf)
    {
        Property prop;

        prop = conf.get(CATEGORY_BLOCK_PLACER, "blockPlacerIntegerPropertyWhitelist", new String[] { "minecraft:repeater" });
        prop.setComment("List of blocks for which the Programmable Block Placer should be allowed to set\n" +
                        "PropertyInteger type blockstate properties to any values.\n" +
                        "This list is required to prevent exploits like placing Cauldrons already filled with water,\n" +
                        "or setting crops (like Carrots) directly to their fully-grown state.");
        blockPlacerIntegerPropertyWhitelistNames = prop.getStringList();
        BLOCK_PLACER_WHITELIST_INTEGER.clear();

        for (String name : blockPlacerIntegerPropertyWhitelistNames)
        {
            BLOCK_PLACER_WHITELIST_INTEGER.add(new ResourceLocation(name));
        }

        if (conf.hasChanged())
        {
            conf.save();
        }
    }

    public static boolean isBlockWhitelistedForIntegerProperties(Block block)
    {
        return BLOCK_PLACER_WHITELIST_INTEGER.contains(block.getRegistryName());
    }

    public enum BreakerPattern
    {
        ADJACENT,
        SHAPE_3x3,
        SHAPE_5x5;
    }
}
