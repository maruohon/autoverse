package fi.dy.masa.autoverse.network.message;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.util.PlacementProperties;
import io.netty.buffer.ByteBuf;

public class MessageSyncNBTTag implements IMessage
{
    private Type type;
    private NBTTagCompound tag;

    public MessageSyncNBTTag()
    {
    }

    public MessageSyncNBTTag(Type type, NBTTagCompound tag)
    {
        this.type = type;
        this.tag = tag;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        try
        {
            this.type = Type.fromOrdinal(buf.readByte());
            this.tag = AutoverseByteBufUtils.readNBTTagCompoundFromBuffer(buf);
        }
        catch (IOException e)
        {
            Autoverse.logger.warn("MessageSyncNBTTag: Exception while reading data from buffer", e);
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeByte((byte) this.type.ordinal());
        AutoverseByteBufUtils.writeNBTTagCompoundToBuffer(buf, this.tag);
    }

    public static class Handler implements IMessageHandler<MessageSyncNBTTag, IMessage>
    {
        @Override
        public IMessage onMessage(final MessageSyncNBTTag message, MessageContext ctx)
        {
            if (ctx.side != Side.CLIENT)
            {
                Autoverse.logger.error("Wrong side in MessageSyncNBTTag: " + ctx.side);
                return null;
            }

            final Minecraft mc = FMLClientHandler.instance().getClient();
            final EntityPlayer player = Autoverse.proxy.getPlayerFromMessageContext(ctx);

            if (mc == null)
            {
                Autoverse.logger.error("Minecraft was null in MessageSyncNBTTag");
                return null;
            }

            if (player == null)
            {
                Autoverse.logger.error("Player was null in MessageSyncNBTTag");
                return null;
            }

            mc.addScheduledTask(new Runnable()
            {
                public void run()
                {
                    processMessage(message, player);
                }
            });

            return null;
        }

        protected void processMessage(final MessageSyncNBTTag message, final EntityPlayer player)
        {
            if (message.type == Type.PLACEMENT_PROPERTIES_FULL)
            {
                PlacementProperties.getInstance().readAllDataForPlayerFromNBT(message.tag);
            }
            else if (message.type == Type.PLACEMENT_PROPERTIES_CURRENT)
            {
                PlacementProperties.getInstance().readSyncedItemData(player, message.tag);
            }
        }
    }

    public enum Type
    {
        PLACEMENT_PROPERTIES_FULL,
        PLACEMENT_PROPERTIES_CURRENT;

        public static Type fromOrdinal(int type)
        {
            return values()[type % values().length];
        }
    }
}
