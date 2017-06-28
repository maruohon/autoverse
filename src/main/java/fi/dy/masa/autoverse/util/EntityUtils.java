package fi.dy.masa.autoverse.util;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityUtils
{
    public static Vec3d getEyesVec(Entity entity)
    {
        return new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
    }

    /**
     * Returns the index of the BB in the given list that the given entity is currently looking at.
     * @return the list index of the pointed box, or -1 of no hit was detected
     */
    /*
    public static int getPointedBox(Entity entity, double reach, List<AxisAlignedBB> boxes, float partialTicks)
    {
        Vec3d eyesVec = entity.getPositionEyes(partialTicks);
        Vec3d lookVec = entity.getLook(partialTicks);

        return getPointedBox(eyesVec, lookVec, reach, boxes);
    }
    */

    /**
     * Returns the index of the BB in the given list that the given vectors are currently pointing at.
     * @return the list index of the pointed box, or -1 of no hit was detected
     */
    /*
    public static int getPointedBox(Vec3d eyesVec, Vec3d lookVec, double reach, List<AxisAlignedBB> boxes)
    {
        Vec3d lookEndVec = eyesVec.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);
        double distance = reach;
        int index = -1;

        for (int i = 0; i < boxes.size(); ++i)
        {
            AxisAlignedBB bb = boxes.get(i);
            RayTraceResult rayTrace = bb.calculateIntercept(eyesVec, lookEndVec);

            if (bb.isVecInside(eyesVec))
            {
                if (distance >= 0.0D)
                {
                    distance = 0.0D;
                    index = i;
                }
            }
            else if (rayTrace != null)
            {
                double distanceTmp = eyesVec.distanceTo(rayTrace.hitVec);

                if (distanceTmp < distance)
                {
                    distance = distanceTmp;
                    index = i;
                }
            }
        }

        return index;
    }
    */

    /**
     * Returns the index of the BB in the given list that the given entity is currently looking at.
     * @return the list index of the pointed box, or null of no hit was detected
     */
    public static <T> T getPointedBox(Entity entity, double reach, Map<T, AxisAlignedBB> boxes, float partialTicks)
    {
        Vec3d eyesVec = entity.getPositionEyes(partialTicks);
        Vec3d lookVec = entity.getLook(partialTicks);

        return getPointedBox(eyesVec, lookVec, reach, boxes);
    }

    /**
     * Returns the index of the BB in the given list that the given vectors are currently pointing at.
     * @return the list index of the pointed box, or null of no hit was detected
     */
    @Nullable
    public static <T> T getPointedBox(Vec3d eyesVec, Vec3d lookVec, double reach, Map<T, AxisAlignedBB> boxMap)
    {
        Vec3d lookEndVec = eyesVec.addVector(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);
        double distance = reach;
        T key = null;

        for (Map.Entry<T, AxisAlignedBB> entry : boxMap.entrySet())
        {
            AxisAlignedBB bb = entry.getValue();
            RayTraceResult rayTrace = bb.calculateIntercept(eyesVec, lookEndVec);

            if (bb.contains(eyesVec))
            {
                if (distance >= 0.0D)
                {
                    distance = 0.0D;
                    key = entry.getKey();
                }
            }
            else if (rayTrace != null)
            {
                double distanceTmp = eyesVec.distanceTo(rayTrace.hitVec);

                if (distanceTmp < distance)
                {
                    distance = distanceTmp;
                    key = entry.getKey();
                }
            }
        }

        return key;
    }

    /**
     * Drops/spawns EntityItems to the world from the provided ItemStack stack.
     * The number of items dropped is dictated by the parameter amountOverride.
     * If amountOverride > 0, then stack is only the ItemStack template and amountOverride is the number of items that will be dropped.
     * (Thus amountOverride can also be larger than stack.stackSize.)
     * If amountOverride <= 0, then stack.stackSize is used for the amount to be dropped.
     * @param worldIn
     * @param pos
     * @param stack The template ItemStack of the dropped items.
     * @param amountOverride Amount of items to drop. If amountOverride is > 0, stack is only a template. If <= 0, stack.stackSize is used.
     * @param dropFullStacks If false, then the stackSize of the the spawned EntityItems is randomized between 10..32
     * @param randomMotion If true, then a small amount on random motion is applied to the spawned entities
     */
    public static void dropItemStacksInWorld(World worldIn, BlockPos pos, ItemStack stack, int amountOverride, boolean dropFullStacks)
    {
        dropItemStacksInWorld(worldIn, pos, stack, amountOverride, dropFullStacks, true);
    }

    /**
     * Drops/spawns EntityItems to the world from the provided ItemStack stack.
     * The number of items dropped is dictated by the parameter amountOverride.
     * If amountOverride > 0, then stack is only the ItemStack template and amountOverride is the number of items that will be dropped.
     * (Thus amountOverride can also be larger than stack.stackSize.)
     * If amountOverride <= 0, then stack.stackSize is used for the amount to be dropped.
     * @param worldIn
     * @param pos
     * @param stack The template ItemStack of the dropped items.
     * @param amountOverride Amount of items to drop. If amountOverride is > 0, stack is only a template. If <= 0, stack.stackSize is used.
     * @param dropFullStacks If false, then the stackSize of the the spawned EntityItems is randomized between 10..32
     * @param randomMotion If true, then a small amount on random motion is applied to the spawned entities
     */
    public static void dropItemStacksInWorld(World worldIn, BlockPos pos, ItemStack stack, int amountOverride, boolean dropFullStacks, boolean randomMotion)
    {
        double x = worldIn.rand.nextFloat() * -0.5d + 0.75d + pos.getX();
        double y = worldIn.rand.nextFloat() * -0.5d + 0.75d + pos.getY();
        double z = worldIn.rand.nextFloat() * -0.5d + 0.75d + pos.getZ();

        dropItemStacksInWorld(worldIn, new Vec3d(x, y, z), stack, amountOverride, dropFullStacks, randomMotion);
    }

    /**
     * Drops/spawns EntityItems to the world from the provided ItemStack stack.
     * The number of items dropped is dictated by the parameter amountOverride.
     * If amountOverride > 0, then stack is only the ItemStack template and amountOverride is the number of items that will be dropped.
     * (Thus amountOverride can also be larger than stack.stackSize.)
     * If amountOverride <= 0, then stack.stackSize is used for the amount to be dropped.
     * @param worldIn
     * @param pos The exact position where the EntityItems will be spawned
     * @param stack The template ItemStack of the dropped items.
     * @param amountOverride Amount of items to drop. If amountOverride is > 0, stack is only a template. If <= 0, stack.stackSize is used.
     * @param dropFullStacks If false, then the stackSize of the the spawned EntityItems is randomized between 10..32
     * @param randomMotion If true, then a small amount on random motion is applied to the spawned entities
     */
    public static void dropItemStacksInWorld(World worldIn, Vec3d pos, ItemStack stack, int amountOverride, boolean dropFullStacks, boolean randomMotion)
    {
        int amount = stack.getCount();
        int max = stack.getMaxStackSize();
        int num = max;

        if (amountOverride > 0)
        {
            amount = amountOverride;
        }

        while (amount > 0)
        {
            if (dropFullStacks == false)
            {
                num = Math.min(worldIn.rand.nextInt(23) + 10, max);
            }

            num = Math.min(num, amount);
            amount -= num;

            ItemStack dropStack = stack.copy();
            dropStack.setCount(num);

            EntityItem entityItem = new EntityItem(worldIn, pos.x, pos.y, pos.z, dropStack);

            if (randomMotion)
            {
                double motionScale = 0.04d;
                entityItem.motionX = worldIn.rand.nextGaussian() * motionScale;
                entityItem.motionY = worldIn.rand.nextGaussian() * motionScale + 0.3d;
                entityItem.motionZ = worldIn.rand.nextGaussian() * motionScale;
            }
            else
            {
                entityItem.motionX = 0d;
                entityItem.motionY = 0d;
                entityItem.motionZ = 0d;
            }

            worldIn.spawnEntity(entityItem);
        }
    }
}
