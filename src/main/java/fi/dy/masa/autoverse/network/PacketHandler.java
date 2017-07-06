package fi.dy.masa.autoverse.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.network.message.MessageAddEffects;
import fi.dy.masa.autoverse.network.message.MessageGuiAction;
import fi.dy.masa.autoverse.network.message.MessageKeyPressed;
import fi.dy.masa.autoverse.network.message.MessageSyncContainerProperty;
import fi.dy.masa.autoverse.network.message.MessageSyncNBTTag;
import fi.dy.masa.autoverse.network.message.MessageSyncSlot;
import fi.dy.masa.autoverse.reference.Reference;

public class PacketHandler
{
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID.toLowerCase());

    public static void init()
    {
        INSTANCE.registerMessage(MessageGuiAction.Handler.class,                MessageGuiAction.class,                 0, Side.SERVER);
        INSTANCE.registerMessage(MessageKeyPressed.Handler.class,               MessageKeyPressed.class,                1, Side.SERVER);

        INSTANCE.registerMessage(MessageSyncSlot.Handler.class,                 MessageSyncSlot.class,                 11, Side.CLIENT);
        INSTANCE.registerMessage(MessageSyncContainerProperty.Handler.class,    MessageSyncContainerProperty.class,    12, Side.CLIENT);
        INSTANCE.registerMessage(MessageSyncNBTTag.Handler.class,               MessageSyncNBTTag.class,               13, Side.CLIENT);
        INSTANCE.registerMessage(MessageAddEffects.Handler.class,               MessageAddEffects.class,               14, Side.CLIENT);
    }
}
