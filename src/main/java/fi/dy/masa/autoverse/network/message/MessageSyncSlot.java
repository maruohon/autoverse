package fi.dy.masa.autoverse.network.message;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.inventory.ICustomSlotSync;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import io.netty.buffer.ByteBuf;

public class MessageSyncSlot implements IMessage
{
    private int windowId;
    private int typeId;
    private int slotNum;
    private ItemStack stack = ItemStack.EMPTY;

    public MessageSyncSlot()
    {
    }

    public MessageSyncSlot(int windowId, int typeId, int slotNum, ItemStack stack)
    {
        this.windowId = windowId;
        this.typeId = typeId;
        this.slotNum = slotNum;
        this.stack = stack;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        try
        {
            this.windowId = buf.readByte();
            this.typeId = buf.readByte();
            this.slotNum = buf.readShort();
            this.stack = AutoverseByteBufUtils.readItemStackFromBuffer(buf);
        }
        catch (IOException e)
        {
            Autoverse.logger.warn("MessageSyncSlot: Exception while reading data from buffer", e);
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeByte(this.windowId);
        buf.writeByte(this.typeId);
        buf.writeShort(this.slotNum);
        AutoverseByteBufUtils.writeItemStackToBuffer(buf, this.stack);
    }

    public static class Handler implements IMessageHandler<MessageSyncSlot, IMessage>
    {
        @Override
        public IMessage onMessage(final MessageSyncSlot message, MessageContext ctx)
        {
            if (ctx.side != Side.CLIENT)
            {
                Autoverse.logger.error("Wrong side in MessageSyncSlot: " + ctx.side);
                return null;
            }

            Minecraft mc = FMLClientHandler.instance().getClient();
            final EntityPlayer player = Autoverse.proxy.getPlayerFromMessageContext(ctx);

            if (mc == null || player == null)
            {
                Autoverse.logger.error("Minecraft or player was null in MessageSyncSlot");
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

        protected void processMessage(final MessageSyncSlot message, EntityPlayer player)
        {
            if (message.windowId == player.openContainer.windowId && player.openContainer instanceof ContainerAutoverse)
            {
                //System.out.printf("MessageSyncSlot - slot: %3d stack: %s\n", message.slotNum, message.stack);
                if (message.typeId == ContainerAutoverse.SLOT_TYPE_NORMAL)
                {
                    ((ContainerAutoverse) player.openContainer).syncStackInSlot(message.slotNum, message.stack);
                }
                else if (player.openContainer instanceof ICustomSlotSync)
                {
                    ((ICustomSlotSync) player.openContainer).putCustomStack(message.typeId, message.slotNum, message.stack);
                }
            }
        }
    }
}
