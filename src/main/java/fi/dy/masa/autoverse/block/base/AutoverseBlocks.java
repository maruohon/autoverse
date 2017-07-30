package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import fi.dy.masa.autoverse.block.*;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.*;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class AutoverseBlocks
{
    public static final BlockAutoverse BARREL                   = new BlockBarrel(ReferenceNames.NAME_BLOCK_BARREL,                                 4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse BLOCK_BREAKER            = new BlockBreaker(ReferenceNames.NAME_BLOCK_BREAKER,                               4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse BLOCK_DETECTOR           = new BlockDetector(ReferenceNames.NAME_BLOCK_DETECTOR,                             4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse BLOCK_PLACER             = new BlockPlacer(ReferenceNames.NAME_BLOCK_PLACER,                                 4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse BLOCK_READER             = new BlockReader(ReferenceNames.NAME_BLOCK_BLOCK_READER,                           4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse BUFFER                   = new BlockBuffer(ReferenceNames.NAME_BLOCK_BUFFER,                                 4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse CRAFTER                  = new BlockCrafter(ReferenceNames.NAME_BLOCK_CRAFTER,                               4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse FILTER                   = new BlockFilter(ReferenceNames.NAME_BLOCK_FILTER,                                 4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse INVENTORY_READER         = new BlockInventoryReader(ReferenceNames.NAME_BLOCK_INVENTORY_READER,              4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse MACHINE_FRAME            = new BlockMachineFrame(ReferenceNames.NAME_BLOCK_MACHINE_FRAME,                    4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse MUXER                    = new BlockMuxer(ReferenceNames.NAME_BLOCK_MUXER,                                   4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse PIPE                     = new BlockPipe(ReferenceNames.NAME_BLOCK_PIPE,                                     4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse REDSTONE_EMITTER         = new BlockRedstoneEmitter(ReferenceNames.NAME_BLOCK_REDSTONE_EMITTER,              4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse SENSOR                   = new BlockSensor(ReferenceNames.NAME_BLOCK_SENSOR,                                 4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse SEQUENCE_DETECTOR        = new BlockSequenceDetector(ReferenceNames.NAME_BLOCK_SEQUENCE_DETECTOR,            4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse SEQUENCER                = new BlockSequencer(ReferenceNames.NAME_BLOCK_SEQUENCER,                           4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse SPLITTER                 = new BlockSplitter(ReferenceNames.NAME_BLOCK_SPLITTER,                             4.0f, 10f, 1, Material.ROCK);
    public static final BlockAutoverse TRASH                    = new BlockTrash(ReferenceNames.NAME_BLOCK_TRASH,                                   4.0f, 10f, 1, Material.ROCK);

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        IForgeRegistry<Block> registry = event.getRegistry();

        registerBlock(registry, BARREL,                 Configs.disableBlockBarrel);
        registerBlock(registry, BLOCK_BREAKER,          Configs.disableBlockBlockBreaker);
        registerBlock(registry, BLOCK_DETECTOR,         Configs.disableBlockBlockDetector);
        registerBlock(registry, BLOCK_PLACER,           Configs.disableBlockBlockPlacer);
        registerBlock(registry, BLOCK_READER,           Configs.disableBlockBlockReader);
        registerBlock(registry, BUFFER,                 Configs.disableBlockBuffer);
        registerBlock(registry, CRAFTER,                Configs.disableBlockCrafter);
        registerBlock(registry, FILTER,                 Configs.disableBlockFilter);
        registerBlock(registry, INVENTORY_READER,       Configs.disableBlockInventoryReader);
        registerBlock(registry, MACHINE_FRAME,          Configs.disableBlockMachineFrame);
        registerBlock(registry, MUXER,                  Configs.disableBlockMuxer);
        registerBlock(registry, PIPE,                   Configs.disableBlockPipe);
        registerBlock(registry, REDSTONE_EMITTER,       Configs.disableBlockRedstoneEmitter);
        registerBlock(registry, SENSOR,                 Configs.disableBlockSensor);
        registerBlock(registry, SEQUENCE_DETECTOR,      Configs.disableBlockSequenceDetector);
        registerBlock(registry, SEQUENCER,              Configs.disableBlockSequencer);
        registerBlock(registry, SPLITTER,               Configs.disableBlockSplitter);
        registerBlock(registry, TRASH,                  Configs.disableBlockTrash);

        registerTileEntities();
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event)
    {
        IForgeRegistry<Item> registry = event.getRegistry();

        registerItemBlock(registry, BARREL,                 Configs.disableBlockBarrel);
        registerItemBlock(registry, BLOCK_BREAKER,          Configs.disableBlockBlockBreaker);
        registerItemBlock(registry, BLOCK_DETECTOR,         Configs.disableBlockBlockDetector);
        registerItemBlock(registry, BLOCK_PLACER,           Configs.disableBlockBlockPlacer);
        registerItemBlock(registry, BLOCK_READER,           Configs.disableBlockBlockReader);
        registerItemBlock(registry, BUFFER,                 Configs.disableBlockBuffer);
        registerItemBlock(registry, CRAFTER,                Configs.disableBlockCrafter);
        registerItemBlock(registry, FILTER,                 Configs.disableBlockFilter);
        registerItemBlock(registry, INVENTORY_READER,       Configs.disableBlockInventoryReader);
        registerItemBlock(registry, MACHINE_FRAME,          Configs.disableBlockMachineFrame);
        registerItemBlock(registry, MUXER,                  Configs.disableBlockMuxer);
        registerItemBlock(registry, PIPE,                   Configs.disableBlockPipe);
        registerItemBlock(registry, REDSTONE_EMITTER,       Configs.disableBlockRedstoneEmitter);
        registerItemBlock(registry, SENSOR,                 Configs.disableBlockSensor);
        registerItemBlock(registry, SEQUENCE_DETECTOR,      Configs.disableBlockSequenceDetector);
        registerItemBlock(registry, SEQUENCER,              Configs.disableBlockSequencer);
        registerItemBlock(registry, SPLITTER,               Configs.disableBlockSplitter);
        registerItemBlock(registry, TRASH,                  Configs.disableBlockTrash);
    }

    private static void registerBlock(IForgeRegistry<Block> registry, BlockAutoverse block, boolean isDisabled)
    {
        if (isDisabled == false)
        {
            block.setRegistryName(Reference.MOD_ID + ":" + block.getBlockName());
            registry.register(block);
        }
        else
        {
            block.setEnabled(false);
        }
    }

    private static void registerItemBlock(IForgeRegistry<Item> registry, BlockAutoverse block, boolean isDisabled)
    {
        registerItemBlock(registry, block, isDisabled, true);
    }

    private static void registerItemBlock(IForgeRegistry<Item> registry, BlockAutoverse block, boolean isDisabled, boolean hasSubtypes)
    {
        if (isDisabled == false)
        {
            Item item = block.createItemBlock().setRegistryName(Reference.MOD_ID, block.getBlockName()).setHasSubtypes(hasSubtypes);
            registry.register(item);
        }
    }

    private static void registerTileEntities()
    {
        registerTileEntity(TileEntityBarrel.class,                  ReferenceNames.NAME_BLOCK_BARREL);
        registerTileEntity(TileEntityBlockBreaker.class,            ReferenceNames.NAME_BLOCK_BREAKER);
        registerTileEntity(TileEntityBlockDetector.class,           ReferenceNames.NAME_BLOCK_DETECTOR);
        registerTileEntity(TileEntityBlockPlacer.class,             ReferenceNames.NAME_BLOCK_PLACER);
        registerTileEntity(TileEntityBlockPlacerProgrammable.class, ReferenceNames.NAME_TILE_ENTITY_PLACER_PROGRAMMABLE);
        registerTileEntity(TileEntityBlockReaderNBT.class,          ReferenceNames.NAME_TILE_ENTITY_BLOCK_READER_NBT);
        registerTileEntity(TileEntityBufferFifo.class,              ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);
        registerTileEntity(TileEntityBufferFifoAuto.class,          ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_AUTO);
        registerTileEntity(TileEntityBufferFifoPulsed.class,        ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_PULSED);
        registerTileEntity(TileEntityCrafter.class,                 ReferenceNames.NAME_BLOCK_CRAFTER);
        registerTileEntity(TileEntityFilter.class,                  ReferenceNames.NAME_BLOCK_FILTER);
        registerTileEntity(TileEntityFilterSequential.class,        ReferenceNames.NAME_BLOCK_FILTER_SEQUENTIAL);
        registerTileEntity(TileEntityInventoryReader.class,         ReferenceNames.NAME_BLOCK_INVENTORY_READER);
        registerTileEntity(TileEntityMuxer.class,                   ReferenceNames.NAME_BLOCK_MUXER);
        registerTileEntity(TileEntityPipe.class,                    ReferenceNames.NAME_BLOCK_PIPE);
        registerTileEntity(TileEntityPipeDirectional.class,         ReferenceNames.NAME_TILE_ENTITY_PIPE_DIRECTIONAL);
        registerTileEntity(TileEntityPipeExtraction.class,          ReferenceNames.NAME_TILE_ENTITY_PIPE_EXTRACTION);
        registerTileEntity(TileEntityPipeRoundRobin.class,          ReferenceNames.NAME_TILE_ENTITY_PIPE_ROUNDROBIN);
        registerTileEntity(TileEntityRedstoneEmitter.class,         ReferenceNames.NAME_BLOCK_REDSTONE_EMITTER);
        registerTileEntity(TileEntityRedstoneEmitterAdvanced.class, ReferenceNames.NAME_TILE_ENTITY_REDSTONE_EMITTER_ADVANCED);
        registerTileEntity(TileEntitySequenceDetector.class,        ReferenceNames.NAME_BLOCK_SEQUENCE_DETECTOR);
        registerTileEntity(TileEntitySequencer.class,               ReferenceNames.NAME_BLOCK_SEQUENCER);
        registerTileEntity(TileEntitySequencerProgrammable.class,   ReferenceNames.NAME_BLOCK_SEQUENCER_PROGRAMMABLE);
        registerTileEntity(TileEntitySplitter.class,                ReferenceNames.NAME_BLOCK_SPLITTER);
        registerTileEntity(TileEntityTrashBin.class,                ReferenceNames.NAME_TILE_ENTITY_TRASH_BIN);
        registerTileEntity(TileEntityTrashBuffer.class,             ReferenceNames.NAME_TILE_ENTITY_TRASH_BUFFER);
    }

    private static void registerTileEntity(Class<? extends TileEntity> clazz, String id)
    {
        GameRegistry.registerTileEntity(clazz, Reference.MOD_ID + ":" + id);
    }
}
