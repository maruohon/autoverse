package fi.dy.masa.autoverse.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class TileEntityInventoryReader extends TileEntityAutoverse
{
    private int output;

    public TileEntityInventoryReader()
    {
        super(ReferenceNames.NAME_BLOCK_INVENTORY_READER);
    }

    public int getOutpuStrength()
    {
        return this.output;
    }

    public void setOutputStrength(int strength)
    {
        this.output = strength;
        // Does this lead to an infinite loop?
        //this.markDirty();
        this.getWorld().markChunkDirty(this.pos, this);
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

        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.output = tag.getByte("str");

        super.handleUpdateTag(tag);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.output = nbt.getByte("Output");
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt.setByte("Output", (byte) this.output);

        return super.writeToNBTCustom(nbt);
    }
}
