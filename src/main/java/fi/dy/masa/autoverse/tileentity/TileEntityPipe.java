package fi.dy.masa.autoverse.tileentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockPipe;
import fi.dy.masa.autoverse.block.BlockPipe.PipePart;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiPipe;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerPipe;
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
    protected int cloggedItemsMask;
    private int delay = 8;
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

    public TileEntityPipe(String name)
    {
        super(name);

        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, 6, 64, false, "Items", this);
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
    public void setPlacementProperties(World world, BlockPos pos, ItemStack stack, NBTTagCompound tag)
    {
        if (tag.hasKey("pipe.delay", Constants.NBT.TAG_INT))
        {
            this.setDelay(tag.getInteger("pipe.delay"));
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
            this.cloggedItemsMask   = PositionUtils.rotateFacingMask(this.cloggedItemsMask, rotation);

            this.rotateSidesPerSideArray(this.validOutputSidesPerSide, rotation);
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

    public void setDelay(int delay)
    {
        this.delay = MathHelper.clamp(delay, 1, 72000);
    }

    public int getDelayForSide(int side)
    {
        return this.scheduledTimes[side];
    }

    /*
    protected void setDelayForSide(int side, int delay)
    {
        this.delaysPerSide[side] = delay;
    }
    */

    public void setMaxStackSize(int maxSize)
    {
        this.itemHandlerBase.setStackLimit(MathHelper.clamp(maxSize, 1, 64));
    }

    public int getConnectedSidesMask()
    {
        return this.connectedSidesMask;
    }

    protected void setConnectedSidesMask(int mask)
    {
        this.connectedSidesMask = mask;
    }

    protected IItemHandler getInputInventory(int side)
    {
        return this.inputInventories[side];
    }

    public BlockPipe.Connection getConnectionType(int sideIndex)
    {
        return (this.connectedSidesMask & (1 << sideIndex)) != 0 ? BlockPipe.Connection.BASIC : BlockPipe.Connection.NONE;
    }

    private void toggleSideDisabled(EnumFacing side)
    {
        this.setDisabledSidesMask(this.disabledSidesMask ^ (1 << side.getIndex()));
        this.scheduleCurrentWork(this.delay);
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

    public boolean updateConnectedSides(boolean notify)
    {
        int mask = 0;

        for (EnumFacing side : EnumFacing.values())
        {
            if (this.checkCanConnectOnSide(side))
            {
                mask |= (1 << side.getIndex());
            }

            this.updateAllValidOutputSidesForInputSide(side);
            this.updateInputInventoryForSide(side);
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

    protected boolean checkCanConnectOnSide(EnumFacing side)
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
            //System.out.printf("%d - setScheduledTimesFromDelays() @ %s, side: %d, sched for: %d\n", currentTime, this.getPos(), i, this.scheduledTimes[i]);
            //byte delay = (byte) this.scheduledTimes[i];

            //if (delay >= 0)
            // This check is to ensure that the delays don't get modified multiple times.
            // Valid delay values are at most 7 bits, thus this check can avoid adding an extra boolean field "initialized".
            if (this.scheduledTimes[i] >= 0 && this.scheduledTimes[i] <= 127)
            {
                //System.out.printf("%d - setScheduledTimesFromDelays() @ %s, side: %d, ADJ sched for: %d\n", currentTime, this.getPos(), i, currentTime + this.scheduledTimes[i]);
                //this.scheduledTimes[i] = currentTime + delay;
                this.scheduledTimes[i] += currentTime;
            }
        }
    }

    protected void setScheduledTimeForSide(int side, int delay)
    {
        int currentTime = (int) (this.getWorld().getTotalWorldTime() & 0x3FFFFFFF);
        this.scheduledTimes[side] = delay + currentTime;
    }

    protected void tryPushOutCloggedItems()
    {
        for (int slot = 0; slot < 6; slot++)
        {
            if ((this.cloggedItemsMask & (1 << slot)) != 0 && this.scheduledTimes[slot] < 0 && this.hasWorkOnSide(slot))
            {
                //System.out.printf("tryPushOutCloggedItems() @ %s, slot: %d\n", this.getPos(), slot);
                InvResult result = this.tryPushOutItem(this.getWorld(), this.getPos(), slot);
                //System.out.printf("tryPushOutCloggedItems() @ %s, slot: %d, result: %s\n", this.getPos(), slot, result);

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

    protected boolean scheduleCurrentWork(int delay)
    {
        int currentTime = (int) (this.getWorld().getTotalWorldTime() & 0x3FFFFFFF);
        int nextSheduledTick = -1;
        //if (pos.equals(new BlockPos(1308, 65, 1268)))
        //System.out.printf("%d - scheduleCurrentWork() @ %s - start\n", currentTime, this.getPos());

        for (int slot = 0; slot < 6; slot++)
        {
            if (this.hasWorkOnSide(slot))
            {
                //if (pos.equals(new BlockPos(1308, 65, 1268)))
                //System.out.printf("%d - scheduleCurrentWork() @ %s, HAS WORK, slot: %d, old sched time: %d\n", currentTime, this.getPos(), slot, this.scheduledTimes[slot]);
                // No previous scheduled update
                if (this.scheduledTimes[slot] < 0 || this.scheduledTimes[slot] < currentTime)
                {
                    this.setScheduledTimeForSide(slot, delay);
                    //if (pos.equals(new BlockPos(1308, 65, 1268)))
                    //System.out.printf("%d - scheduleCurrentWork() @ %s - slot: %d, new sched time: %d\n", currentTime, this.getPos(), slot, this.scheduledTimes[slot]);
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
            //if (pos.equals(new BlockPos(1308, 65, 1268)))
            //System.out.printf("%d - scheduleCurrentWork() @ %s, sched for: %d (remaining delay: %d)\n", currentTime, this.getPos(), nextSheduledTick, nextSheduledTick - currentTime);
            //this.reScheduleUpdateIfSooner(nextSheduledTick - currentTime);
            this.scheduleBlockUpdate(nextSheduledTick - currentTime, false);
            return true;
        }

        return false;
    }

    private boolean tryMoveScheduledItems(World world, BlockPos pos)
    {
        int currentTime = (int) (world.getTotalWorldTime() & 0x3FFFFFFF);
        int nextSheduledTick = -1;
        //if (pos.equals(new BlockPos(1308, 65, 1268)))
        //System.out.printf("%d - tryMoveScheduledItems() @ %s - start\n", currentTime, this.getPos());

        for (int slot = 0; slot < 6; slot++)
        {
            if (this.scheduledTimes[slot] >= 0)
            {
                //if (pos.equals(new BlockPos(1308, 65, 1268))) System.out.printf("%d - tryMoveScheduledItems() @ %s - slot: %d, sched for: %d\n", currentTime, this.getPos(), slot, this.scheduledTimes[slot]);
                // This slot's item is ready to be moved out
                if (this.scheduledTimes[slot] <= currentTime)
                {
                    //if (pos.equals(new BlockPos(1308, 65, 1268))) System.out.printf("%d - tryMoveScheduledItems() @ %s - slot: %d, sched for: %d - NOW\n", currentTime, this.getPos(), slot, this.scheduledTimes[slot]);
                    if (this.tryMoveItemsForSide(world, pos, slot))
                    {
                        //if (pos.equals(new BlockPos(1308, 65, 1268))) System.out.printf("%d - tryMoveScheduledItems() @ %s - slot: %d, MOVED\n", currentTime, this.getPos(), slot);
                        this.setScheduledTimeForSide(slot, this.delay);
                        //System.out.printf("tryMoveScheduledItems(): pos: %s, slot: %d - SUCCESS, MORE WORK, new time: %d\n", pos, slot, this.scheduledTimes[slot]);
                    }
                    else
                    {
                        //if (pos.equals(new BlockPos(1308, 65, 1268))) System.out.printf("%d - tryMoveScheduledItems() @ %s - slot: %d, FAILED\n", currentTime, this.getPos(), slot);
                        //System.out.printf("tryMoveScheduledItems(): pos: %s, slot: %d - FAIL or ALL DONE\n", pos, slot);
                        this.scheduledTimes[slot] = -1;
                    }
                }
                // Not ready to be moved out yet
                else
                {
                    //if (pos.equals(new BlockPos(1308, 65, 1268))) System.out.printf("%d - tryMoveScheduledItems() @ %s - slot: %d, sched for: %d - NOT YET\n", currentTime, this.getPos(), slot, this.scheduledTimes[slot]);
                    //System.out.printf("tryMoveScheduledItems(): pos: %s, slot: %d - NOT YET\n", pos, slot);
                    // Update the time remaining in the delay
                    //this.delaysPerSide[slot] -= elapsedTime;
                    //delay = this.delaysPerSide[slot];
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
            //System.out.printf("tryMoveScheduledItems(): pos: %s - sched for: %d (remaining delay: %d)\n", pos, nextSheduledTick, nextSheduledTick - currentTime);
            //this.reScheduleUpdateIfSooner(nextSheduledTick - currentTime);
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
                //if (posSelf.equals(new BlockPos(1308, 64, 1268)))
                //System.out.printf("%d - tryMoveItemsForSide(): pos: %s, slot: %d - PUSHED\n", world.getTotalWorldTime(), posSelf, slot);
                // Notify only the neighbor on the side for which the items were moved out from.
                //this.notifyNeighborOnSide(world, posSelf, EnumFacing.getFront(slot));
                world.updateComparatorOutputLevel(posSelf, this.getBlockType());
            }

            // Only schedule a new update if only some items were moved out.
            // If all or none were moved, then there is no need to schedule an update at this point.
            return result == InvResult.MOVED_SOME;
            //return this.tryPushOutItem(world, pos, slot) != InvResult.MOVED_NOTHING;
        }
        else
        {
            //System.out.printf("tryMoveItemsForSide(): pos: %s, slot: %d - FAILED PUSH\n", pos, slot);
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
        //System.out.printf("BASIC tryPushOutItem(): pos: %s, slot: %d, valid sides: %d\n", pos, slot, this.validOutputSidesPerSide[slot].length);
        for (int i = 0; i < this.validOutputSidesPerSide[slot].length; i++)
        {
            EnumFacing outputSide = this.validOutputSidesPerSide[slot][i];
            InvResult result = this.tryPushOutItemsToSide(world, pos, outputSide, slot);

            if (result != InvResult.MOVED_NOTHING)
            {
                //System.out.printf("BASIC tryPushOutItem(): pos: %s, side: %s - SUCCESS\n", pos, outputSide);
                return result;
            }
            else
            {
                //System.out.printf("BASIC tryPushOutItem(): pos: %s, side: %s - FAIL\n", pos, outputSide);
            }
        }

        //System.out.printf("tryPushOutItem(): CLOGGED @ %s, item: %s\n", pos, this.itemHandlerBase.getStackInSlot(slot));
        this.cloggedItemsMask |= (1 << slot);
        return InvResult.MOVED_NOTHING;
    }

    protected InvResult tryPushOutItemsToSide(World world, BlockPos posSelf, EnumFacing side, int slot)
    {
        TileEntity te = world.getTileEntity(posSelf.offset(side));

        if (te == null)
        {
            return InvResult.MOVED_NOTHING;
        }

        boolean isPipe = te instanceof TileEntityPipe;

        if (isPipe || te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite()))
        {
            //System.out.printf("tryPushOutItemsToSide(): pos: %s, slot: %d pushing tp pipe on side: %s\n", posSelf, slot, side);
            ItemStack stack = this.itemHandlerBase.extractItem(slot, 64, true);
            int sizeOrig = stack.getCount();

            // This is used to prevent scheduling a new update because of an adjacent inventory changing
            // while we push out items, and our own inventory changing due to this extract.
            // The update will be scheduled, if needed, after the push is complete.
            this.disableUpdateScheduling = true;
            this.disableNeighorNotification = true;

            if (isPipe)
            {
                stack = ((TileEntityPipe) te).pushItemIn(PositionUtils.FACING_OPPOSITE_INDICES[side.getIndex()], stack, false);
            }
            else
            {
                IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());

                if (inv != null)
                {
                    stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);
                }
            }

            boolean movedAll = stack.isEmpty();
            boolean movedSome = stack.getCount() != sizeOrig;

            if (movedAll || movedSome)
            {
                this.itemHandlerBase.extractItem(slot, sizeOrig - stack.getCount(), false);

                if (isPipe)
                {
                    this.sendPacketPushToAdjacentPipe(slot, side.getIndex(), ((TileEntityPipe) te).getDelay());
                }
                else
                {
                    this.sendPacketMoveItemOut(slot, side.getIndex());
                }
            }

            if (movedAll)
            {
                this.cloggedItemsMask &= ~(1 << slot);
            }

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

    private void updateInputInventoryForSide(EnumFacing side)
    {
        int slot = side.getIndex();

        if ((this.disabledSidesMask & (1 << side.getIndex())) == 0)
        {
            TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(side));

            if ((te instanceof TileEntityPipe) ||
                (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())))
            {
                this.createInputInventoryIfNull(slot);
                return;
            }
        }

        this.inputInventories[slot] = null;
    }

    protected void createInputInventoryIfNull(int slot)
    {
        if (this.inputInventories[slot] == null)
        {
            this.inputInventories[slot] = new InventoryWrapperSides(this.itemHandlerBase, slot, this);
        }
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
        //System.out.printf("updateAllValidOutputSidesForInputSide(): pos: %s, inputSide: %s - valid out: %s\n", this.getPos(), inputSide, Joiner.on(", ").join(this.validOutputSidesPerSide[inputSide.getIndex()]));
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

    /*
    protected void notifyNeighborOnSide(World world, BlockPos posSelf, EnumFacing side)
    {
        // This is the method used for comparator changes, and it comes back to
        // Autoverse TileEntities as onNeighborTileChange().
        BlockPos posNeighbor = posSelf.offset(side);
        world.getBlockState(posNeighbor).getBlock().onNeighborChange(world, posNeighbor, posSelf);
    }
    */

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        if (worldIn.isRemote == false)
        {
            this.updateConnectedSides(true);
            this.scheduleCurrentWork(this.delay);
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
            /*
            // When the pipe is clogged, try to push out the clogged items immediately
            // on neighbor tile change, instead of scheduling an update
            if (this.cloggedItemsMask != 0)
            {
                //System.out.printf("onNeighborTileChange(), pos: %s - CLOGGED\n", this.getPos());
                this.tryPushOutCloggedItems();
            }
            else
            {
                //System.out.printf("onNeighborTileChange(), pos: %s - NOT clogged\n", this.getPos());
                //this.onNeighborInventoryChange();
                // TODO this is only needed for the extraction pipe?
                //this.reScheduleStuckItems();
                //this.scheduleBlockUpdate(this.delay, false);
            }
            */

            //if (pos.equals(new BlockPos(1308, 65, 1268)))
            //System.out.printf("%d - onNeighborTileChange(), pos: %s - scheduling\n", this.getWorld().getTotalWorldTime(), this.getPos());
            // Schedule an update with minimal delay, if work becomes possible by a neighbor inventory freeing
            this.scheduleCurrentWork(1);
            //this.onNeighborInventoryChange();
        }
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking() && player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.STICK)
        {
            if (world.isRemote == false)
            {
                long curr = world.getTotalWorldTime();
                String sched = "" + this.scheduledTimes[0]; for (int i = 1; i < 6; i++) { sched += "," + this.scheduledTimes[i]; }
                System.out.printf("%d - pos: %s - clogged: 0x%02X, sched: %s\n", curr, pos, this.cloggedItemsMask, sched);
            }
            return true;
        }
        if (player.isSneaking() && player.getHeldItem(EnumHand.MAIN_HAND).isEmpty())
        {
            if (world.isRemote == false)
            {
                EnumFacing targetSide = this.getActionTargetSide(world, pos, state, side, player);
                this.toggleSideDisabled(targetSide);
            }

            return true;
        }

        return false;
    }

    protected EnumFacing getActionTargetSide(World world, BlockPos pos, IBlockState state, EnumFacing side, EntityPlayer player)
    {
        if (state.getBlock() instanceof BlockPipe)
        {
            Pair<PipePart, EnumFacing> key = BlockPipe.getPointedElementId(world, pos, (BlockPipe) state.getBlock(), null, player);

            // Targeting one of the side connections
            if (key != null && key.getLeft() == PipePart.SIDE)
            {
                side = key.getRight();
            }
            //else: Targeting the middle part
        }

        return side;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        //System.out.printf("%d - BASIC onScheduledBlockUpdate(): pos: %s - START\n", world.getTotalWorldTime(), pos);
        if (this.tryMoveScheduledItems(world, pos))
        {
            //System.out.printf("BASIC onScheduledBlockUpdate(): pos: %s - SUCCESS\n", pos);
            //this.scheduleBlockUpdate(this.delay, false);
        }
        else
        {
            //System.out.printf("BASIC onScheduledBlockUpdate(): pos: %s - FAIL\n", pos);
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        if (this.disableUpdateScheduling == false && this.getWorld().isRemote == false)
        {
            this.onSlotChange(slot, false);
        }

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

    private void onSlotChange(int slot, boolean force)
    {
        if (this.scheduledTimes[slot] < 0 && (force || this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false))
        {
            //System.out.printf("BASIC onSlotChange() @ %s: slot/side: %d - %s - SCHED\n", this.getPos(), slot, EnumFacing.getFront(slot));
            //this.setScheduledTimeForSide(slot, this.delay);
            //this.scheduleBlockUpdate(this.delay, false);
            this.scheduleCurrentWork(this.delay);
        }
        else
        {
            //System.out.printf("BASIC onSlotChange() @ %s: slot/side: %d - %s - NOPE, time: %d\n", this.getPos(), slot, EnumFacing.getFront(slot), this.scheduledTimes[slot]);
        }
    }

    protected ItemStack pushItemIn(int slot, ItemStack stack, boolean simulate)
    {
        if (this.inputInventories[slot] == null)
        {
            return stack;
        }

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

    protected void sendPacketPushToAdjacentPipe(int inputSide, int outputSide, int delayTarget)
    {
        int val = (1 << 28);
        val |= (outputSide << 20);
        val |= (inputSide << 16);
        val |= (delayTarget & 0xFF) << 8;
        val |= (this.delay & 0xFF);

        this.sendPacketToWatchers(new MessageSyncTileEntity(this.getPos(), val), this.getPos());
    }

    protected void sendPacketMoveItemOut(int inputSide, int outputSide)
    {
        int val = (2 << 28);
        val |= (outputSide << 20);
        val |= (inputSide << 16);
        val |= (this.delay & 0xFF);

        this.sendPacketToWatchers(new MessageSyncTileEntity(this.getPos(), val), this.getPos());
    }

    protected void sendPacketRemoveItem(int inputSide)
    {
        int val = (3 << 28);
        val |= (inputSide << 16);

        this.sendPacketToWatchers(new MessageSyncTileEntity(this.getPos(), val ), this.getPos());
    }

    protected void sendPacketInputItem(int inputSide, ItemStack stack)
    {
        int val = 0;
        val |= (inputSide << 16);
        val |= (this.delay & 0xFF);

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
            int slot =  (val >>> 16) & 0x0F;
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
            int action =    (val >>> 28) & 0x0F;
            int sideOut =   (val >>> 20) & 0x0F;
            int slot =      (val >>> 16) & 0x0F;
            int delayAdj =  (val >>>  8) & 0xFF;
            int delay =     (val         & 0xFF);

            switch (action)
            {
                // Move item to an adjacent pipe
                case 1:
                    TileEntityPipe te = BlockAutoverse.getTileEntitySafely(this.getWorld(),
                            this.getPos().offset(EnumFacing.getFront(sideOut)), TileEntityPipe.class);

                    if (te != null)
                    {
                        int sideIn = PositionUtils.FACING_OPPOSITE_INDICES[sideOut];
                        te.stacksLast.set(sideIn, this.stacksLast.get(slot));
                        te.isInput[sideIn] = 0;
                        te.delaysClient[sideIn] = (byte) delayAdj;
                        te.scheduledTimes[sideIn] = delayAdj;
                        this.stacksLast.set(slot, ItemStack.EMPTY);
                        this.delaysClient[slot] = -2;
                    }
                    break;

                // Move input item to output buffer
                case 2:
                    this.stacksOut.set(slot, this.stacksLast.get(slot));
                    this.stacksLast.set(slot, ItemStack.EMPTY);
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
            this.setDelay(this.delay + element);
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
        if (facing != null &&
            capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY &&
            this.inputInventories[facing.getIndex()] != null)
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

        this.delay = nbt.getByte("Dl");
        this.disabledSidesMask = nbt.getByte("Dis");
        this.connectedSidesMask = nbt.getByte("Conn");
        this.cloggedItemsMask = nbt.getByte("Clgg");
        this.setMaxStackSize(nbt.getByte("Max"));

        NBTUtils.readByteArrayIntoIntArray(this.scheduledTimes, nbt, "Sch");
        this.readSideArraysFromNBT(this.validOutputSidesPerSide, nbt, "Vld");
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt = super.writeToNBTCustom(nbt);

        nbt.setByte("Dl", (byte) this.delay);
        nbt.setByte("Dis", (byte) this.disabledSidesMask);
        nbt.setByte("Conn", (byte) this.connectedSidesMask);
        nbt.setByte("Max", (byte) this.itemHandlerBase.getSlotLimit(0));
        nbt.setByte("Clgg", (byte) this.cloggedItemsMask);

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

    protected EnumFacing[] createFacingArrayFromMask(int side, int mask)
    {
        EnumFacing opposite = EnumFacing.getFront(side).getOpposite();
        final List<EnumFacing> sides = new ArrayList<EnumFacing>();

        for (int i = 0, bit = 0x1; i < 6; i++, bit <<= 1)
        {
            if ((mask & bit) != 0)
            {
                EnumFacing facing = EnumFacing.getFront(i);

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

    protected int createMaskFromFacingArray(EnumFacing[] sides)
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
