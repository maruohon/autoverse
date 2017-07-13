package fi.dy.masa.autoverse.tileentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
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
    protected final IItemHandler[] sideInventories;
    protected final EnumFacing[][] validSides;
    protected final NonNullList<ItemStack> stacksLast = NonNullList.withSize(6, ItemStack.EMPTY);
    protected int sideIndices[] = new int[6];
    protected int connectedSides;
    protected int disabledSides;
    protected long lastDelayUpdate;
    protected int delay = 8;
    protected boolean disableUpdateScheduling;
    public int delays[] = new int[6];

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
        this.sideInventories = new IItemHandler[6];

        for (int i = 0; i < 6; i++)
        {
            this.sideInventories[i] = new InventoryWrapperSides(this.itemHandlerBase, i, this);
        }

        this.validSides = new EnumFacing[6][];

        for (int i = 0; i < 6; i++)
        {
            this.validSides[i] = new EnumFacing[0];
        }

        Arrays.fill(this.delays, -1);
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

    public void setMaxStackSize(int maxSize)
    {
        this.itemHandlerBase.setStackLimit(MathHelper.clamp(maxSize, 1, 64));
    }

    public int getConnectedSidesMask()
    {
        return this.connectedSides;
    }

    public BlockPipe.Connection getConnectionType(int sideIndex)
    {
        return (this.connectedSides & (1 << sideIndex)) != 0 ? BlockPipe.Connection.BASIC : BlockPipe.Connection.NONE;
    }

    private void setDisabledSidesMask(int mask)
    {
        for (int i = 0, bit = 0x1; i < 6; i++, bit <<= 1)
        {
            this.setSideDisabled(i, (mask & bit) != 0);
        }
    }

    private void setSideDisabled(int sideIndex, boolean disabled)
    {
        if (disabled)
        {
            this.disabledSides |= (1 << sideIndex);
        }
        else
        {
            this.disabledSides &= ~(1 << sideIndex);
        }

        this.markDirty();
    }

    private void toggleSideDisabled(EnumFacing side)
    {
        this.disabledSides ^= (1 << side.getIndex());
        this.updateConnectedSides(true);
        this.notifyBlockUpdate(this.getPos());
        this.getWorld().neighborChanged(this.getPos().offset(side), this.getBlockType(), this.getPos());
        this.markDirty();
    }

    private boolean isSideDisabled(EnumFacing side)
    {
        return (this.disabledSides & (1 << side.getIndex())) != 0;
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

        if (mask != this.connectedSides)
        {
            this.connectedSides = mask;

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
        if ((this.disabledSides & (1 << side.getIndex())) == 0)
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
            if (this.delays[slot] >= 0)
            {
                // This slot's item is ready to be moved out
                if (this.delays[slot] <= elapsedTime)
                {
                    //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s, slot: %d - NOW\n", pos, slot);
                    int newDelay = this.tryMoveItemsForSide(world, pos, slot);
                    this.delays[slot] = newDelay;

                    if (newDelay == -1)
                    {
                        continue;
                    }
                }
                // Not ready to be moved out yet
                else
                {
                    //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s, slot: %d - NOT YET\n", pos, slot);
                    // Update the time remaining in the delay
                    this.delays[slot] -= elapsedTime;
                }

                // Get the soonest next scheduled update's delay
                if (nextSheduledTick < 0 || this.delays[slot] < nextSheduledTick)
                {
                    nextSheduledTick = this.delays[slot];
                }
            }
        }

        if (nextSheduledTick > 0)
        {
            //System.out.printf("BASIC tryMoveScheduledItems(): pos: %s - SCHED\n", pos);
            this.scheduleBlockUpdate(nextSheduledTick, false);
        }

        this.lastDelayUpdate = currentTime;
        return nextSheduledTick > 0;
    }

    protected int tryMoveItemsForSide(World world, BlockPos pos, int slot)
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
            return this.delay;
        }
        else
        {
            //System.out.printf("BASIC tryMoveItemsForSide(): pos: %s, slot: %d - FAILED PUSH\n", pos, slot);
            return -1;
        }
    }

    private boolean tryPushOutItem(World world, BlockPos pos, int slot)
    {
        EnumFacing outputSide = this.getOutputSideForInputSide(slot);

        //System.out.printf("BASIC tryPushOutItem(): pos: %s, slot: %d, valid sides: %d\n", pos, slot, this.validSides[slot].length);
        for (int i = 0; i < this.validSides[slot].length; i++)
        {
            if (outputSide != null && this.tryPushOutItemsToSide(world, pos, outputSide, slot))
            {
                //System.out.printf("BASIC tryPushOutItem(): pos: %s, side: %s - SUCCESS\n", pos, outputSide);
                return true;
            }
            else
            {
                //System.out.printf("BASIC tryPushOutItem(): pos: %s, side: %s - FAIL\n", pos, outputSide);
            }

            outputSide = this.cycleOutputSideForInputSide(slot);
        }

        return false;
    }

    private boolean tryPushOutItemsToSide(World world, BlockPos posSelf, EnumFacing side, int slot)
    {
        TileEntity te = world.getTileEntity(posSelf.offset(side));

        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite()))
        {
            //System.out.printf("BASIC tryPushOutItemsToSide(): pos: %s, slot: %d pushing to side: %s\n", posSelf, slot, side);
            IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());

            if (inv != null)
            {
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

                    stack = this.itemHandlerBase.extractItem(slot, 64, false);
                    sizeOrig = stack.getCount();
                    stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);
                }
                else
                {
                    return false;
                }

                // Return the items that couldn't be moved
                if (stack.isEmpty() == false)
                {
                    movedSome = stack.getCount() != sizeOrig;
                    this.itemHandlerBase.insertItem(slot, stack, false);
                }

                this.disableUpdateScheduling = false;

                return stack.isEmpty() || movedSome;
            }
        }

        return false;
    }

    protected boolean reScheduleStuckItems()
    {
        boolean addedDelays = false;

        for (int slot = 0; slot < 6; slot++)
        {
            if (this.delays[slot] <= 0 && this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false)
            {
                this.delays[slot] = this.delay;
                addedDelays = true;
            }
        }

        return addedDelays;
    }

    @Nullable
    private EnumFacing getOutputSideForInputSide(int slot)
    {
        return this.sideIndices[slot] < this.validSides[slot].length ? this.validSides[slot][this.sideIndices[slot]] : null;
    }

    private EnumFacing cycleOutputSideForInputSide(int slot)
    {
        if (++this.sideIndices[slot] >= this.validSides[slot].length)
        {
            this.sideIndices[slot] = 0;
        }

        return this.getOutputSideForInputSide(slot);
    }

    protected boolean checkCanOutputOnSide(EnumFacing side)
    {
        if ((this.disabledSides & (1 << side.getIndex())) == 0)
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
        if ((this.disabledSides & (1 << side.getIndex())) == 0)
        {
            TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(side));
            int slot = side.getIndex();

            if (te instanceof TileEntityPipe)
            {
                this.createInputInventoryIfNull(slot);
            }
            else
            {
                if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite()))
                {
                    this.createInputInventoryIfNull(slot);
                }
                else
                {
                    this.sideInventories[slot] = null;
                }
            }
        }
    }

    protected void createInputInventoryIfNull(int slot)
    {
        if (this.sideInventories[slot] == null)
        {
            this.sideInventories[slot] = new InventoryWrapperSides(this.itemHandlerBase, slot, this);
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

        this.validSides[inputSide.getIndex()] = sides.toArray(new EnumFacing[sides.size()]);
    }

    @Override
    public void onLoad()
    {
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        this.updateConnectedSides(true);
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        // When an adjacent tile changes, schedule a new tile tick.
        // Updates will not be scheduled due to the adjacent inventory changing
        // while we are pushing items to it (this.disableUpdateScheduling == true).
        if (this.disableUpdateScheduling == false && this.reScheduleStuckItems()) // && this.shouldOperate())
        {
            //System.out.printf("onNeighborTileChange(), scheduling(?) for %s\n", this.getPos());
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
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
        if (this.delays[slot] <= 0 && (force || this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false))
        {
            //System.out.printf("BASIC onSlotChange(): slot/side: %d - %s - SCHED\n", slot, EnumFacing.getFront(slot));
            this.delays[slot] = this.delay;
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
                this.delays[intValues[0]] = intValues[1];
                this.delaysClient[intValues[0]] = intValues[1];
            }
            else
            {
                this.delays[intValues[0]] = -2;
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
            this.sideInventories[facing.getIndex()] != null)
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
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.sideInventories[facing.getIndex()]);
        }
        else
        {
            return super.getCapability(capability, facing);
        }
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound nbt)
    {
        nbt = super.getUpdatePacketTag(nbt);
        nbt.setByte("sd", (byte) this.connectedSides);

        return nbt;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.connectedSides = tag.getByte("sd");

        super.handleUpdateTag(tag);

        this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.delay = nbt.getByte("Delay");
        this.disabledSides = nbt.getByte("DisabledSides");
        this.connectedSides = nbt.getByte("ConnectedSides");
        this.lastDelayUpdate = nbt.getLong("LastUpdate");
        this.setMaxStackSize(nbt.getByte("MaxStack"));

        NBTUtils.readByteArrayIntoIntArray(this.delays, nbt, "Delays");
        NBTUtils.readByteArrayIntoIntArray(this.sideIndices, nbt, "SideIndices");

        byte[] validSidesMasks = new byte[this.validSides.length];
        NBTUtils.readByteArray(validSidesMasks, nbt, "ValidSides");

        for (int i = 0; i < validSidesMasks.length; i++)
        {
            this.validSides[i] = this.createFacingArrayFromMask(validSidesMasks[i]);
        }
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt = super.writeToNBTCustom(nbt);

        nbt.setByte("Delay", (byte) this.delay);
        nbt.setByte("DisabledSides", (byte) this.disabledSides);
        nbt.setByte("ConnectedSides", (byte) this.connectedSides);
        nbt.setLong("LastUpdate", this.lastDelayUpdate);
        nbt.setByte("MaxStack", (byte) this.itemHandlerBase.getSlotLimit(0));

        NBTUtils.writeIntArrayAsByteArray(this.delays, nbt, "Delays");
        NBTUtils.writeIntArrayAsByteArray(this.sideIndices, nbt, "SideIndices");

        byte[] validSidesMasks = new byte[this.validSides.length];

        for (int i = 0; i < validSidesMasks.length; i++)
        {
            validSidesMasks[i] = (byte) this.createMaskFromFacingArray(this.validSides[i]);
        }

        nbt.setByteArray("ValidSides", validSidesMasks);

        return nbt;
    }

    private EnumFacing[] createFacingArrayFromMask(int mask)
    {
        final List<EnumFacing> sides = new ArrayList<EnumFacing>();

        for (int i = 0, bit = 0x1; i < 6; i++, bit <<= 1)
        {
            if ((mask & bit) != 0)
            {
                sides.add(EnumFacing.getFront(i));
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
