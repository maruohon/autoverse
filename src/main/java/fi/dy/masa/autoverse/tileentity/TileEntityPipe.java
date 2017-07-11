package fi.dy.masa.autoverse.tileentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
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
import fi.dy.masa.autoverse.gui.client.GuiPipe;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerPipe;
import fi.dy.masa.autoverse.network.message.ISyncableTile;
import fi.dy.masa.autoverse.network.message.MessageSyncTileEntity;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.NBTUtils;

public class TileEntityPipe extends TileEntityAutoverseInventory implements ISyncableTile
{
    private static final EnumFacing[] SIDES_X = new EnumFacing[] { EnumFacing.DOWN,  EnumFacing.UP,    EnumFacing.NORTH, EnumFacing.SOUTH };
    private static final EnumFacing[] SIDES_Z = new EnumFacing[] { EnumFacing.DOWN,  EnumFacing.UP,    EnumFacing.WEST,  EnumFacing.EAST };
    private static final EnumFacing[] SIDES_Y = new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST,  EnumFacing.EAST };

    private final IItemHandler[] sideInventories;
    private final EnumFacing[][] validSides;
    private final NonNullList<ItemStack> stacksLast = NonNullList.withSize(6, ItemStack.EMPTY);
    private int delays[] = new int[6];
    private int sideIndices[] = new int[6];
    private int connectedSides;
    private int disabledSides;
    private long lastDelayUpdate;
    private int delay = 8;
    private boolean disableUpdateScheduling;

    public TileEntityPipe()
    {
        super(ReferenceNames.NAME_BLOCK_PIPE);

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
        this.markDirty();
    }

    private boolean isSideDisabled(EnumFacing side)
    {
        return (this.disabledSides & (1 << side.getIndex())) != 0;
    }

    public void updateConnectedSides()
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

        this.connectedSides = mask;
    }

    private boolean checkCanConnectOnSide(EnumFacing side)
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

    private boolean tryPushOutScheduledItems(World world, BlockPos pos)
    {
        long currentTime = this.getWorld().getTotalWorldTime();
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
                    System.out.printf("tryPushOutScheduledItems(): pos: %s, slot: %d - NOW\n", pos, slot);
                    if (this.tryPushOutItem(world, pos, slot))
                    {
                        System.out.printf("tryPushOutScheduledItems(): pos: %s, slot: %d - PUSHED\n", pos, slot);
                        if (this.itemHandlerBase.getStackInSlot(slot).isEmpty())
                        {
                            this.delays[slot] = -1;
                            continue;
                        }
                        else
                        {
                            this.delays[slot] = this.delay;
                        }
                    }
                    else
                    {
                        System.out.printf("tryPushOutScheduledItems(): pos: %s, slot: %d - FAILED PUSH\n", pos, slot);
                        this.delays[slot] = -1;
                        continue;
                    }
                }
                // Not ready to be moved out yet
                else
                {
                    System.out.printf("tryPushOutScheduledItems(): pos: %s, slot: %d - NOT YET\n", pos, slot);
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
            System.out.printf("tryPushOutScheduledItems(): pos: %s - SCHED\n", pos);
            this.scheduleBlockUpdate(nextSheduledTick, false);
        }

        this.lastDelayUpdate = currentTime;
        return nextSheduledTick > 0;
    }

    private boolean tryPushOutItem(World world, BlockPos pos, int slot)
    {
        System.out.printf("tryPushOutItem(): pos: %s, slot: %d, valid sides: %d\n", pos, slot, this.validSides[slot].length);
        for (int i = 0; i < this.validSides[slot].length; i++)
        {
            EnumFacing outputSide = this.getOutputSideForInputSide(slot);

            if (this.tryPushOutItemsToSide(world, pos, outputSide, slot))
            {
                System.out.printf("tryPushOutItem(): pos: %s, side: %s - SUCCESS\n", pos, outputSide);
                return true;
            }
            else
            {
                System.out.printf("tryPushOutItem(): pos: %s, side: %s - FAIL\n", pos, outputSide);
            }

            outputSide = this.cycleOutputSideForInputSide(slot);
        }

        return false;
    }

    private boolean addDelaysForStuckItems()
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

    private boolean tryPushOutItemsToSide(World world, BlockPos posSelf, EnumFacing side, int slot)
    {
        TileEntity te = world.getTileEntity(posSelf.offset(side));

        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite()))
        {
            System.out.printf("tryPushOutItemsToSide(): pos: %s, slot: %d pushing to side: %s\n", posSelf, slot, side);
            IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());

            if (inv != null)
            {
                // This is used to prevent scheduling a new update because of an adjacent inventory changing
                // while we push out items, and our own inventory changing due to this extract.
                // The update will be scheduled, if needed, after the push is complete.
                this.disableUpdateScheduling = true;

                ItemStack stack = this.itemHandlerBase.extractItem(slot, 64, false);
                int sizeOrig = stack.getCount();
                boolean movedSome = false;

                stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);

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

    private void updateAllValidOutputSidesForInputSide(final EnumFacing inputSide)
    {
        final List<EnumFacing> sides = new ArrayList<EnumFacing>();
        final EnumFacing oppositeSide = inputSide.getOpposite();

        if (this.checkCanConnectOnSide(oppositeSide))
        {
            sides.add(oppositeSide);
        }

        EnumFacing[] possibleSides;

        switch (inputSide)
        {
            case UP:
            case DOWN:
                possibleSides = SIDES_Y;
                break;

            case NORTH:
            case SOUTH:
                possibleSides = SIDES_Z;
                break;

            default:
                possibleSides = SIDES_X;
        }

        for (EnumFacing side : possibleSides)
        {
            if (this.checkCanConnectOnSide(side))
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

        this.updateConnectedSides();
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        // When an adjacent tile changes, schedule a new tile tick.
        // Updates will not be scheduled due to the adjacent inventory changing
        // while we are pushing items to it (this.disableUpdateScheduling == true).
        if (this.disableUpdateScheduling == false && this.addDelaysForStuckItems()) // && this.shouldOperate())
        {
            //System.out.printf("onNeighborTileChange(), scheduling(?) for %s\n", this.getPos());
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    @Override
    public boolean onRightClickBlock(EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking() && player.getHeldItem(hand).isEmpty())
        {
            this.toggleSideDisabled(side);
            return true;
        }

        return false;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        if (this.tryPushOutScheduledItems(world, pos))
        {
            System.out.printf("onScheduledBlockUpdate(): pos: %s - YAY\n", pos);
            this.scheduleBlockUpdate(this.delay, false);
        }
        else
        {
            System.out.printf("onScheduledBlockUpdate(): pos: %s - FAIL\n", pos);
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        if (this.disableUpdateScheduling == false && this.getWorld().isRemote == false)
        {
            this.onSlotChange(slot, false);

            ItemStack stack = this.itemHandlerBase.getStackInSlot(slot);

            // Yes that's right, we compare references here, as it should suffice
            if (this.stacksLast.get(slot) != stack)
            {
                this.stacksLast.set(slot, stack);
                this.sendPacketToWatchers(new MessageSyncTileEntity(this.getPos(),
                        new int[] { slot, this.delay }, new ItemStack[] { stack }));
            }
        }
    }

    private void onSlotChange(int slot, boolean force)
    {
        if (this.delays[slot] <= 0 && (force || this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false))
        {
            System.out.printf("onItemReceived(): slot/side: %d - %s - SCHED\n", slot, EnumFacing.getFront(slot));
            this.delays[slot] = this.delay;
            this.scheduleBlockUpdate(this.delay, false);
        }
        else
        {
            System.out.printf("onItemReceived(): slot/side: %d - %s - NOPE\n", slot, EnumFacing.getFront(slot));
        }
    }

    @Override
    public void syncTile(int[] intValues, ItemStack[] stacks)
    {
        if (intValues.length == 2 && stacks.length == 1)
        {
            this.itemHandlerBase.setStackInSlot(intValues[0], stacks[0]);

            if (stacks[0].isEmpty() == false)
            {
                this.delays[intValues[0]] = intValues[1];
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        if (facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
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
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        return super.getUpdatePacketTag(tag);
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        super.handleUpdateTag(tag);
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
