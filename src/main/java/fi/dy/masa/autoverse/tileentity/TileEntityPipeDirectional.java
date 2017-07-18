package fi.dy.masa.autoverse.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.BlockPipe;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityPipeDirectional extends TileEntityPipe
{
    private int outputSidesMask;

    public TileEntityPipeDirectional()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_PIPE_DIRECTIONAL);
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        if (propId == 3)
        {
            this.setOutputMask(value);
            return true;
        }
        else
        {
            return super.applyProperty(propId, value);
        }
    }

    private void toggleOutputOnSide(EnumFacing side)
    {
        this.setOutputMask(this.outputSidesMask ^ (1 << side.getIndex()));
    }

    private void setOutputMask(int mask)
    {
        this.outputSidesMask = mask & 0x3F;
        this.updateConnectedSides(true);
        this.markDirty();

        this.getWorld().notifyNeighborsOfStateChange(this.getPos(), this.getBlockType(), false);
        this.notifyBlockUpdate(this.getPos());
    }

    /*
    @Override
    protected boolean canInputOnSide(EnumFacing side)
    {
        return super.canInputOnSide(side) && (this.outputSidesMask & (1 << side.getIndex())) == 0;
    }
    */

    @Override
    protected boolean checkCanOutputOnSide(EnumFacing side)
    {
        return (this.outputSidesMask & (1 << side.getIndex())) != 0 && super.checkCanOutputOnSide(side);
    }

    @Override
    public BlockPipe.Connection getConnectionType(int sideIndex)
    {
        if ((this.getConnectedSidesMask() & (1 << sideIndex)) != 0)
        {
            if ((this.outputSidesMask & (1 << sideIndex)) != 0)
            {
                return BlockPipe.Connection.OUTPUT;
            }
            else 
            {
                return BlockPipe.Connection.BASIC;
            }
        }

        return BlockPipe.Connection.NONE;
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking() &&
            player.getHeldItemMainhand().isEmpty() &&
            player.getHeldItemOffhand().isEmpty() == false)
        {
            if (world.isRemote == false)
            {
                EnumFacing targetSide = this.getActionTargetSide(world, pos, state, side, player);
                this.toggleOutputOnSide(targetSide);
                this.scheduleCurrentWork(this.getDelay());
            }

            return true;
        }

        return super.onRightClickBlock(world, pos, state, side, player, hand, hitX, hitY, hitZ);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.outputSidesMask = nbt.getByte("Outs");
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt = super.writeToNBTCustom(nbt);

        nbt.setByte("Outs", (byte) this.outputSidesMask);

        return nbt;
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound nbt)
    {
        nbt.setShort("sd", (short) ((this.outputSidesMask << 6) | this.getConnectedSidesMask()));

        return nbt;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        int mask = tag.getShort("sd");
        this.setConnectedSidesMask(mask & 0x3F);
        this.outputSidesMask = mask >>> 6;

        this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
    }
}
