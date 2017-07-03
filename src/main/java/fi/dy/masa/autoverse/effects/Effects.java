package fi.dy.masa.autoverse.effects;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageAddEffects;

public class Effects
{
    public static void spawnParticles(World world, EnumParticleTypes type, double x, double y, double z, int count, double offset, double velocity)
    {
        // Spawn some particles
        for (int i = 0; i < count; i++)
        {
            double offX = (world.rand.nextFloat() - 0.5d) * offset;
            double offY = (world.rand.nextFloat() - 0.5d) * offset;
            double offZ = (world.rand.nextFloat() - 0.5d) * offset;

            double velX = (world.rand.nextFloat() - 0.5d) * velocity;
            double velY = (world.rand.nextFloat() - 0.5d) * velocity;
            double velZ = (world.rand.nextFloat() - 0.5d) * velocity;
            world.spawnParticle(type, x + offX, y + offY, z + offZ, -velX, -velY, -velZ);
        }
    }

    public static void spawnParticlesFromServer(int dimension, BlockPos pos, EnumParticleTypes particle, int count)
    {
        spawnParticlesFromServer(dimension, pos, particle, count, 1.8f, 0.9f);
    }

    public static void spawnParticlesFromServer(int dimension, BlockPos pos, EnumParticleTypes particle, int count, float offset, float velocity)
    {
        PacketHandler.INSTANCE.sendToAllAround(new MessageAddEffects(MessageAddEffects.EFFECT_PARTICLES, particle.getParticleID(),
                pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, count, offset, velocity),
                    new NetworkRegistry.TargetPoint(dimension, pos.getX(), pos.getY(), pos.getZ(), 24.0d));
    }
}
