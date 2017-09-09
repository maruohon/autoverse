package fi.dy.masa.autoverse.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.BlockCircuit;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class TileEntityLatch extends TileEntityAutoverse
{
    private EnumFacing facing2 = EnumFacing.EAST;
    private EnumFacing facing3 = EnumFacing.WEST;
    private boolean externalPower;
    private int output;

    public TileEntityLatch()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_LATCH);
    }

    public boolean externallyPowered()
    {
        return this.externalPower;
    }

    public void setExternallyPowered(boolean powered)
    {
        this.externalPower = powered;
        // Does this lead to an infinite loop?
        //this.markDirty();
        this.getWorld().markChunkDirty(this.pos, this);
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
    public void setFacing(EnumFacing facing)
    {
        EnumFacing old = this.getFacing();

        super.setFacing(facing);

        this.updateNeighbor(old);
        this.updateNeighbor(facing);
    }

    public void setFacing2(EnumFacing facing)
    {
        EnumFacing old = this.facing2;
        this.facing2 = facing;

        this.getWorld().markChunkDirty(this.pos, this);
        this.updateNeighbor(old);
        this.updateNeighbor(facing);
    }

    public void setFacing3(EnumFacing facing)
    {
        EnumFacing old = this.facing3;
        this.facing3 = facing;

        this.getWorld().markChunkDirty(this.pos, this);
        this.updateNeighbor(old);
        this.updateNeighbor(facing);
    }

    public EnumFacing getFacing2()
    {
        return this.facing2;
    }

    public EnumFacing getFacing3()
    {
        return this.facing3;
    }

    private boolean getHasExtraFacings()
    {
        IBlockState state = this.getWorld().getBlockState(this.getPos());
        return state.getBlock() == AutoverseBlocks.CIRCUIT && state.getValue(BlockCircuit.TYPE) == BlockCircuit.CircuitType.LATCH_RS;
    }

    @Override
    public void rotate(Rotation rotationIn)
    {
        this.facing2 = rotationIn.rotate(this.facing2);
        this.facing3 = rotationIn.rotate(this.facing3);

        super.rotate(rotationIn);
    }

    private void updateNeighbor(EnumFacing facing)
    {
        if (this.getWorld() != null)
        {
            IBlockState state = this.getWorld().getBlockState(this.getPos());

            if (state.getBlock() == AutoverseBlocks.CIRCUIT)
            {
                ((BlockCircuit) state.getBlock()).notifyNeighbors(this.getWorld(), this.getPos(), facing);
            }
        }
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side, EntityPlayer player,
            EnumHand hand, float hitX, float hitY, float hitZ)
    {
        if (world.isRemote)
        {
            return true;
        }

        // Sneaking - set the 'Set' side
        if (player.isSneaking())
        {
            if (this.getHasExtraFacings() && side != this.getFacing() && side != this.facing2 && side != this.facing3)
            {
                this.setFacing2(side);
                this.notifyBlockUpdate(this.getPos());
            }
        }
        // Not sneaking - set the main facing/output side
        else
        {
            // Clicked on the output side -> set it to the opposite side
            if (side == this.getFacing() &&
                (this.getHasExtraFacings() == false || (side.getOpposite() != this.facing2 && side.getOpposite() != this.facing3)))
            {
                this.setFacing(side.getOpposite());
            }
            else if (this.getHasExtraFacings() == false || (side != this.facing2 && side != this.facing3))
            {
                this.setFacing(side);
            }
        }

        return true;
    }

    @Override
    public void onLeftClickBlock(World world, BlockPos pos, EnumFacing side, EntityPlayer player)
    {
        // Sneaking - set the 'Reset' side
        if (player.isSneaking())
        {
            if (this.getHasExtraFacings() && side != this.getFacing() && side != this.facing2 && side != this.facing3)
            {
                this.setFacing3(side);
                this.notifyBlockUpdate(this.getPos());
            }
        }
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
        byte f = (byte) (this.facing3.getIndex() << 4 | this.facing2.getIndex());
        tag.setByte("f2", f);

        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.output = tag.getByte("str");
        byte f = tag.getByte("f2");
        this.facing3 = EnumFacing.getFront(f >>> 4);
        this.facing2 = EnumFacing.getFront(f & 0xF);

        super.handleUpdateTag(tag);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.output = nbt.getByte("Output");
        this.externalPower = nbt.getBoolean("ExtPower");

        byte f = nbt.getByte("CircuitFacings");
        this.facing3 = EnumFacing.getFront(f >>> 4);
        this.facing2 = EnumFacing.getFront(f & 0xF);
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt.setByte("Output", (byte) this.output);
        nbt.setBoolean("ExtPower", this.externalPower);
        byte f = (byte) (this.facing3.getIndex() << 4 | this.facing2.getIndex());
        nbt.setByte("CircuitFacings", f);

        return super.writeToNBTCustom(nbt);
    }
}
