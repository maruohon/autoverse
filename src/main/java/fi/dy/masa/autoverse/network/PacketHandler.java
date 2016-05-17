package fi.dy.masa.autoverse.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.network.message.MessageSyncSlot;
import fi.dy.masa.autoverse.network.message.MessageSyncSpecialSlot;
import fi.dy.masa.autoverse.reference.Reference;

public class PacketHandler
{
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID.toLowerCase());

    public static void init()
    {
        INSTANCE.registerMessage(MessageSyncSlot.Handler.class,             MessageSyncSlot.class,          0, Side.CLIENT);
        INSTANCE.registerMessage(MessageSyncSpecialSlot.Handler.class,      MessageSyncSpecialSlot.class,   1, Side.CLIENT);
    }
}
