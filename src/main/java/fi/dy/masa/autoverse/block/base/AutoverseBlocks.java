package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.common.registry.GameRegistry;
import fi.dy.masa.autoverse.block.BlockBarrel;
import fi.dy.masa.autoverse.block.BlockBuffer;
import fi.dy.masa.autoverse.block.BlockFilter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequentialSmart;

public class AutoverseBlocks
{
    public static final BlockAutoverse blockBarrel = new BlockBarrel(ReferenceNames.NAME_BLOCK_BARREL, 4.0f, 1, Material.ROCK);
    public static final BlockAutoverse blockBuffer = new BlockBuffer(ReferenceNames.NAME_BLOCK_BUFFER, 4.0f, 1, Material.ROCK);
    public static final BlockAutoverse blockFilter = new BlockFilter(ReferenceNames.NAME_BLOCK_FILTER, 4.0f, 1, Material.ROCK, TileEntityFilter.class);
    public static final BlockAutoverse blockFilterSeq = new BlockFilter(ReferenceNames.NAME_BLOCK_FILTER_SEQUENTIAL, 4.0f, 1, Material.ROCK, TileEntityFilterSequential.class);
    public static final BlockAutoverse blockFilterSeqSmart = new BlockFilter(ReferenceNames.NAME_BLOCK_FILTER_SEQ_SMART, 4.0f, 1, Material.ROCK, TileEntityFilterSequentialSmart.class);

    public static void init()
    {
        registerBlock(blockBarrel, ReferenceNames.NAME_BLOCK_BARREL, false);
        registerBlock(blockBuffer, ReferenceNames.NAME_BLOCK_BUFFER, false);
        registerBlock(blockFilter, ReferenceNames.NAME_BLOCK_FILTER, false);
        registerBlock(blockFilterSeq, ReferenceNames.NAME_BLOCK_FILTER_SEQUENTIAL, false);
        registerBlock(blockFilterSeqSmart, ReferenceNames.NAME_BLOCK_FILTER_SEQ_SMART, false);
    }

    private static void registerBlock(Block block, String registryName, boolean isDisabled)
    {
        if (isDisabled == false)
        {
            block.setRegistryName(registryName);
            GameRegistry.register(block);
            GameRegistry.register(new ItemBlockAutoverse(block).setRegistryName(block.getRegistryName()));
        }
    }
}
