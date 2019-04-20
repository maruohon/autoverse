package fi.dy.masa.autoverse.tileentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockPipe;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.gui.client.GuiPipe;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerPipe;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.network.message.ISyncableTile;
import fi.dy.masa.autoverse.network.message.MessageSyncTileEntity;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.NBTUtils;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntityPipe extends TileEntityAutoverseInventory implements ISyncableTile
{
    private final IItemHandler[] inputInventories;
    protected final EnumFacing[][] validOutputSidesPerSide;
    private final int scheduledTimes[] = new int[6];
    private int connectedSidesMask;
    private int disabledSidesMask;
    //protected int cloggedItemsMask;
    private int delay = Configs.pipeDefaultDelay;
    protected boolean disableUpdateScheduling;
    protected boolean disableNeighorNotification;

    // Client-side or client sync/rendering related things
    public final NonNullList<ItemStack> stacksLast = NonNullList.withSize(6, ItemStack.EMPTY);
    public final NonNullList<ItemStack> stacksOut = NonNullList.withSize(6, ItemStack.EMPTY);
    public byte delaysClient[] = new byte[6];
    public byte delaysOut[] = new byte[6];
    public byte isInput[] = new byte[6];
    public byte outputDirections[] = new byte[6];
    public float partialTicksLast;

    public TileEntityPipe()
    {
        this(ReferenceNames.NAME_BLOCK_PIPE);
    }

    protected TileEntityPipe(String name)
    {
        super(name);

        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, 6, Configs.pipeMaxStackSize, false, "Items", this);
        this.itemHandlerBase.setStackLimit(1);
        this.inputInventories = new IItemHandler[6];

        for (int i = 0; i < 6; i++)
        {
            this.inputInventories[i] = new InventoryWrapperSides(this.itemHandlerBase, i, this);
        }

        this.validOutputSidesPerSide = new EnumFacing[6][];

        for (int i = 0; i < 6; i++)
        {
            this.validOutputSidesPerSide[i] = new EnumFacing[0];
        }

        Arrays.fill(this.scheduledTimes, -1);
        Arrays.fill(this.delaysClient, (byte) -1);
        Arrays.fill(this.delaysOut, (byte) -1);
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 0:
                this.setDelay(value);
                return true;

            case 1:
                this.setMaxStackSize(value);
                return true;

            case 2:
                this.setDisabledSidesMask(value);
                return true;
        }

        return false;
    }

    @Override
    public int[] getProperties()
    {
        int[] values = new int[4];
        Arrays.fill(values, -1);

        values[0] = this.getDelay();
        values[1] = this.itemHandlerBase.getInventoryStackLimit();
        values[2] = this.disabledSidesMask;

        return values;
    }

    @Override
    public void setPlacementProperties(World world, BlockPos pos, ItemStack stack, NBTTagCompound tag)
    {
        if (tag.hasKey("pipe.delay", Constants.NBT.TAG_BYTE))
        {
            this.setDelayFromByte(tag.getByte("pipe.delay"));
            this.markDirty();
        }

        if (tag.hasKey("pipe.max_stack", Constants.NBT.TAG_BYTE))
        {
            this.setMaxStackSize(tag.getByte("pipe.max_stack"));
            this.markDirty();
        }
    }

    @Override
    public void onLoad()
    {
        if (this.getWorld().isRemote == false)
        {
            this.setScheduledTimesFromDelays();
        }
    }

    @Override
    public void rotate(Rotation rotation)
    {
        if (rotation != Rotation.NONE)
        {
            this.connectedSidesMask = PositionUtils.rotateFacingMask(this.connectedSidesMask, rotation);
            this.disabledSidesMask  = PositionUtils.rotateFacingMask(this.disabledSidesMask, rotation);
            //this.cloggedItemsMask   = PositionUtils.rotateFacingMask(this.cloggedItemsMask, rotation);

            this.rotateSidesPerSideArray(this.validOutputSidesPerSide, rotation);

            if (this.getWorld() != null && this.getWorld().isRemote == false)
            {
                this.markDirty();
                this.notifyBlockUpdate(this.getPos());
            }
        }
    }

    protected void rotateSidesPerSideArray(EnumFacing[][] sideArr, Rotation rotation)
    {
        if (rotation != Rotation.NONE)
        {
            int[] facingIndexRotations = PositionUtils.getFacingIndexRotations(rotation);
            EnumFacing[][] oldSides = Arrays.copyOf(sideArr, 6);

            for (int inputSide = 0; inputSide < 6; inputSide++)
            {
                List<EnumFacing> sidesNew = new ArrayList<EnumFacing>();
                EnumFacing[] sidesOld = oldSides[inputSide];

                for (int outputSide = 0; outputSide < sidesOld.length; outputSide++)
                {
                    EnumFacing side = sidesOld[outputSide];

                    // Maintain the opposite side as the first entry, if it's present
                    if (side.getIndex() == PositionUtils.FACING_OPPOSITE_INDICES[inputSide])
                    {
                        sidesNew.add(0, rotation.rotate(side));
                    }
                    else
                    {
                        sidesNew.add(rotation.rotate(side));
                    }
                }

                sideArr[facingIndexRotations[inputSide]] = sidesNew.toArray(new EnumFacing[sidesNew.size()]);
            }
        }
    }

    public int getDelay()
    {
        return this.delay;
    }

    public void setDelayFromByte(byte delay)
    {
        this.setDelay(((int) delay) & 0xFF);
    }

    private void setDelay(int delay)
    {
        this.delay = MathHelper.clamp(delay, Configs.pipeMinimumDelay, 127);
    }

    public int getDelayForSide(int side)
    {
        return this.scheduledTimes[side];
    }

    public void setMaxStackSize(int maxSize)
    {
        this.itemHandlerBase.setStackLimit(MathHelper.clamp(maxSize, 1, Configs.pipeMaxStackSize));
    }

    public int getConnectedSidesMask()
    {
        return this.connectedSidesMask;
    }

    protected void setConnectedSidesMask(int mask)
    {
        this.connectedSidesMask = mask;
    }

    @Override
    public IItemHandler getInventoryForInventoryReader(EnumFacing side)
    {
        return this.itemHandlerBase;
    }

    public BlockPipe.Connection getConnectionType(int sideIndex)
    {
        return (this.connectedSidesMask & (1 << sideIndex)) != 0 ? BlockPipe.Connection.BASIC : BlockPipe.Connection.NONE;
    }

    private void toggleSideDisabled(EnumFacing side)
    {
        this.setDisabledSidesMask(this.disabledSidesMask ^ (1 << side.getIndex()));
        this.scheduleCurrentWork(this.getDelay());
    }

    private void setDisabledSidesMask(int mask)
    {
        this.disabledSidesMask = mask & 0x3F;
        this.updateConnectedSides(true);
        this.markDirty();

        this.getWorld().notifyNeighborsOfStateChange(this.getPos(), this.getBlockType(), false);
        this.notifyBlockUpdate(this.getPos());
    }

    private boolean isSideDisabled(EnumFacing side)
    {
        return (this.disabledSidesMask & (1 << side.getIndex())) != 0;
    }

    protected boolean updateConnectedSides(boolean notify)
    {
        int mask = 0;

        for (EnumFacing side : EnumFacing.values())
        {
            if (this.checkCanConnectOnSide(side))
            {
                mask |= (1 << side.getIndex());
            }

            this.updateAllValidOutputSidesForInputSide(side);
        }

        if (mask != this.connectedSidesMask)
        {
            this.connectedSidesMask = mask;

            if (notify)
            {
                this.notifyBlockUpdate(this.getPos());
            }

            return true;
        }

        return false;
    }

    private boolean checkCanConnectOnSide(EnumFacing side)
    {
        if ((this.disabledSidesMask & (1 << side.getIndex())) == 0)
        {
            TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(side));

            if (te instanceof TileEntityPipe)
            {
                return ((TileEntityPipe) te).isSideDisabled(side.getOpposite()) == false;
            }
            else
            {
                return te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            }
        }

        return false;
    }

    protected boolean hasWorkOnSide(int slot)
    {
        return this.validOutputSidesPerSide[slot].length > 0 &&
               this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false;
    }

    private void setScheduledTimesFromDelays()
    {
        int currentTime = (int) (this.getWorld().getTotalWorldTime() & 0x3FFFFFFF);

        // Set the scheduled times on chunk load from the relative delays loaded from NBT
        for (int i = 0; i < this.scheduledTimes.length; i++)
        {
            // This check is to ensure that the delays don't get modified multiple times.
            // Valid delay values are at most 7 bits, thus this check can avoid adding an extra boolean field "initialized".
            if (this.scheduledTimes[i] >= 0 && this.scheduledTimes[i] <= 127)
            {
                this.scheduledTimes[i] += currentTime;
            }
        }
    }

    private void setScheduledTimeForSide(int side, int delay)
    {
        int currentTime = (int) (this.getWorld().getTotalWorldTime() & 0x3FFFFFFF);
        this.scheduledTimes[side] = delay + currentTime;
    }

    /*
    protected void tryPushOutCloggedItems()
    {
        for (int slot = 0; slot < 6; slot++)
        {
            if ((this.cloggedItemsMask & (1 << slot)) != 0 && this.scheduledTimes[slot] < 0 && this.hasWorkOnSide(slot))
            {
                InvResult result = this.tryPushOutItem(this.getWorld(), this.getPos(), slot);

                if (result == InvResult.MOVED_ALL)
                {
                    this.cloggedItemsMask &= ~(1 << slot);
                }

                //if (this.hasWorkOnSide(slot))
                {
                    this.setScheduledTimeForSide(slot, this.delay);
                    this.scheduleBlockUpdate(this.delay, false);
                }
            }
        }
    }
    */

    protected boolean scheduleCurrentWork(int delay)
    {
        int currentTime = (int) (this.getWorld().getTotalWorldTime() & 0x3FFFFFFF);
        int nextSheduledTick = -1;

        for (int slot = 0; slot < 6; slot++)
        {
            if (this.hasWorkOnSide(slot))
            {
                // No previous scheduled update
                if (this.scheduledTimes[slot] < 0 || this.scheduledTimes[slot] < currentTime)
                {
                    this.setScheduledTimeForSide(slot, delay);
                }

                // Get the soonest next scheduled update's time
                if (this.scheduledTimes[slot] >= 0 && (nextSheduledTick < 0 || this.scheduledTimes[slot] < nextSheduledTick))
                {
                    nextSheduledTick = this.scheduledTimes[slot];
                }
            }
        }

        if (nextSheduledTick > currentTime)
        {
            this.scheduleBlockUpdate(nextSheduledTick - currentTime, false);
            return true;
        }

        return false;
    }

    private boolean tryMoveScheduledItems(World world, BlockPos pos)
    {
        int currentTime = (int) (world.getTotalWorldTime() & 0x3FFFFFFF);
        int nextSheduledTick = -1;

        for (int slot = 0; slot < 6; slot++)
        {
            if (this.scheduledTimes[slot] >= 0)
            {
                // This slot's item is ready to be moved out
                if (this.scheduledTimes[slot] <= currentTime)
                {
                    if (this.tryMoveItemsForSide(world, pos, slot))
                    {
                        this.setScheduledTimeForSide(slot, this.getDelay());
                    }
                    else
                    {
                        this.scheduledTimes[slot] = -1;
                    }
                }

                // Get the soonest next scheduled update's time
                if (this.scheduledTimes[slot] >= 0 && (nextSheduledTick < 0 || this.scheduledTimes[slot] < nextSheduledTick))
                {
                    nextSheduledTick = this.scheduledTimes[slot];
                }
            }
        }

        if (nextSheduledTick > currentTime)
        {
            this.scheduleBlockUpdate(nextSheduledTick - currentTime, false);
            return true;
        }

        return false;
    }

    protected boolean tryMoveItemsForSide(World world, BlockPos posSelf, int slot)
    {
        if (this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false)
        {
            InvResult result = this.tryPushOutItem(world, posSelf, slot);

            if (result != InvResult.MOVED_NOTHING)
            {
                world.updateComparatorOutputLevel(posSelf, this.getBlockType());
            }

            // Only schedule a new update if only some items were moved out.
            // If all or none were moved, then there is no need to schedule an update at this point.
            return result == InvResult.MOVED_SOME;
        }
        else
        {
            return false;
        }
    }

    /**
     * ONLY call this method when there are items to move!
     * Otherwise the clogged bit will be set for nothing.
     * @param world
     * @param pos
     * @param slot
     * @return
     */
    protected InvResult tryPushOutItem(World world, BlockPos pos, int slot)
    {
        for (int i = 0; i < this.validOutputSidesPerSide[slot].length; i++)
        {
            EnumFacing outputSide = this.validOutputSidesPerSide[slot][i];
            InvResult result = this.tryPushOutItemsToSide(world, pos, outputSide, slot);

            if (result != InvResult.MOVED_NOTHING)
            {
                return result;
            }
        }

        //this.cloggedItemsMask |= (1 << slot);
        return InvResult.MOVED_NOTHING;
    }

    protected InvResult tryPushOutItemsToSide(World world, BlockPos posSelf, EnumFacing outputSide, int inputSlot)
    {
        TileEntity te = world.getTileEntity(posSelf.offset(outputSide));

        if (te == null)
        {
            return InvResult.MOVED_NOTHING;
        }

        boolean isPipe = te instanceof TileEntityPipe;

        if (isPipe || te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, outputSide.getOpposite()))
        {
            ItemStack stack = this.itemHandlerBase.extractItem(inputSlot, 64, true);
            int sizeOrig = stack.getCount();

            // This is used to prevent scheduling a new update because of an adjacent inventory changing
            // while we push out items, and our own inventory changing due to this extract.
            // The update will be scheduled, if needed, after the push is complete.
            this.disableUpdateScheduling = true;
            this.disableNeighorNotification = true;

            if (isPipe)
            {
                stack = ((TileEntityPipe) te).pushItemIn(PositionUtils.FACING_OPPOSITE_INDICES[outputSide.getIndex()], stack, false);
            }
            else
            {
                IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, outputSide.getOpposite());

                if (inv != null)
                {
                    stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);
                }
            }

            int sizeNew = stack.getCount();
            boolean movedAll = stack.isEmpty();
            boolean movedSome = sizeOrig != sizeNew;

            if (movedAll || movedSome)
            {
                int count = movedAll ? 0 : sizeOrig - sizeNew;
                this.itemHandlerBase.extractItem(inputSlot, sizeOrig - sizeNew, false);

                if (isPipe)
                {
                    this.sendPacketPushToAdjacentPipe(inputSlot, outputSide.getIndex(), ((TileEntityPipe) te).getDelay(), count);
                }
                else
                {
                    this.sendPacketMoveItemOut(inputSlot, outputSide.getIndex(), count);
                }
            }

            /*
            if (movedAll)
            {
                this.cloggedItemsMask &= ~(1 << slot);
            }
            */

            this.disableUpdateScheduling = false;
            this.disableNeighorNotification = false;

            return movedAll ? InvResult.MOVED_ALL : (movedSome ? InvResult.MOVED_SOME : InvResult.MOVED_NOTHING);
        }

        return InvResult.MOVED_NOTHING;
    }

    protected boolean checkHasValidOutputOnSide(EnumFacing side)
    {
        if ((this.disabledSidesMask & (1 << side.getIndex())) == 0)
        {
            TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(side));

            if (te instanceof TileEntityPipe)
            {
                TileEntityPipe tepipe = (TileEntityPipe) te;
                return tepipe.canInputOnSide(side.getOpposite());
            }
            else
            {
                return te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            }
        }

        return false;
    }

    protected boolean checkCanOutputOnSide(EnumFacing side)
    {
        return this.checkHasValidOutputOnSide(side);
    }

    protected boolean canInputOnSide(EnumFacing side)
    {
        return this.isSideDisabled(side) == false;
    }

    protected void updateAllValidOutputSidesForInputSide(final EnumFacing inputSide)
    {
        final List<EnumFacing> sides = new ArrayList<EnumFacing>();
        final EnumFacing oppositeSide = inputSide.getOpposite();

        // Always prefer the "straight through" option, ie. add the opposite side first
        if (this.checkCanOutputOnSide(oppositeSide))
        {
            sides.add(oppositeSide);
        }

        EnumFacing[] possibleSides = PositionUtils.getSidesForAxis(inputSide);

        for (EnumFacing side : possibleSides)
        {
            if (this.checkCanOutputOnSide(side))
            {
                sides.add(side);
            }
        }

        this.validOutputSidesPerSide[inputSide.getIndex()] = sides.toArray(new EnumFacing[sides.size()]);
    }

    @Override
    public void markDirty()
    {
        World world = this.getWorld();

        if (world != null)
        {
            BlockPos pos = this.getPos();
            world.markChunkDirty(pos, this);

            if (this.disableNeighorNotification == false)
            {
                this.updateContainingBlockInfo();
                Block block = this.getBlockType();
                this.getBlockMetadata();

                if (block != Blocks.AIR)
                {
                    world.updateComparatorOutputLevel(pos, block);
                }
            }
        }
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        if (worldIn.isRemote == false)
        {
            this.updateConnectedSides(true);
            this.scheduleCurrentWork(this.getDelay());
        }
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        // When an adjacent tile changes, schedule a new tile tick.
        // Updates will not be scheduled due to the adjacent inventory changing
        // while we are pushing items to it (this.disableUpdateScheduling == true).
        if (this.disableUpdateScheduling == false && this.getWorld().isRemote == false)
        {
            this.scheduleCurrentWork(1);
        }
    }

    @Override
    public boolean onLeftClickBlock(World world, BlockPos pos, EnumFacing side, EntityPlayer player)
    {
        if (player.isSneaking())
        {
            ItemStack stackOffhand = player.getHeldItemOffhand();

            // Sneak + left clicking with an empty main hand, and the same item
            // in the off-hand - apply the placement properties from that item.
            if (player.getHeldItemMainhand().isEmpty() && stackOffhand.isEmpty() == false &&
                stackOffhand.getItem() instanceof ItemBlockAutoverse &&
                ((ItemBlockAutoverse) stackOffhand.getItem()).getBlock() == this.getBlockType())
            {
                if (world.isRemote == false)
                {
                    this.applyPlacementPropertiesFrom(world, pos, player, stackOffhand);
                    player.sendStatusMessage(new TextComponentTranslation("autoverse.chat.placement_properties.applied_from_held_item"), true);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, EnumFacing side, EntityPlayer player, EnumHand hand)
    {
        if (player.isSneaking() && player.getHeldItem(hand).isEmpty())
        {
            if (world.isRemote == false)
            {
                this.toggleSideDisabled(side);
            }

            return true;
        }

        return false;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        this.tryMoveScheduledItems(world, pos);
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        if (this.disableUpdateScheduling == false && this.getWorld().isRemote == false)
        {
            this.onSlotChange(slot, false);
        }

        this.updateComparators();

        ItemStack stack = this.itemHandlerBase.getStackInSlot(slot);

        // Yes that's right, we compare references here, as it should suffice
        if (this.stacksLast.get(slot) != stack)
        {
            this.stacksLast.set(slot, stack);

            if (this.disableNeighorNotification == false)
            {
                if (stack.isEmpty() == false)
                {
                    this.sendPacketInputItem(slot, stack);
                }
                else
                {
                    this.sendPacketRemoveItem(slot);
                }
            }
        }
    }

    private void updateComparators()
    {
        World world = this.getWorld();

        // This is mainly to update Comparators
        for (EnumFacing side : EnumFacing.VALUES)
        {
            // Only bother checking and updating on sides that don't have inventories/connections
            if ((this.connectedSidesMask & (1 << side.getIndex())) == 0)
            {
                BlockPos pos = this.getPos().offset(side);

                if (world.isBlockLoaded(pos))
                {
                    IBlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (block != AutoverseBlocks.PIPE)
                    {
                        block.onNeighborChange(world, pos, this.getPos());

                        /*
                        if (block.isNormalCube(state, world, pos))
                        {
                            pos = pos.offset(side);
                            state = world.getBlockState(pos);

                            if (state.getBlock().getWeakChanges(world, pos))
                            {
                                state.getBlock().onNeighborChange(world, pos, this.getPos());
                            }
                        }
                        */
                    }
                }
            }
        }
    }

    private void onSlotChange(int slot, boolean force)
    {
        if (this.scheduledTimes[slot] < 0 && (force || this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false))
        {
            this.scheduleCurrentWork(this.getDelay());
        }
    }

    private ItemStack pushItemIn(int slot, ItemStack stack, boolean simulate)
    {
        if (simulate == false)
        {
            this.disableNeighorNotification = true;
            stack = this.inputInventories[slot].insertItem(slot, stack, simulate);
            this.disableNeighorNotification = false;
            return stack;
        }
        else
        {
            return this.inputInventories[slot].insertItem(slot, stack, simulate);
        }
    }

    private void sendPacketPushToAdjacentPipe(int inputSide, int outputSide, int delayTarget, int count)
    {
        int val = (1 << 29);
        val |= (count << 22);
        val |= (outputSide << 19);
        val |= (inputSide << 16);
        val |= (delayTarget & 0xFF) << 8;
        val |= (this.getDelay() & 0xFF);

        this.sendPacketToWatchers(new MessageSyncTileEntity(this.getPos(), val), this.getPos());
    }

    private void sendPacketMoveItemOut(int inputSide, int outputSide, int count)
    {
        int val = (2 << 29);
        val |= (count << 22);
        val |= (outputSide << 19);
        val |= (inputSide << 16);
        val |= (this.getDelay() & 0xFF);

        this.sendPacketToWatchers(new MessageSyncTileEntity(this.getPos(), val), this.getPos());
    }

    private void sendPacketRemoveItem(int inputSide)
    {
        int val = (3 << 29);
        val |= (inputSide << 16);

        this.sendPacketToWatchers(new MessageSyncTileEntity(this.getPos(), val ), this.getPos());
    }

    protected void sendPacketInputItem(int inputSide, ItemStack stack)
    {
        int val = 0;
        val |= (inputSide << 16);
        val |= (this.getDelay() & 0xFF);

        this.sendPacketToWatchers(new MessageSyncTileEntity(this.getPos(),
                new int[] { val }, new ItemStack[] { stack } ), this.getPos());
    }

    @Override
    public void syncTile(int[] intValues, ItemStack[] stacks)
    {
        // Input item
        if (intValues.length == 1 && stacks.length == 1)
        {
            int val = intValues[0];
            int slot =  (val >>> 16) & 0x07;
            int delay = (val         & 0xFF);
            this.stacksLast.set(slot, stacks[0]);

            if (stacks[0].isEmpty() == false)
            {
                this.isInput[slot] = 1;
                this.delaysClient[slot] = (byte) delay;
                this.scheduledTimes[slot] = (byte) delay;
            }
            else
            {
                this.isInput[slot] = 0;
                this.delaysClient[intValues[0]] = -2;
            }
        }
        else if (intValues.length == 1 && stacks.length == 0)
        {
            int val = intValues[0];
            int action    = (val >>> 29) & 0x07;
            int count     = (val >>> 22) & 0x7F;
            int sideOut   = (val >>> 19) & 0x07;
            int slot      = (val >>> 16) & 0x07;
            int delayAdj  = (val >>>  8) & 0xFF;
            int delay     = (val         & 0xFF);

            switch (action)
            {
                // Move item to an adjacent pipe
                case 1:
                    TileEntityPipe te = BlockAutoverse.getTileEntitySafely(this.getWorld(),
                            this.getPos().offset(EnumFacing.byIndex(sideOut)), TileEntityPipe.class);

                    if (te != null)
                    {
                        int sideIn = PositionUtils.FACING_OPPOSITE_INDICES[sideOut];

                        // entire stack
                        if (count == 0)
                        {
                            te.stacksLast.set(sideIn, this.stacksLast.get(slot));
                            this.stacksLast.set(slot, ItemStack.EMPTY);
                        }
                        else
                        {
                            te.stacksLast.set(sideIn, this.stacksLast.get(slot).splitStack(count));
                        }

                        te.isInput[sideIn] = 0;
                        te.delaysClient[sideIn] = (byte) delayAdj;
                        te.scheduledTimes[sideIn] = delayAdj;
                        this.delaysClient[slot] = -2;
                    }
                    break;

                // Move input item to output buffer
                case 2:
                    // entire stack
                    if (count == 0)
                    {
                        this.stacksOut.set(slot, this.stacksLast.get(slot));
                        this.stacksLast.set(slot, ItemStack.EMPTY);
                    }
                    else
                    {
                        this.stacksOut.set(slot, this.stacksLast.get(slot).splitStack(count));
                    }

                    this.outputDirections[slot] = (byte) sideOut;
                    this.isInput[slot] = 0;
                    this.delaysClient[slot] = -2;
                    // When directly outputting items, they only move from the center of this block to the edge,
                    // ie. half the distance of normal pipe-to-pipe movement.
                    this.delaysOut[slot] = (byte) (delay / 2);
                    this.scheduledTimes[slot] = (byte) delay;
                    break;

                // Remove a stack
                case 3:
                    this.stacksLast.set(slot, ItemStack.EMPTY);
                    this.isInput[slot] = 0;
                    this.delaysClient[slot] = -2;
                    break;
            }
        }
    }

    public NonNullList<ItemStack> getRenderStacks()
    {
        return this.stacksLast;
    }

    @Override
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        if (action == 0)
        {
            this.setDelay(this.getDelay() + element);
        }
        else if (action == 1)
        {
            this.setMaxStackSize(this.itemHandlerBase.getSlotLimit(0) + element);
        }

        this.markDirty();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return true;
        }
        else
        {
            return super.hasCapability(capability, facing);
        }
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.inputInventories[facing.getIndex()]);
        }
        else
        {
            return super.getCapability(capability, facing);
        }
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound nbt)
    {
        nbt.setByte("sd", (byte) this.connectedSidesMask);

        for (int slot = 0; slot < this.itemHandlerBase.getSlots(); slot++)
        {
            ItemStack stack = this.itemHandlerBase.getStackInSlot(slot);

            if (stack.isEmpty() == false)
            {
                NBTTagCompound tag = stack.writeToNBT(new NBTTagCompound());
                nbt.setTag("st" + slot, tag);
            }
        }

        return nbt;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.connectedSidesMask = tag.getByte("sd");

        for (int slot = 0; slot < this.itemHandlerBase.getSlots(); slot++)
        {
            if (tag.hasKey("st" + slot, Constants.NBT.TAG_COMPOUND))
            {
                this.stacksLast.set(slot, new ItemStack(tag.getCompoundTag("st" + slot)));
            }
        }

        this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.setDelayFromByte(nbt.getByte("Dl"));
        this.disabledSidesMask = nbt.getByte("Dis");
        this.connectedSidesMask = nbt.getByte("Conn");
        //this.cloggedItemsMask = nbt.getByte("Clgg");
        this.setMaxStackSize(nbt.getByte("Max"));

        NBTUtils.readByteArrayIntoIntArray(this.scheduledTimes, nbt, "Sch");
        this.readSideArraysFromNBT(this.validOutputSidesPerSide, nbt, "Vld");
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt = super.writeToNBTCustom(nbt);

        nbt.setByte("Dl", (byte) this.getDelay());
        nbt.setByte("Dis", (byte) this.disabledSidesMask);
        nbt.setByte("Conn", (byte) this.connectedSidesMask);
        nbt.setByte("Max", (byte) this.itemHandlerBase.getSlotLimit(0));
        //nbt.setByte("Clgg", (byte) this.cloggedItemsMask);

        if (this.getWorld() != null)
        {
            int currentTime = (int) (this.getWorld().getTotalWorldTime() & 0x3FFFFFFF);
            byte[] delays = new byte[this.scheduledTimes.length];

            for (int i = 0; i < delays.length; i++)
            {
                if (this.scheduledTimes[i] >= 0)
                {
                    delays[i] = (byte) (this.scheduledTimes[i] - currentTime);
                }
                else
                {
                    delays[i] = (byte) -1;
                }
            }

            nbt.setByteArray("Sch", delays);
        }

        this.writeSideArraysToNBT(this.validOutputSidesPerSide, nbt, "Vld");

        return nbt;
    }

    protected void readSideArraysFromNBT(EnumFacing[][] sideArr, NBTTagCompound nbt, String tagName)
    {
        byte[] sidesMasks = new byte[sideArr.length];
        NBTUtils.readByteArray(sidesMasks, nbt, tagName);

        for (int i = 0; i < sidesMasks.length; i++)
        {
            sideArr[i] = this.createFacingArrayFromMask(i, sidesMasks[i]);
        }
    }

    protected void writeSideArraysToNBT(EnumFacing[][] sideArr, NBTTagCompound nbt, String tagName)
    {
        byte[] sidesMasks = new byte[sideArr.length];

        for (int i = 0; i < sidesMasks.length; i++)
        {
            sidesMasks[i] = (byte) this.createMaskFromFacingArray(sideArr[i]);
        }

        nbt.setByteArray(tagName, sidesMasks);
    }

    private EnumFacing[] createFacingArrayFromMask(int side, int mask)
    {
        EnumFacing opposite = EnumFacing.byIndex(side).getOpposite();
        final List<EnumFacing> sides = new ArrayList<EnumFacing>();

        for (int i = 0, bit = 0x1; i < 6; i++, bit <<= 1)
        {
            if ((mask & bit) != 0)
            {
                EnumFacing facing = EnumFacing.byIndex(i);

                // The "straight through" output side must be the first entry, if it's present
                if (facing == opposite)
                {
                    sides.add(0, facing);
                }
                else
                {
                    sides.add(facing);
                }
            }
        }

        return sides.toArray(new EnumFacing[sides.size()]);
    }

    private int createMaskFromFacingArray(EnumFacing[] sides)
    {
        int mask = 0;

        for (int i = 0; i < sides.length; i++)
        {
            mask |= (1 << sides[i].getIndex());
        }

        return mask;
    }

    private static class InventoryWrapperSides implements IItemHandler
    {
        private final TileEntityPipe te;
        private final IItemHandler baseHandler;
        private final int slot;

        public InventoryWrapperSides(IItemHandler baseHandler, int slot, TileEntityPipe te)
        {
            this.te = te;
            this.baseHandler = baseHandler;
            this.slot = slot;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return this.baseHandler.getSlotLimit(slot);
        }

        @Override
        public int getSlots()
        {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return this.baseHandler.getStackInSlot(this.slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (simulate == false)
            {
                int sizeOrig = stack.getCount();
                stack = this.baseHandler.insertItem(this.slot, stack, simulate);

                if (stack.isEmpty() || stack.getCount() < sizeOrig)
                {
                    this.te.onSlotChange(this.slot, true);
                }

                return stack;
            }
            else
            {
                return this.baseHandler.insertItem(this.slot, stack, simulate);
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ContainerPipe getContainer(EntityPlayer player)
    {
        return new ContainerPipe(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiPipe(this.getContainer(player), this);
    }
}
