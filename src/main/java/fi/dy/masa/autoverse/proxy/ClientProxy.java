package fi.dy.masa.autoverse.proxy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.config.Configs;

public class ClientProxy extends CommonProxy
{
    @Override
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case CLIENT:
                return FMLClientHandler.instance().getClientPlayerEntity();
            case SERVER:
                return ctx.getServerHandler().playerEntity;
            default:
                Autoverse.logger.warn("Invalid side in getPlayerFromMessageContext(): " + ctx.side);
                return null;
        }
    }

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
        this.registerAllItemBlockModels(AutoverseBlocks.blockBarrel, "tier=", "");
        this.registerAllItemBlockModels(AutoverseBlocks.blockBuffer, "facing=north,type=", "");
        this.registerAllItemBlockModels(AutoverseBlocks.blockFilter, "facing=north,tier=", "");
        this.registerAllItemBlockModels(AutoverseBlocks.blockFilterSeqSmart, "facing=north,tier=", "");
        this.registerAllItemBlockModels(AutoverseBlocks.blockFilterSeq, "facing=north,tier=", "");
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
        String[] names = blockIn.getItemBlockVariantStrings();

        for (ItemStack stack : stacks)
        {
            Item item = stack.getItem();
            int meta = stack.getMetadata();
            String name = names != null ? names[meta] : String.valueOf(meta);
            ModelResourceLocation mrl = new ModelResourceLocation(item.getRegistryName(), variantPre + name + variantPost);
            ModelLoader.setCustomModelResourceLocation(item, meta, mrl);
        }
    }
}
