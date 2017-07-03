package fi.dy.masa.autoverse.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.effects.Effects;
import io.netty.buffer.ByteBuf;

public class MessageAddEffects implements IMessage
{
    public static final int SOUND = 1;
    public static final int PARTICLES = 2;

    public static final int EFFECT_PARTICLES = 100;

    private int effectType;
    private int flags;
    private float x;
    private float y;
    private float z;
    private int particleCount;
    private double offset;
    private double velocity;

    public MessageAddEffects()
    {
    }

    public MessageAddEffects(int id, int flags, double x, double y, double z)
    {
        this(id, flags, x, y, z, 32, 0.2f, 2.0f);
    }

    public MessageAddEffects(int id, int flags, double x, double y, double z, int particleCount)
    {
        this(id, flags, x, y, z, particleCount, 0.2f, 2.0f);
    }

    public MessageAddEffects(int id, int flags, double x, double y, double z, int particleCount, float offset, float velocity)
    {
        this.effectType = id;
        this.flags = flags;
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.particleCount = particleCount;
        this.offset = offset;
        this.velocity = velocity;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeByte(this.effectType);
        buf.writeByte(this.flags);
        buf.writeFloat((float) this.x);
        buf.writeFloat((float) this.y);
        buf.writeFloat((float) this.z);

        buf.writeShort(this.particleCount);
        buf.writeFloat((float) this.offset);
        buf.writeFloat((float) this.velocity);
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.effectType = buf.readByte();
        this.flags = buf.readByte();
        this.x = buf.readFloat();
        this.y = buf.readFloat();
        this.z = buf.readFloat();

        this.particleCount = buf.readShort();
        this.offset = buf.readFloat();
        this.velocity = buf.readFloat();
    }

    public static class Handler implements IMessageHandler<MessageAddEffects, IMessage>
    {
        @Override
        public IMessage onMessage(final MessageAddEffects message, MessageContext ctx)
        {
            if (ctx.side != Side.CLIENT)
            {
                Autoverse.logger.error("Wrong side in MessageAddEffects: " + ctx.side);
                return null;
            }

            Minecraft mc = FMLClientHandler.instance().getClient();
            final EntityPlayer player = Autoverse.proxy.getPlayerFromMessageContext(ctx);

            if (mc == null || player == null)
            {
                Autoverse.logger.error("Minecraft or player was null in MessageAddEffects");
                return null;
            }

            mc.addScheduledTask(new Runnable()
            {
                public void run()
                {
                    processMessage(message, player, player.getEntityWorld(), mc.getSoundHandler());
                }
            });

            return null;
        }

        protected void processMessage(final MessageAddEffects message, EntityPlayer player, World world, SoundHandler soundHandler)
        {
            if (message.effectType == EFFECT_PARTICLES)
            {
                Effects.spawnParticles(world, EnumParticleTypes.getParticleFromId(message.flags),
                        message.x, message.y, message.z, message.particleCount, message.offset, message.velocity);
            }
        }
    }
}
