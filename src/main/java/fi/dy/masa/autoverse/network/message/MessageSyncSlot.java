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
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.inventory.container.IBaseInventory;
import io.netty.buffer.ByteBuf;

public class MessageSyncSlot implements IMessage
{
    private int windowId;
    private int slotNum;
    private ItemStack stack;

    public MessageSyncSlot()
    {
    }

    public MessageSyncSlot(int windowId, int slotNum, ItemStack stack)
    {
        this.windowId = windowId;
        this.slotNum = slotNum;
        this.stack = stack;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        try
        {
            this.windowId = buf.readByte();
            this.slotNum = buf.readShort();
            this.stack = AutoverseByteBufUtils.readItemStackFromBuffer(buf);
        }
        catch (IOException e)
        {
            Autoverse.logger.warn("MessageSyncSlot: Exception while reading data from buffer");
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeByte(this.windowId);
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
                Autoverse.logger.error("Wrong side in MessageSyncCustomSlot: " + ctx.side);
                return null;
            }

            Minecraft mc = FMLClientHandler.instance().getClient();
            final EntityPlayer player = Autoverse.proxy.getPlayerFromMessageContext(ctx);
            if (mc == null || player == null)
            {
                Autoverse.logger.error("Minecraft or player was null in MessageSyncCustomSlot");
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
            if (player.openContainer instanceof IBaseInventory && message.windowId == player.openContainer.windowId)
            {
                IBaseInventory container = (IBaseInventory)player.openContainer;
                IItemHandlerModifiable inv = container.getBaseInventory();
                //System.out.printf("message slot: %d stack: %s\n", message.slotNum, message.stack);
                inv.setStackInSlot(message.slotNum, message.stack);
            }
        }
    }
}
