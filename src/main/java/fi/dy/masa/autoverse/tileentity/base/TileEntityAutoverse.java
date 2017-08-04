package fi.dy.masa.autoverse.tileentity.base;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.base.Predicates;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.ItemType;
import fi.dy.masa.autoverse.util.PlacementProperties;

public abstract class TileEntityAutoverse extends TileEntity
{
    protected String tileEntityName;
    protected EnumFacing facing = EnumFacing.UP;
    protected EnumFacing facingOpposite = EnumFacing.DOWN;
    protected BlockPos posFront = BlockPos.ORIGIN;
    protected boolean redstoneState;
    protected FakePlayer fakePlayer;

    public TileEntityAutoverse(String name)
    {
        this.facing = BlockAutoverse.DEFAULT_FACING;
        this.tileEntityName = name;
    }

    public String getTEName()
    {
        return this.tileEntityName;
    }

    @Override
    public void setPos(BlockPos posIn)
    {
        super.setPos(posIn);

        this.posFront = this.getPos().offset(this.getFacing());
    }

    public void setFacing(EnumFacing facing)
    {
        this.facing = facing;
        this.facingOpposite = facing.getOpposite();
        this.posFront = this.getPos().offset(facing);
        this.markDirty();

        if (this.getWorld() != null && this.getWorld().isRemote == false)
        {
            this.notifyBlockUpdate(this.getPos());
        }
    }

    public EnumFacing getFacing()
    {
        return this.facing;
    }

    @Override
    public void mirror(Mirror mirrorIn)
    {
        this.rotate(mirrorIn.toRotation(this.facing));
    }

    @Override
    public void rotate(Rotation rotationIn)
    {
        this.setFacing(rotationIn.rotate(this.getFacing()));
    }

