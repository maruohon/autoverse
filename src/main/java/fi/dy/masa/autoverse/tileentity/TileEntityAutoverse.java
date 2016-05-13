package fi.dy.masa.autoverse.tileentity;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.reference.Reference;

public class TileEntityAutoverse extends TileEntity
{
    protected String tileEntityName;
    protected int rotation;

    public TileEntityAutoverse(String name)
    {
        this.rotation = 0;
        this.tileEntityName = name;
    }

    public String getTEName()
    {
        return this.tileEntityName;
    }

    public void setRotation(int rot)
    {
        this.rotation = rot;
    }

    public int getRotation()
    {
        return this.rotation;
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) { }

    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        this.rotation = nbt.getByte("Rotation");
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
        nbt.setByte("Rotation", (byte)this.rotation);
    }

    public NBTTagCompound getDescriptionPacketTag(NBTTagCompound nbt)
    {
        nbt.setByte("r", (byte)(this.getRotation() & 0x07));
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

        if (nbt.hasKey("r") == true)
        {
            this.setRotation((byte)(nbt.getByte("r") & 0x07));
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
