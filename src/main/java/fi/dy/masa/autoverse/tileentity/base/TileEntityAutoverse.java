package fi.dy.masa.autoverse.tileentity.base;

import java.util.Arrays;
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
import net.minecraftforge.common.util.Constants;
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
    private String tileEntityName;
    private EnumFacing facing = BlockAutoverse.DEFAULT_FACING;
    private boolean redstoneState;
    private boolean slimModel;
    private FakePlayer fakePlayer;

    public TileEntityAutoverse(String name)
    {
        this.tileEntityName = name;
    }

    public String getTEName()
    {
        return this.tileEntityName;
    }

    public boolean isSlimModel()
    {
        return this.slimModel;
    }

    public void setIsSlimModel(boolean isSlim)
    {
        this.slimModel = isSlim;
    }

    public void setFacing(EnumFacing facing)
    {
        this.facing = facing;

        if (this.getWorld() != null && this.getWorld().isRemote == false)
        {
            this.markDirty();
            this.notifyBlockUpdate(this.getPos());
        }
    }

    public EnumFacing getFacing()
    {
        return this.facing;
    }

    protected EnumFacing getOppositeFacing()
    {
        return this.facing.getOpposite();
    }

    public boolean getRedstoneState()
    {
        return this.redstoneState;
    }

    protected void setRedstoneState(boolean powered)
    {
        this.redstoneState = powered;
    }

    /**
     * Returns the position on the front side of this tile
     */
    protected BlockPos getFrontPosition()
    {
        return this.getPos().offset(this.facing);
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
                this.setFacing(EnumFacing.byIndex(value));
                return true;

            case 3:
                this.setIsSlimModel(value == 1);
                return true;
        }

        return false;
    }

    /**
     * Returns an array of the custom properties, to be used in the Block Placer Configurator
     * @return
     */
    public int[] getProperties()
    {
        int[] values = new int[4];
        Arrays.fill(values, -1);

        values[0] = this.getFacing().getIndex();
        values[3] = this.slimModel ? 1 : 0;

        return values;
    }

    protected Vec3d getSpawnedItemPosition()
    {
        return this.getSpawnedItemPosition(this.facing);
    }

    protected Vec3d getSpawnedItemPosition(EnumFacing side)
    {
        double x = this.getPos().getX() + 0.5 + side.getXOffset() * 0.625;
        double y = this.getPos().getY() + 0.5 + side.getYOffset() * 0.5;
        double z = this.getPos().getZ() + 0.5 + side.getZOffset() * 0.625;

        if (side == EnumFacing.DOWN)
        {
            y -= 0.25;
        }

        return new Vec3d(x, y, z);
    }

    public void setPlacementProperties(World world, BlockPos pos, @Nonnull ItemStack stack, @Nonnull NBTTagCompound tag)
    {
        if (tag.hasKey("machine.slim_model", Constants.NBT.TAG_BYTE))
        {
            this.setIsSlimModel(tag.getByte("machine.slim_model") == 1);
        }
    }

    protected boolean applyPlacementPropertiesFrom(World world, BlockPos pos, EntityPlayer player, ItemStack stack)
    {
        if (stack.isEmpty() == false && stack.getItem() instanceof ItemBlockAutoverse)
        {
            ItemBlockAutoverse item = (ItemBlockAutoverse) stack.getItem();

            if (item.hasPlacementProperty(stack)) // && item.getBlock() == this.getBlockType()
            {
                ItemType type = new ItemType(stack, item.getPlacementProperty(stack).isNBTSensitive());
                NBTTagCompound tag = PlacementProperties.getInstance().getPropertyTag(player.getUniqueID(), type);

                if (tag != null)
                {
                    this.setPlacementProperties(world, pos, stack, tag);
                    this.markDirty();
                    this.notifyBlockUpdate(this.getPos());
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
                    new GameProfile(new UUID(dim, dim), Reference.MOD_ID + ":" + this.tileEntityName));
        }

        return this.fakePlayer;
    }

    /**
     * @return true if something happened, and further processing (such as opening the GUI) should not happen
     */
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking() && player.getHeldItemMainhand().isEmpty())
        {
            ItemStack stackOffhand = player.getHeldItemOffhand();

            // Sneak + right clicking with an empty main hand, and an Autoverse ItemBlock in the offhand,
            // (re-)apply the placement properties from that item.
            if (stackOffhand.isEmpty() == false && stackOffhand.getItem() instanceof ItemBlockAutoverse)
            {
                if (world.isRemote == false)
                {
                    this.applyPlacementPropertiesFrom(world, pos, player, stackOffhand);
                }

                return true;
            }
        }

        return false;
    }

    public boolean onLeftClickBlock(World world, BlockPos pos, EnumFacing side, EntityPlayer player)
    {
        return false;
    }

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

    protected void onRedstoneChange(boolean state)
    {
    }

    protected void reScheduleUpdateIfSooner(int delay)
    {
        BlockPos pos = this.getPos();
        World world = this.getWorld();
        long targetTime = world.getTotalWorldTime() + delay;

        StructureBoundingBox bb = new StructureBoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
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

    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        this.redstoneState = nbt.getBoolean("Redstone");
        this.slimModel = nbt.getBoolean("Slim");
        this.facing = EnumFacing.byIndex(nbt.getByte("Facing"));
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
        nbt.setBoolean("Slim", this.slimModel);

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
        int mask = (this.slimModel ? 0x08 : 0) | (this.getFacing().getIndex() & 0x07);
        tag.setByte("f", (byte) mask);
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
            int value = tag.getByte("f");
            this.setFacing(EnumFacing.byIndex(value & 0x07));
            this.slimModel = (value & 0x08) == 0x08;
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
