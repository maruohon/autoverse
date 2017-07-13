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
    protected boolean canInputOnSide(EnumFacing side)
    {
        return super.canInputOnSide(side) && (this.outputSidesMask & (1 << side.getIndex())) == 0;
    }

    @Override
    protected boolean checkCanOutputOnSide(EnumFacing side)
    {
        return (this.outputSidesMask & (1 << side.getIndex())) != 0 && super.checkCanOutputOnSide(side);
    }

    @Override
    public BlockPipe.Connection getConnectionType(int sideIndex)
    {
        if ((this.connectedSides & (1 << sideIndex)) != 0)
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
                this.updateConnectedSides(true);
            }

            return true;
        }

        return super.onRightClickBlock(world, pos, state, side, player, hand, hitX, hitY, hitZ);
    }

    private void toggleOutputOnSide(EnumFacing side)
    {
        this.outputSidesMask ^= (1 << side.getIndex());
        this.markDirty();
        this.notifyBlockUpdate(this.getPos());
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.outputSidesMask = nbt.getByte("OutSides");
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt = super.writeToNBTCustom(nbt);

        nbt.setByte("OutSides", (byte) this.outputSidesMask);

        return nbt;
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound nbt)
    {
        nbt = super.getUpdatePacketTag(nbt);
        nbt.setShort("sd", (short) ((this.outputSidesMask << 6) | this.connectedSides));

        return nbt;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        int mask = tag.getShort("sd");
        this.connectedSides = mask & 0x3F;
        this.outputSidesMask = mask >>> 6;

        super.handleUpdateTag(tag);

        this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
    }
}
