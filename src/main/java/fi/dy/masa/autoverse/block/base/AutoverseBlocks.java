package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.common.registry.GameRegistry;
import fi.dy.masa.autoverse.block.BlockBarrel;
import fi.dy.masa.autoverse.block.BlockBuffer;
import fi.dy.masa.autoverse.block.BlockFilter;
import fi.dy.masa.autoverse.block.BlockFilterSequential;
import fi.dy.masa.autoverse.block.BlockSequencer;
import fi.dy.masa.autoverse.item.ItemBlockAutoverse;
import fi.dy.masa.autoverse.item.ItemBlockBarrel;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequentialSmart;

public class AutoverseBlocks
{
    public static final BlockAutoverse blockBarrel          = new BlockBarrel(ReferenceNames.NAME_BLOCK_BARREL, 4.0f, 1, Material.IRON);
    public static final BlockAutoverse blockBuffer          = new BlockBuffer(ReferenceNames.NAME_BLOCK_BUFFER, 4.0f, 1, Material.IRON);
    public static final BlockAutoverse blockFilter          = new BlockFilter(ReferenceNames.NAME_BLOCK_FILTER, 4.0f, 1, Material.IRON, TileEntityFilter.class);
    public static final BlockAutoverse blockFilterSeq       = new BlockFilterSequential(ReferenceNames.NAME_BLOCK_FILTER_SEQUENTIAL, 4.0f, 1, Material.IRON, TileEntityFilterSequential.class);
    public static final BlockAutoverse blockFilterSeqSmart  = new BlockFilterSequential(ReferenceNames.NAME_BLOCK_FILTER_SEQ_SMART, 4.0f, 1, Material.IRON, TileEntityFilterSequentialSmart.class);
    public static final BlockAutoverse blockSequencer       = new BlockSequencer(ReferenceNames.NAME_BLOCK_SEQUENCER, 4.0f, 1, Material.IRON);

    public static void init()
    {
        registerBlock(blockBarrel,          ReferenceNames.NAME_BLOCK_BARREL,               false, false);
        registerBlock(blockBuffer,          ReferenceNames.NAME_BLOCK_BUFFER,               false);
        registerBlock(blockFilter,          ReferenceNames.NAME_BLOCK_FILTER,               false);
        registerBlock(blockFilterSeq,       ReferenceNames.NAME_BLOCK_FILTER_SEQUENTIAL,    false);
        registerBlock(blockFilterSeqSmart,  ReferenceNames.NAME_BLOCK_FILTER_SEQ_SMART,     false);
        registerBlock(blockSequencer,       ReferenceNames.NAME_BLOCK_SEQUENCER,            false);

        GameRegistry.register(new ItemBlockBarrel(blockBarrel).setRegistryName(blockBarrel.getRegistryName()));
    }

    private static void registerBlock(Block block, String registryName, boolean isDisabled)
    {
        registerBlock(block, registryName, isDisabled, true);
    }

    private static void registerBlock(Block block, String registryName, boolean isDisabled, boolean registerItem)
    {
        if (isDisabled == false)
        {
            block.setRegistryName(registryName);
            GameRegistry.register(block);

            if (registerItem)
            {
                GameRegistry.register(new ItemBlockAutoverse(block).setRegistryName(block.getRegistryName()));
            }
        }
    }
}
