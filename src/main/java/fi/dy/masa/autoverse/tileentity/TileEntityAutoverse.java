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
import net.minecraft.world.World;
import fi.dy.masa.autoverse.reference.Reference;

public class TileEntityAutoverse extends TileEntity
{
    protected String tileEntityName;
    protected EnumFacing facing;
    protected boolean redstoneState;

    public TileEntityAutoverse(String name)
    {
        this.facing = EnumFacing.NORTH;
        this.tileEntityName = name;
    }

    public String getTEName()
    {
        return this.tileEntityName;
    }

    public void setFacing(EnumFacing facing)
    {
        this.facing = facing;
    }

    public EnumFacing getFacing()
    {
        return this.facing;
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

    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        this.facing = EnumFacing.getFront(nbt.getByte("Facing"));
        this.redstoneState = nbt.getBoolean("Redstone");
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
