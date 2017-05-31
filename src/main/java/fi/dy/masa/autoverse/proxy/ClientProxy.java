package fi.dy.masa.autoverse.proxy;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.client.HotKeys;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.reference.Reference;

public class ClientProxy extends CommonProxy
{
    private ModFixs dataFixer = null;

    @Override
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case CLIENT:
                return FMLClientHandler.instance().getClientPlayerEntity();

            case SERVER:
                return ctx.getServerHandler().player;

            default:
                Autoverse.logger.warn("Invalid side in getPlayerFromMessageContext(): " + ctx.side);
                return null;
        }
    }

    @Override
    public ModFixs getDataFixer()
    {
        // On a server, the DataFixer gets created for and is stored inside MinecraftServer,
        // but in single player the DataFixer is stored in the client Minecraft class
        // over world reloads.
        if (this.dataFixer == null)
        {
            this.dataFixer = FMLCommonHandler.instance().getDataFixer().init(Reference.MOD_ID, Autoverse.DATA_FIXER_VERSION);
        }

        return this.dataFixer;
    }

    @Override
    public boolean isShiftKeyDown()
    {
        return GuiScreen.isShiftKeyDown();
    }

    @Override
    public boolean isControlKeyDown()
    {
        return GuiScreen.isCtrlKeyDown();
    }

    @Override
    public boolean isAltKeyDown()
    {
        return GuiScreen.isAltKeyDown();
    }

    @Override
    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new Configs());
    }

    @Override
    public void registerKeyBindings()
    {
        HotKeys.keyToggleMode = new KeyBinding(HotKeys.KEYBIND_NAME_TOGGLE_MODE,
                                               HotKeys.DEFAULT_KEYBIND_TOGGLE_MODE,
                                               HotKeys.KEYBIND_CATEGORY_ENDERUTILITIES);

        ClientRegistry.registerKeyBinding(HotKeys.keyToggleMode);
    }

    @Override
    public void registerModels()
    {
        this.registerItemBlockModels();
        this.registerAllItemModels();
    }

    public void registerAllItemModels()
    {
    }

    private void registerItemBlockModels()
    {
        this.registerBarrelItemBlockModels(AutoverseBlocks.BARREL);
        this.registerAllItemBlockModels(AutoverseBlocks.FIFO_BUFFER, "facing=north,type=", "", false);
        this.registerItemBlockModel(AutoverseBlocks.CRAFTER, 0, "facing=north");
        this.registerAllItemBlockModels(AutoverseBlocks.FILTER_BASIC, "facing=north,facing_filter=east,tier=", "", true);
        this.registerAllItemBlockModels(AutoverseBlocks.FILTER_SEQUENTIAL_SMART, "facing=north,facing_filter=east,tier=", "", true);
        this.registerAllItemBlockModels(AutoverseBlocks.FILTER_SEQUENTIAL, "facing=north,facing_filter=east,tier=", "", true);
        this.registerAllItemBlockModels(AutoverseBlocks.SEQUENCER, "facing=north,tier=", "", true);
        this.registerItemBlockModel(AutoverseBlocks.SPLITTER, 0, "facing=north,facing_out2=east,type=togglable");
        this.registerItemBlockModel(AutoverseBlocks.SPLITTER, 1, "facing=north,facing_out2=east,type=selectable");
        this.registerItemBlockModel(AutoverseBlocks.SPLITTER, 2, "facing=north,facing_out2=east,type=redstone");
    }

    private void registerItemBlockModel(BlockAutoverse block, int meta, String fullVariant)
    {
        if (block.isEnabled())
        {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), meta,
                new ModelResourceLocation(block.getRegistryName(), fullVariant));
        }
    }

    private void registerAllItemBlockModels(BlockAutoverse block, String variantPre, String variantPost, boolean useMeta)
    {
        if (block.isEnabled())
        {
            NonNullList<ItemStack> stacks = NonNullList.create();
            block.getSubBlocks(Item.getItemFromBlock(block), block.getCreativeTabToDisplayOn(), stacks);
            String[] names = block.getUnlocalizedNames();

            for (ItemStack stack : stacks)
            {
                Item item = stack.getItem();
                int meta = stack.getMetadata();
                String variant = useMeta ? String.valueOf(meta) : names[meta];
                ModelResourceLocation mrl = new ModelResourceLocation(item.getRegistryName(), variantPre + variant + variantPost);
                ModelLoader.setCustomModelResourceLocation(item, meta, mrl);
            }
        }
    }

    private void registerBarrelItemBlockModels(BlockAutoverse blockIn)
    {
        NonNullList<ItemStack> stacks = NonNullList.create();
        blockIn.getSubBlocks(Item.getItemFromBlock(blockIn), blockIn.getCreativeTabToDisplayOn(), stacks);

        for (ItemStack stack : stacks)
        {
            Item item = stack.getItem();
            int meta = stack.getMetadata();
            int tier = meta & 0xF;
            boolean pulsed = meta >= 16;
            ModelResourceLocation mrl = new ModelResourceLocation(item.getRegistryName(), "pulsed=" + pulsed + ",tier=" + tier);
            ModelLoader.setCustomModelResourceLocation(item, meta, mrl);
        }
    }
}
