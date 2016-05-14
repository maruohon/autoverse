package fi.dy.masa.autoverse.util;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityUtils
{
    /**
     * Drops/spawns EntityItems to the world from the provided ItemStack stack.
     * The number of items dropped is dictated by the parameter amount.
     * If amountOverride > 0, then stack is only the ItemStack template and amountOverride is the number of items that will be dropped.
     * (Thus amountOverride can also be larger than stack.stackSize.)
     * If amountOverride <= 0, then stack.stackSize is used for the amount to be dropped.
     * @param worldIn
     * @param pos
     * @param stack The template ItemStack of the dropped items.
     * @param amountOverride Amount of items to drop. If amountOverride is > 0, stack is only a template. If <= 0, stack.stackSize is used.
     * @param dropFullStacks If false, then the stackSize of the the spawned EntityItems is randomized between 10..32
     * @param randomMotion If true, then some random motion is added to the spawned EntityItem
     */
    public static void dropItemStacksInWorld(World worldIn, BlockPos pos, ItemStack stack, int amountOverride, boolean dropFullStacks, boolean randomMotion)
    {
        dropItemStacksInWorld(worldIn, new Vec3d(pos.getX(), pos.getY(), pos.getZ()), stack, amountOverride, dropFullStacks, randomMotion);
    }

    public static void dropItemStacksInWorld(World worldIn, Vec3d pos, ItemStack stack, int amountOverride, boolean dropFullStacks, boolean randomMotion)
    {
        if (stack == null)
        {
            return;
        }

        int amount = stack.stackSize;
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
            ItemStack dropStack = stack.copy();
            dropStack.stackSize = num;
            amount -= num;

            EntityItem entityItem = new EntityItem(worldIn, pos.xCoord, pos.yCoord, pos.zCoord, dropStack);

            if (randomMotion == true)
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

            worldIn.spawnEntityInWorld(entityItem);
        }
    }
}
