package fi.dy.masa.autoverse.tileentity;

import java.util.Arrays;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.BlockRepeaterBase;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public abstract class TileEntityRepeaterBase extends TileEntityAutoverse
{
    protected EnumFacing outputFacing = EnumFacing.SOUTH;
    protected int output = -1;

    public TileEntityRepeaterBase(String name)
    {
        super(name);
    }

    public int getOutputStrength()
    {
        return this.output;
    }

    public EnumFacing getOutputFacing()
    {
        return this.outputFacing;
    }

    public void setOutputStrength(int strength)
    {
        this.output = strength;
        // Does this lead to an infinite loop?
        //this.markDirty();
        this.getWorld().markChunkDirty(this.pos, this);
    }

    public void setOutputFacing(EnumFacing facing)
    {
        this.outputFacing = facing;
        this.notifyBlockUpdate(this.getPos());
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        if (propId == 0)
        {
            this.setOutputFacing(EnumFacing.byIndex(value));
            return true;
        }

        return false;
    }

    @Override
    public int[] getProperties()
    {
        int[] values = new int[4];
        Arrays.fill(values, -1);

        values[0] = this.outputFacing.getIndex();

        return values;
    }

    @Override
    public void rotate(Rotation rotationIn)
    {
        this.setOutputFacing(rotationIn.rotate(this.outputFacing));
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, EnumFacing side, EntityPlayer player, EnumHand hand)
    {
        if (world.isRemote)
        {
            return true;
        }

        EnumFacing oldOutputSide = this.getOutputFacing();

        // Sneaking - set the output side
        if (player.isSneaking() && side != oldOutputSide && side != this.getFacing())
        {
            this.setOutputFacing(side);

            IBlockState state = world.getBlockState(pos);
            BlockRepeaterBase.notifyOutputs(state, world, pos, oldOutputSide);
            BlockRepeaterBase.notifyOutputs(state, world, pos, side);

            return true;
        }

        return super.onRightClickBlock(world, pos, side, player, hand);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState)
    {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public boolean hasGui()
    {
        return false;
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag = super.getUpdatePacketTag(tag);

        tag.setByte("str", (byte) this.output);
        tag.setByte("of", (byte) this.outputFacing.getIndex());

        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.output = tag.getByte("str");
        this.outputFacing = EnumFacing.byIndex(tag.getByte("of"));

        super.handleUpdateTag(tag);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.output = nbt.getByte("Output");
        this.outputFacing = EnumFacing.byIndex(nbt.getByte("OutputFacing"));
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt.setByte("Output", (byte) this.output);
        nbt.setByte("OutputFacing", (byte) this.outputFacing.getIndex());

        return super.writeToNBTCustom(nbt);
    }
}
