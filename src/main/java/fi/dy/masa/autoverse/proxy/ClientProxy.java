package fi.dy.masa.autoverse.proxy;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.block.BlockSensor;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.client.HotKeys;
import fi.dy.masa.autoverse.client.render.model.BakedModelMachineSlim;
import fi.dy.masa.autoverse.client.render.model.BakedModelPipe;
import fi.dy.masa.autoverse.client.render.model.BakedModelRedstoneEmitter;
import fi.dy.masa.autoverse.client.renderer.tile.TESRPipe;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.event.InputEventHandler;
import fi.dy.masa.autoverse.event.RenderEventHandler;
import fi.dy.masa.autoverse.item.base.AutoverseItems;
import fi.dy.masa.autoverse.item.base.ItemAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityPipe;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    @Override
    public EntityPlayer getClientPlayer()
    {
        return FMLClientHandler.instance().getClientPlayerEntity();
    }

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
        super.registerEventHandlers();

        MinecraftForge.EVENT_BUS.register(new Configs());
        MinecraftForge.EVENT_BUS.register(new InputEventHandler());
        MinecraftForge.EVENT_BUS.register(new RenderEventHandler());

        HotKeys.keyToggleMode = new KeyBinding(HotKeys.KEYBIND_NAME_TOGGLE_MODE,
                HotKeys.DEFAULT_KEYBIND_TOGGLE_MODE,
                HotKeys.KEYBIND_CATEGORY_AUTOVERSE);

        ClientRegistry.registerKeyBinding(HotKeys.keyToggleMode);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        registerBlockModels();
        registerItemBlockModels();
        registerItemModels();

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPipe.class, new TESRPipe());
    }

    private static void registerBlockModels()
    {
        ModelLoaderRegistry.registerLoader(new BakedModelMachineSlim.ModelLoaderMachineSlim());

        ModelLoader.setCustomStateMapper(AutoverseBlocks.PIPE, new BakedModelPipe.StateMapper());
        ModelLoaderRegistry.registerLoader(new BakedModelPipe.ModelLoaderPipe());

        ModelLoader.setCustomStateMapper(AutoverseBlocks.REDSTONE_EMITTER, new BakedModelRedstoneEmitter.StateMapper());
        ModelLoaderRegistry.registerLoader(new BakedModelRedstoneEmitter.ModelLoaderRedstoneEmitter());

        ModelLoader.setCustomStateMapper(AutoverseBlocks.SENSOR, (new StateMap.Builder()).ignore(BlockSensor.POWER).build());
    }

    private static void registerItemBlockModels()
    {
        registerItemBlockModel(AutoverseBlocks.BARREL, 0, "pulsed=false,tier=0");
        registerItemBlockModel(AutoverseBlocks.BARREL, 1, "pulsed=true,tier=0");
        registerItemBlockModel(AutoverseBlocks.BLOCK_BREAKER, 0, "facing=north,slim=false,type=normal");
        registerItemBlockModel(AutoverseBlocks.BLOCK_BREAKER, 1, "facing=north,slim=false,type=greedy");
        registerItemBlockModel(AutoverseBlocks.BLOCK_DETECTOR, 0, "facing=north,facing_out=east,slim=false");
        registerItemBlockModel(AutoverseBlocks.BLOCK_PLACER, 0, "facing=north,slim=false,type=nbt");
        registerItemBlockModel(AutoverseBlocks.BLOCK_PLACER, 1, "facing=north,slim=false,type=programmable");
        registerItemBlockModel(AutoverseBlocks.BLOCK_READER, 0, "facing=north,slim=false,type=nbt");
        registerItemBlockModel(AutoverseBlocks.BUFFER, 0, "facing=north,slim=false,type=fifo_normal");
        registerItemBlockModel(AutoverseBlocks.BUFFER, 1, "facing=north,slim=false,type=fifo_pulsed");
        registerItemBlockModel(AutoverseBlocks.BUFFER, 2, "facing=north,slim=false,type=fifo_auto");
        registerItemBlockModel(AutoverseBlocks.CRAFTER, 0, "facing=north,slim=false");
        registerItemBlockModel(AutoverseBlocks.FILTER, 0, "facing=north,facing_filter=east,slim=false,type=basic");
        registerItemBlockModel(AutoverseBlocks.FILTER, 1, "facing=north,facing_filter=east,slim=false,type=sequential");
        registerItemBlockModel(AutoverseBlocks.FILTER, 2, "facing=north,facing_filter=east,slim=false,type=sequential_strict");
        registerItemBlockModel(AutoverseBlocks.INVENTORY_READER, 0, "facing=north,powered=false,type=items");
        registerItemBlockModel(AutoverseBlocks.INVENTORY_READER, 1, "facing=north,powered=false,type=slots");
        registerItemBlockModel(AutoverseBlocks.MACHINE_FRAME, 0, "inventory");
        registerItemBlockModel(AutoverseBlocks.MUXER, 0, "facing=north,facing_in2=east,slim=false,type=redstone");
        registerItemBlockModel(AutoverseBlocks.MUXER, 1, "facing=north,facing_in2=east,slim=false,type=priority");
        registerItemBlockModel(AutoverseBlocks.MUXER, 2, "facing=north,facing_in2=east,slim=false,type=programmable");
        registerItemBlockModel(AutoverseBlocks.PIPE, 0, "type=basic");
        registerItemBlockModel(AutoverseBlocks.PIPE, 1, "type=extraction");
        registerItemBlockModel(AutoverseBlocks.PIPE, 2, "type=directional");
        registerItemBlockModel(AutoverseBlocks.PIPE, 3, "type=roundrobin");
        registerItemBlockModel(AutoverseBlocks.REDSTONE_EMITTER, 0, "type=basic");
        registerItemBlockModel(AutoverseBlocks.REDSTONE_EMITTER, 1, "type=advanced");
        registerItemBlockModel(AutoverseBlocks.SENSOR, 0, "inventory");
        registerItemBlockModel(AutoverseBlocks.SEQUENCE_DETECTOR, 0, "facing=north,powered=false,slim=false");
        registerItemBlockModel(AutoverseBlocks.SEQUENCER, 0, "facing=north,slim=false,type=basic");
        registerItemBlockModel(AutoverseBlocks.SEQUENCER, 1, "facing=north,slim=false,type=programmable");
        registerItemBlockModel(AutoverseBlocks.SPLITTER, 0, "facing=north,facing_out2=east,slim=false,type=switchable");
        registerItemBlockModel(AutoverseBlocks.SPLITTER, 1, "facing=north,facing_out2=east,slim=false,type=redstone");
        registerItemBlockModel(AutoverseBlocks.SPLITTER, 2, "facing=north,facing_out2=east,slim=false,type=length");
        registerItemBlockModel(AutoverseBlocks.TRASH, 0, "type=bin");
        registerItemBlockModel(AutoverseBlocks.TRASH, 1, "type=buffer");
    }

    private static void registerItemModels()
    {
        registerItemModel(AutoverseItems.WAND, 0, "inventory");
    }

    private static void registerItemBlockModel(BlockAutoverse block, int meta, String fullVariant)
    {
        if (block.isEnabled())
        {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), meta,
                new ModelResourceLocation(block.getRegistryName(), fullVariant));
        }
    }

    private static void registerItemModel(ItemAutoverse item, int meta, String fullVariant)
    {
        if (item.isEnabled())
        {
            ModelLoader.setCustomModelResourceLocation(item, meta,
                    new ModelResourceLocation(item.getRegistryName(), fullVariant));
        }
    }
}
