package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.common.registry.GameRegistry;
import fi.dy.masa.autoverse.block.BlockBuffer;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class AutoverseBlocks
{
    public static final BlockAutoverse blockBuffer = new BlockBuffer(ReferenceNames.NAME_BLOCK_BUFFER, 4.0f, 1, Material.ROCK);

    public static void init()
    {
        registerBlock(blockBuffer, ReferenceNames.NAME_BLOCK_BUFFER, false);
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
