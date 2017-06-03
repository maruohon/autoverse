package fi.dy.masa.autoverse.util;

import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockUtils
{
    /**
     * Sets the block state in the world and plays the placement sound.
     * @return true if setting the block state succeeded
     */
    public static boolean setBlockStateWithPlaceSound(World world, BlockPos pos, IBlockState newState, int setBlockStateFlags)
    {
        boolean success = world.setBlockState(pos, newState, setBlockStateFlags);

        if (success)
        {
            SoundType soundtype = newState.getBlock().getSoundType(newState, world, pos, null);
            world.playSound(null, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS,
                    (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
        }

        return success;
    }
}