    /**
     * Applies a property this this TileEntity.
     * The properties are TileEntity-specific "useful values to set".
     * This will be used by the Programmable Block Placer to configure/orient
     * the blocks upon placement.
     * @param propId
     * @param value
     * @return true if setting the property to the given value succeeded
     */
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 0:
                this.setFacing(EnumFacing.getFront(value));
                return true;
        }

        return false;
    }

    protected Vec3d getSpawnedItemPosition()
    {
        return this.getSpawnedItemPosition(this.facing);
    }

    protected Vec3d getSpawnedItemPosition(EnumFacing side)
    {
        double x = this.getPos().getX() + 0.5 + side.getFrontOffsetX() * 0.625;
        double y = this.getPos().getY() + 0.5 + side.getFrontOffsetY() * 0.5;
        double z = this.getPos().getZ() + 0.5 + side.getFrontOffsetZ() * 0.625;

        if (side == EnumFacing.DOWN)
        {
            y -= 0.25;
        }

        return new Vec3d(x, y, z);
    }

    public void setPlacementProperties(World world, BlockPos pos, @Nonnull ItemStack stack, @Nonnull NBTTagCompound tag)
    {
    }

    protected boolean applyPlacementPropertiesFrom(World world, BlockPos pos, EntityPlayer player, ItemStack stack)
    {
        if (stack.isEmpty() == false && stack.getItem() instanceof ItemBlockAutoverse)
        {
            ItemBlockAutoverse item = (ItemBlockAutoverse) stack.getItem();

            if (item.getBlock() == this.getBlockType() && item.hasPlacementProperty(stack))
            {
                ItemType type = new ItemType(stack, item.getPlacementProperty(stack).isNBTSensitive());
                NBTTagCompound tag = PlacementProperties.getInstance().getPropertyTag(player.getUniqueID(), type);

                if (tag != null)
                {
                    this.setPlacementProperties(world, pos, stack, tag);
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isUsableByPlayer(EntityPlayer player)
    {
        return this.getWorld().getTileEntity(this.getPos()) == this && player.getDistanceSq(this.getPos()) <= 64.0d;
    }

    /**
     * Gets a FakePlayer, which are unique per dimension and per TileEntity type.
     * ONLY call this on the server side!!!
     * @return
     */
    @Nonnull
    protected FakePlayer getPlayer()
    {
        if (this.fakePlayer == null && this.getWorld() instanceof WorldServer)
        {
            int dim = this.getWorld().provider.getDimension();

            this.fakePlayer = FakePlayerFactory.get((WorldServer) this.getWorld(),
                    new GameProfile(new UUID(dim, dim), Reference.MOD_ID + this.tileEntityName));
        }

        return this.fakePlayer;
    }

    /**
     * @return true if something happened, and further processing (such as opening the GUI) should not happen
     */
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        return false;
    }

    public void onLeftClickBlock(World world, BlockPos pos, EntityPlayer player) { }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        this.updateRedstoneState(true);
    }

    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
    }

    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
    }

    public void scheduleBlockUpdate(int delay, boolean force)
    {
        World world = this.getWorld();

        if (world != null)// && (force || world.isUpdateScheduled(this.getPos(), this.getBlockType()) == false))
        {
            world.scheduleUpdate(this.getPos(), this.getBlockType(), delay);
        }
    }

    public void updateRedstoneState(boolean callHook)
    {
        boolean redstone = this.getWorld().isBlockPowered(this.getPos());

        if (redstone != this.redstoneState)
        {
            this.redstoneState = redstone;

            if (callHook)
            {
                this.onRedstoneChange(redstone);
            }
        }
    }

    protected void reScheduleUpdateIfSooner(int delay)
    {
        BlockPos pos = this.getPos();
        World world = this.getWorld();
        long targetTime = world.getTotalWorldTime() + delay;

        StructureBoundingBox bb = new StructureBoundingBox(pos.getX(), pos.getZ(), pos.getX() + 1, pos.getZ() + 1);
        List<NextTickListEntry> list = world.getPendingBlockUpdates(bb, false);

        // If there is an existing scheduled update, then remove the old one and schedule a new one
        if (list != null && list.size() == 1)
        {
            // Don't re-schedule if there is a tick pending for this tick, or if there is an older
            // scheduled tick sooner than the new requested tick
            if (list.get(0).scheduledTime > targetTime && world.isBlockTickPending(pos, this.getBlockType()) == false)
            {
                // Remove the old scheduled update
                world.getPendingBlockUpdates(bb, true);
                this.scheduleBlockUpdate(delay, true);
            }
        }
        // No existing scheduled updates, schedule a new one
        else
        {
            this.scheduleBlockUpdate(delay, true);
        }
    }

    protected void notifyBlockUpdate(BlockPos pos)
    {
        IBlockState state = this.getWorld().getBlockState(pos);
        this.getWorld().notifyBlockUpdate(pos, state, state, 3);
    }

    protected void onRedstoneChange(boolean state)
    {
        if (state)
        {
            World world = this.getWorld();
            this.scheduleBlockUpdate(world.getBlockState(this.getPos()).getBlock().tickRate(world), false);
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

    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt.setString("Version", Reference.MOD_VERSION);
        nbt.setByte("Facing", (byte)this.facing.getIndex());
        nbt.setBoolean("Redstone", this.redstoneState);

        return nbt;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);

        this.writeToNBTCustom(nbt);

        return nbt;
    }

    /**
     * Get the data used for syncing the TileEntity to the client.
     * The data returned from this method doesn't have the position,
     * the position will be added in getUpdateTag() which calls this method.
     */
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag.setByte("f", (byte)(this.getFacing().getIndex() & 0x07));
        return tag;
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        // The tag from this method is used for the initial chunk packet,
        // and it needs to have the TE position!
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("x", this.getPos().getX());
        nbt.setInteger("y", this.getPos().getY());
        nbt.setInteger("z", this.getPos().getZ());

        // Add the per-block data to the tag
        return this.getUpdatePacketTag(nbt);
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        if (this.getWorld() != null)
        {
            return new SPacketUpdateTileEntity(this.getPos(), 0, this.getUpdatePacketTag(new NBTTagCompound()));
        }

        return null;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        if (tag.hasKey("f"))
        {
            this.setFacing(EnumFacing.getFront((byte)(tag.getByte("f") & 0x07)));
        }

        this.notifyBlockUpdate(this.getPos());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet)
    {
        this.handleUpdateTag(packet.getNbtCompound());
    }

    public void performGuiAction(EntityPlayer player, int action, int element)
    {
    }

    protected void sendPacketToWatchers(IMessage message, BlockPos pos)
    {
        World world = this.getWorld();

        if (world instanceof WorldServer)
        {
            WorldServer worldServer = (WorldServer) world;
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            PlayerChunkMap map = worldServer.getPlayerChunkMap();

            for (EntityPlayerMP player : worldServer.getPlayers(EntityPlayerMP.class, Predicates.alwaysTrue()))
            {
                if (/*player.getDistanceSq(pos) <= 900 && */map.isPlayerWatchingChunk(player, chunkX, chunkZ))
                {
                    PacketHandler.INSTANCE.sendTo(message, player);
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + "(" + this.getPos() + ")@" + System.identityHashCode(this);
    }

    public boolean hasGui()
    {
        return true;
    }

    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return null;
    }

    public Object getGui(EntityPlayer player)
    {
        return null;
    }
}
