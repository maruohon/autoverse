package fi.dy.masa.autoverse.proxy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.config.Configs;

public class ClientProxy extends CommonProxy
{

    @Override
    public boolean isShiftKeyDown()
    {
        return GuiScreen.isShiftKeyDown();
    }

    @Override
    public void registerModels()
    {
        this.registerItemBlockModels();
        this.registerAllItemModels();
    }

    @Override
    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new Configs());
    }

    public void registerAllItemModels()
    {
    }

    private void registerItemBlockModels()
    {
        this.registerAllItemBlockModels(AutoverseBlocks.blockBuffer, "facing=north,type=", "");
    }

    /*private void registerItemBlockModel(BlockAutoverse blockIn, int meta, String fullVariant)
    {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockIn), meta,
                new ModelResourceLocation(blockIn.getRegistryName(), fullVariant));
    }*/

    private void registerAllItemBlockModels(BlockAutoverse blockIn, String variantPre, String variantPost)
    {
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        blockIn.getSubBlocks(Item.getItemFromBlock(blockIn), blockIn.getCreativeTabToDisplayOn(), stacks);
        String[] names = blockIn.getUnlocalizedNames();

        for (ItemStack stack : stacks)
        {
            Item item = stack.getItem();
            int meta = stack.getMetadata();
            ModelResourceLocation mrl = new ModelResourceLocation(item.getRegistryName(), variantPre + names[meta] + variantPost);
            ModelLoader.setCustomModelResourceLocation(item, meta, mrl);
        }
    }
}
