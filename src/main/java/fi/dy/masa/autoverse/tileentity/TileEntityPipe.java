package fi.dy.masa.autoverse.tileentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.base.Joiner;
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
import fi.dy.masa.autoverse.gui.client.GuiPipe;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerPipe;
import fi.dy.masa.autoverse.network.message.ISyncableTile;
import fi.dy.masa.autoverse.network.message.MessageSyncTileEntity;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.NBTUtils;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntityPipe extends TileEntityAutoverseInventory implements ISyncableTile
{
    private final IItemHandler[] inputInventories;
    private final EnumFacing[][] validOutputSidesPerSide;
    private final byte outputSideIndices[] = new byte[6];
    private final int delaysPerSide[] = new int[6];
    private int connectedSidesMask;
    private int disabledSidesMask;
    private int cloggedItemsMask;
    private int delay = 8;
    private long lastDelayUpdate;
    protected boolean disableUpdateScheduling;
    protected boolean disableNeighorNotification;

    // Client-side or client sync/rendering related things
    private final NonNullList<ItemStack> stacksLast = NonNullList.withSize(6, ItemStack.EMPTY);
    public int delaysClient[] = new int[6];
    public float partialTicksLast;

    public TileEntityPipe()
    {
        this(ReferenceNames.NAME_BLOCK_PIPE);
    }

    public TileEntityPipe(String name)
    {
        super(name);

        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, 6, 64, false, "Items", this);
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

        Arrays.fill(this.delaysPerSide, -1);
        Arrays.fill(this.delaysClient, -1);
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
        return this.delaysPerSide[side];
    }

    protected void setDelayForSide(int side, int delay)
    {
        this.delaysPerSide[side] = delay;
    }

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
        /*
        this.disabledSidesMask ^= (1 << side.getIndex());
        this.updateConnectedSides(true);
        this.markDirty();

        this.getWorld().neighborChanged(this.getPos().offset(side), this.getBlockType(), this.getPos());
        this.notifyBlockUpdate(this.getPos());
        */
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

    private boolean tryMoveScheduledItems(World world, BlockPos pos)
    {
        long currentTime = world.getTotalWorldTime();
        // Time elapsed since last check
        int elapsedTime = (int) (currentTime - this.lastDelayUpdate);
        int nextSheduledTick = -1;

        for (int slot = 0; slot < 6; slot++)
        {
            int delay = this.delaysPerSide[slot];

            //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s, slot: %d - initial delay: %d\n", pos, slot, this.delaysPerSide[slot]);
            if (delay >= 0)
            {
                // This slot's item is ready to be moved out
                if (delay <= elapsedTime)
                {
                    delay = this.tryMoveItemsForSide(world, pos, slot) ? this.delay : -1;
                    this.delaysPerSide[slot] = delay;
                    //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s, slot: %d - NOW, newDelay: %d\n", pos, slot, delay);

                    if (delay < 0)
                    {
                        //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s, slot: %d - NOW -> FAIL\n", pos, slot);
                        //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s, slot: %d - final delay: %d\n", pos, slot, this.delaysPerSide[slot]);
                    }
                }
                // Not ready to be moved out yet
                else
                {
                    //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s, slot: %d - NOT YET\n", pos, slot);
                    // Update the time remaining in the delay
                    this.delaysPerSide[slot] -= elapsedTime;
                    delay = this.delaysPerSide[slot];
                }

                // Get the soonest next scheduled update's delay
                if (delay >= 0 && (nextSheduledTick < 0 || delay < nextSheduledTick))
                {
                    nextSheduledTick = delay;
                }
            }
            //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s, slot: %d - final delay: %d\n", pos, slot, this.delaysPerSide[slot]);
        }

        this.lastDelayUpdate = currentTime;

        if (nextSheduledTick > 0)
        {
            //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s - SCHED @ %d\n", pos, nextSheduledTick);
            this.scheduleBlockUpdate(nextSheduledTick, false);
            return true;
        }

        return false;
    }

    protected boolean tryMoveItemsForSide(World world, BlockPos pos, int slot)
    {
        if (this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false && this.tryPushOutItem(world, pos, slot))
        {
            //System.out.printf("BASIC tryMoveItemsForSide(): pos: %s, slot: %d - PUSHED\n", pos, slot);
            /*
            if (this.itemHandlerBase.getStackInSlot(slot).isEmpty())
            {
                return -1;
            }
            else
            {
                return this.delay;
            }
            */
            return true;
        }
        else
        {
            //System.out.printf("BASIC tryMoveItemsForSide(): pos: %s, slot: %d - FAILED PUSH\n", pos, slot);
            return false;
        }
    }

    private boolean tryPushOutItem(World world, BlockPos pos, int slot)
    {
        //System.out.printf("BASIC tryPushOutItem(): pos: %s, slot: %d, valid sides: %d\n", pos, slot, this.validOutputSidesPerSide[slot].length);
        for (int i = 0; i < this.validOutputSidesPerSide[slot].length; i++)
        {
            EnumFacing outputSide = this.validOutputSidesPerSide[slot][i];

            if (this.tryPushOutItemsToSide(world, pos, outputSide, slot))
            {
                //System.out.printf("BASIC tryPushOutItem(): pos: %s, side: %s - SUCCESS\n", pos, outputSide);
                return true;
            }
            else
            {
                //System.out.printf("BASIC tryPushOutItem(): pos: %s, side: %s - FAIL\n", pos, outputSide);
            }
        }

        return false;
    }

    private boolean tryPushOutItemsToSide(World world, BlockPos posSelf, EnumFacing side, int slot)
    {
        TileEntity te = world.getTileEntity(posSelf.offset(side));

        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite()))
        {
            IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());

            if (inv != null)
            {
                //System.out.printf("BASIC tryPushOutItemsToSide(): pos: %s, slot: %d pushing to side: %s\n", posSelf, slot, side);
                ItemStack stack = this.itemHandlerBase.extractItem(slot, 64, true);
                int sizeOrig = stack.getCount();
                boolean movedSome = false;

                stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, true);

                if (stack.isEmpty() || stack.getCount() != sizeOrig)
                {
                    // This is used to prevent scheduling a new update because of an adjacent inventory changing
                    // while we push out items, and our own inventory changing due to this extract.
                    // The update will be scheduled, if needed, after the push is complete.
                    this.disableUpdateScheduling = true;
                    this.disableNeighorNotification = true;

                    stack = this.itemHandlerBase.extractItem(slot, 64, false);
                    sizeOrig = stack.getCount();
                    stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);
                }
                else
                {
                    this.cloggedItemsMask |= (1 << slot);
                    //System.out.printf("BASIC tryPushOutItemsToSide(): pos: %s, slot: %d pushing to side: %s - FAILED SIM\n", posSelf, slot, side);
                    return false;
                }

                // Moved entire stack, notify neighbors only in this case
                if (stack.isEmpty())
                {
                    this.cloggedItemsMask &= ~(1 << slot);
                    world.updateComparatorOutputLevel(posSelf, this.getBlockType());
                }
                // Return the items that couldn't be moved
                else
                {
                    //System.out.printf("BASIC tryPushOutItemsToSide(): pos: %s, slot: %d pushed to side: %s but not all\n", posSelf, slot, side);
                    movedSome = stack.getCount() != sizeOrig;
                    this.itemHandlerBase.insertItem(slot, stack, false);
                }

                this.disableUpdateScheduling = false;
                this.disableNeighorNotification = false;
                //System.out.printf("BASIC tryPushOutItemsToSide(): pos: %s, slot: %d pushed to side: %s - isEmpty: %s, movedSome: %s\n", posSelf, slot, side, stack.isEmpty(), movedSome);

                return stack.isEmpty() || movedSome;
            }
        }

        return false;
    }

    protected boolean scheduleCurrentWork()
    {
        int nextSheduledTick = -1;

        for (int slot = 0; slot < 6; slot++)
        {
            if (this.hasWorkOnSide(slot))
            {
                int delay = this.delaysPerSide[slot];

                System.out.printf("re-schedule stuck items @ %s, slot: %d, delay: %d\n", this.getPos(), slot, delay);
                if (delay < 0)
                {
                    delay = this.delay;
                    this.delaysPerSide[slot] = delay;
                }

                // Get the soonest next scheduled update's delay
                if (delay >= 0 && (nextSheduledTick < 0 || delay < nextSheduledTick))
                {
                    nextSheduledTick = delay;
                }
            }
        }

        if (nextSheduledTick >= 0)
        {
            System.out.printf("re-scheduling stuck items @ %s for %d\n", this.getPos(), nextSheduledTick);
            this.scheduleBlockUpdate(nextSheduledTick, false);
            return true;
        }

        return false;
    }

    protected void tryPushOutCloggedItems()
    {
        for (int slot = 0; slot < 6; slot++)
        {
            if (this.delaysPerSide[slot] < 0 &&
                this.hasWorkOnSide(slot) &&
                this.tryPushOutItem(this.getWorld(), this.getPos(), slot))
            {
                this.cloggedItemsMask &= ~(1 << slot);

                if (this.hasWorkOnSide(slot))
                {
                    this.delaysPerSide[slot] = this.delay;
                    this.scheduleBlockUpdate(this.delay, false);
                }
            }
        }
    }

    protected boolean hasWorkOnSide(int slot)
    {
        return this.shouldOperate() &&
               this.validOutputSidesPerSide[slot].length > 0 &&
               this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false;
    }

    /*
    @Nullable
    private EnumFacing getOutputSideForInputSide(int slot)
    {
        if (this.outputSideIndices[slot] < this.validOutputSidesPerSide[slot].length)
        {
            //System.out.printf("getOutputSideForInputSide(): pos: %s, slot: %d, index: %d - valid out: %s\n", this.getPos(), slot, this.outputSideIndices[slot], Joiner.on(", ").join(this.validOutputSidesPerSide[slot]));
            return this.validOutputSidesPerSide[slot][this.outputSideIndices[slot]];
        }

        return null;
    }

    @Nullable
    private EnumFacing cycleOutputSideForInputSide(int slot)
    {
        if (++this.outputSideIndices[slot] >= this.validOutputSidesPerSide[slot].length)
        {
            this.outputSideIndices[slot] = 0;
        }

        return this.getOutputSideForInputSide(slot);
    }
    */

    protected boolean checkCanOutputOnSide(EnumFacing side)
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

    private void updateAllValidOutputSidesForInputSide(final EnumFacing inputSide)
    {
        final List<EnumFacing> sides = new ArrayList<EnumFacing>();
        final EnumFacing oppositeSide = inputSide.getOpposite();

        if (this.checkCanOutputOnSide(oppositeSide))
        {
            sides.add(oppositeSide);
        }

        EnumFacing[] possibleSides;

        switch (inputSide)
        {
            case UP:
            case DOWN:
                possibleSides = PositionUtils.SIDES_Y;
                break;

            case NORTH:
            case SOUTH:
                possibleSides = PositionUtils.SIDES_Z;
                break;

            default:
                possibleSides = PositionUtils.SIDES_X;
        }

        for (EnumFacing side : possibleSides)
        {
            if (this.checkCanOutputOnSide(side))
            {
                sides.add(side);
            }
        }

        this.validOutputSidesPerSide[inputSide.getIndex()] = sides.toArray(new EnumFacing[sides.size()]);
        System.out.printf("updateAllValidOutputSidesForInputSide(): pos: %s, inputSide: %s - valid out: %s\n", this.getPos(), inputSide, Joiner.on(", ").join(this.validOutputSidesPerSide[inputSide.getIndex()]));
    }

    @Override
    public void onLoad()
    {
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

    protected boolean shouldOperate()
    {
        // TODO
        return this.redstoneState == false;
    }

    protected void onNeighborInventoryChange()
    {
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        if (worldIn.isRemote == false)
        {
            this.updateConnectedSides(true);
            this.scheduleCurrentWork();
        }
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        // When an adjacent tile changes, schedule a new tile tick.
        // Updates will not be scheduled due to the adjacent inventory changing
        // while we are pushing items to it (this.disableUpdateScheduling == true).
        if (this.disableUpdateScheduling == false && this.getWorld().isRemote == false && this.shouldOperate())
        {
            // When the pipe is clogged, try to push out the clogged items immediately
            // on neighbor tile change, instead of scheduling an update
            if (this.cloggedItemsMask != 0)
            {
                this.tryPushOutCloggedItems();
            }
            else
            {
                this.onNeighborInventoryChange();
                // TODO this is only needed for the extraction pipe?
                //this.reScheduleStuckItems();
                //System.out.printf("onNeighborTileChange(), scheduling(?) for %s\n", this.getPos());
                //this.scheduleBlockUpdate(this.delay, false);
            }
        }
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        if (world.isRemote == false)
        {
            for (int i = 0; i < this.validOutputSidesPerSide.length; i++)
            {
                System.out.printf("pos: %s, i: %d, valid out: %s\n", this.getPos(), i, Joiner.on(", ").join(this.validOutputSidesPerSide[i]));
            }
        }
        if (player.isSneaking() && player.getHeldItem(EnumHand.MAIN_HAND).isEmpty())
        {
            if (world.isRemote == false)
            {
                EnumFacing targetSide = this.getActionTargetSide(world, pos, state, side, player);
                this.toggleSideDisabled(targetSide);
                this.scheduleCurrentWork();
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
        //System.out.printf("*** BASIC onScheduledBlockUpdate(): pos: %s - START\n", pos);
        if (this.tryMoveScheduledItems(world, pos))
        {
            //System.out.printf("BASIC onScheduledBlockUpdate(): pos: %s - SUCCESS\n", pos);
            this.scheduleBlockUpdate(this.delay, false);
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
            this.sendPacketToWatchers(new MessageSyncTileEntity(this.getPos(),
                    new int[] { slot, this.delay }, new ItemStack[] { stack }));
        }
    }

    private void onSlotChange(int slot, boolean force)
    {
        if (this.delaysPerSide[slot] < 0 && (force || this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false))
        {
            //System.out.printf("BASIC onSlotChange(): slot/side: %d - %s - SCHED\n", slot, EnumFacing.getFront(slot));
            this.delaysPerSide[slot] = this.delay;
            this.scheduleBlockUpdate(this.delay, false);
        }
        else
        {
            //System.out.printf("BASIC onSlotChange(): slot/side: %d - %s - NOPE\n", slot, EnumFacing.getFront(slot));
        }
    }

    @Override
    public void syncTile(int[] intValues, ItemStack[] stacks)
    {
        if (intValues.length == 2 && stacks.length == 1)
        {
            this.stacksLast.set(intValues[0], stacks[0]);

            if (stacks[0].isEmpty() == false)
            {
                this.delaysPerSide[intValues[0]] = intValues[1];
                this.delaysClient[intValues[0]] = intValues[1];
            }
            else
            {
                this.delaysPerSide[intValues[0]] = -2;
                this.delaysClient[intValues[0]] = -2;
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
        this.lastDelayUpdate = nbt.getLong("Last");
        this.cloggedItemsMask = nbt.getByte("Clgg");
        this.setMaxStackSize(nbt.getByte("Max"));

        NBTUtils.readByteArrayIntoIntArray(this.delaysPerSide, nbt, "Dls");
        NBTUtils.readByteArray(this.outputSideIndices, nbt, "Ind");

        byte[] validSidesMasks = new byte[this.validOutputSidesPerSide.length];
        NBTUtils.readByteArray(validSidesMasks, nbt, "Vld");

        for (int i = 0; i < validSidesMasks.length; i++)
        {
            this.validOutputSidesPerSide[i] = this.createFacingArrayFromMask(i, validSidesMasks[i]);
        }
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt = super.writeToNBTCustom(nbt);

        nbt.setByte("Dl", (byte) this.delay);
        nbt.setByte("Dis", (byte) this.disabledSidesMask);
        nbt.setByte("Conn", (byte) this.connectedSidesMask);
        nbt.setLong("Last", this.lastDelayUpdate);
        nbt.setByte("Max", (byte) this.itemHandlerBase.getSlotLimit(0));
        nbt.setByte("Clgg", (byte) this.cloggedItemsMask);

        NBTUtils.writeIntArrayAsByteArray(this.delaysPerSide, nbt, "Dls");
        nbt.setByteArray("Ind", this.outputSideIndices);

        byte[] validSidesMasks = new byte[this.validOutputSidesPerSide.length];

        for (int i = 0; i < validSidesMasks.length; i++)
        {
            validSidesMasks[i] = (byte) this.createMaskFromFacingArray(this.validOutputSidesPerSide[i]);
        }

        nbt.setByteArray("Vld", validSidesMasks);

        return nbt;
    }

    private EnumFacing[] createFacingArrayFromMask(int side, int mask)
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
