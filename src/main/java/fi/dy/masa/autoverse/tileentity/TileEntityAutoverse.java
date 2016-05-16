package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.reference.Reference;

public class TileEntityAutoverse extends TileEntity
{
    protected String tileEntityName;
    protected EnumFacing facing;
    protected EnumFacing facingOpposite;
    protected BlockPos posFront;
    //protected BlockPos posBack;
    protected boolean redstoneState;
    protected boolean tickScheduled;

    public TileEntityAutoverse(String name)
    {
        this.tileEntityName = name;
    }

    public String getTEName()
    {
        return this.tileEntityName;
    }

    public void setFacing(EnumFacing facing)
    {
        this.facing = facing;
        this.facingOpposite = this.facing.getOpposite();
        this.posFront = this.getPos().offset(this.facing);
        //this.posBack = this.getPos().offset(this.facingOpposite);
    }

    public EnumFacing getFacing()
    {
        return this.facing;
    }

    protected Vec3d getSpawnedItemPosition()
    {
        double x = this.getPos().getX() + 0.5 + this.facing.getFrontOffsetX() * 0.625;
        double y = this.getPos().getY() + 0.5 + this.facing.getFrontOffsetY() * 0.5;
        double z = this.getPos().getZ() + 0.5 + this.facing.getFrontOffsetZ() * 0.625;

        if (this.facing == EnumFacing.DOWN)
        {
            y -= 0.25;
        }

        return new Vec3d(x, y, z);
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        boolean redstone = this.worldObj.isBlockPowered(this.getPos());

        if (redstone != this.redstoneState)
        {
            this.onRedstoneChange(redstone);
        }

        this.redstoneState = redstone;
    }

    protected void onRedstoneChange(boolean state)
    {
    }

    public void onBlockTick(IBlockState state, Random rand)
    {
    }

    public void scheduleBlockTick(int delay)
    {
        if (this.tickScheduled == false)
        {
            this.getWorld().scheduleUpdate(this.getPos(), this.getBlockType(), delay);
            this.tickScheduled = true;
        }
    }

    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        this.redstoneState = nbt.getBoolean("Redstone");

        // Update the opposite and the front and back BlockPos
        this.setFacing(EnumFacing.getFront(nbt.getByte("Facing")));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.readFromNBTCustom(nbt); // This call needs to be at the super-most custom TE class
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setString("Version", Reference.MOD_VERSION);
        nbt.setByte("Facing", (byte)this.facing.getIndex());
        nbt.setBoolean("Redstone", this.redstoneState);
    }

    public NBTTagCompound getDescriptionPacketTag(NBTTagCompound nbt)
    {
        nbt.setByte("f", (byte)(this.getFacing().getIndex() & 0x07));
        return nbt;
    }

    @Override
    public Packet<INetHandlerPlayClient> getDescriptionPacket()
    {
        if (this.worldObj != null)
        {
            return new SPacketUpdateTileEntity(this.getPos(), 0, this.getDescriptionPacketTag(new NBTTagCompound()));
        }

        return null;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet)
    {
        NBTTagCompound nbt = packet.getNbtCompound();

        if (nbt.hasKey("f") == true)
        {
            this.setFacing(EnumFacing.getFront((byte)(nbt.getByte("f") & 0x07)));
        }

        IBlockState state = this.worldObj.getBlockState(this.getPos());
        this.worldObj.notifyBlockUpdate(this.getPos(), state, state, 3);
    }

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + "(" + this.getPos() + ")@" + System.identityHashCode(this);
    }
}
