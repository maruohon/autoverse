package fi.dy.masa.autoverse.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import io.netty.buffer.ByteBuf;

public class MessageSyncContainerProperty implements IMessage
{
    private Type type;
    private int windowId;
    private int valueId;
    private long value;

    public MessageSyncContainerProperty()
    {
    }

    public MessageSyncContainerProperty(Type type, int windowId, int id, long value)
    {
        this.type = type;
        this.windowId = windowId;
        this.valueId = id;
        this.value = value;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeByte(this.windowId);
        buf.writeShort(this.valueId);
        buf.writeByte((byte) this.type.getId());

        switch (this.type)
        {
            case BYTE:
                buf.writeByte((byte) this.value);
                break;

            case SHORT:
                buf.writeShort((short) this.value);
                break;

            case INT:
                buf.writeInt((int) this.value);
                break;

            case LONG:
                buf.writeLong(this.value);
                break;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.windowId = buf.readByte();
        this.valueId = buf.readShort();
        this.type = Type.fromId(buf.readByte());

        switch (this.type)
        {
            case BYTE:
                this.value = buf.readByte();
                break;

            case SHORT:
                this.value = buf.readShort();
                break;

            case INT:
                this.value = buf.readInt();
                break;

            case LONG:
                this.value = buf.readLong();
                break;
        }
    }

    public static class Handler implements IMessageHandler<MessageSyncContainerProperty, IMessage>
    {
        @Override
        public IMessage onMessage(final MessageSyncContainerProperty message, MessageContext ctx)
        {
            if (ctx.side != Side.CLIENT)
            {
                Autoverse.logger.error("Wrong side in MessageSyncContainerProperty: " + ctx.side);
                return null;
            }

            Minecraft mc = FMLClientHandler.instance().getClient();
            final EntityPlayer player = Autoverse.proxy.getPlayerFromMessageContext(ctx);

            if (mc == null || player == null)
            {
                Autoverse.logger.error("Minecraft or player was null in MessageSyncContainerProperty");
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

        protected void processMessage(final MessageSyncContainerProperty message, EntityPlayer player)
        {
            if (player.openContainer instanceof ContainerAutoverse && message.windowId == player.openContainer.windowId)
            {
                ContainerAutoverse container = (ContainerAutoverse) player.openContainer;

                if (message.type == Type.LONG)
                {
                    container.receivePropertyLong(message.valueId, message.value);
                }
                else
                {
                    container.receiveProperty(message.valueId, (int) message.value);
                }
            }
        }
    }

    public enum Type
    {
        BYTE    (1),
        SHORT   (2),
        INT     (4),
        LONG    (8);

        private final int id;

        private Type(int id)
        {
            this.id = id;
        }

        public int getId()
        {
            return this.id;
        }

        public static Type fromId(int id)
        {
            switch (id)
            {
                case 1: return BYTE;
                case 2: return SHORT;
                case 4: return INT;
                case 8: return LONG;
                default: return BYTE;
            }
        }
    }
}
