package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockMuxer;
import fi.dy.masa.autoverse.gui.client.GuiMuxerProgrammable;
import fi.dy.masa.autoverse.gui.client.GuiMuxerSimple;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerMuxerProgrammable;
import fi.dy.masa.autoverse.inventory.container.ContainerMuxerSimple;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperInsertOnly;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperMuxer;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntityMuxer extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput1;
    private ItemStackHandlerTileEntity inventoryInput2;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemHandlerWrapperMuxer muxer;
    //private IItemHandler inventoryInputWrapper1;
    private IItemHandler inventoryInputWrapper2;
    private BlockMuxer.MuxerType type = BlockMuxer.MuxerType.REDSTONE;
    private EnumFacing facingInput = EnumFacing.EAST;
    private int delay = 2;

    public TileEntityMuxer()
    {
        super(ReferenceNames.NAME_BLOCK_MUXER);
    }

    public TileEntityMuxer(BlockMuxer.MuxerType type)
    {
        this();

        this.type = type;
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput1        = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn1", this);
        this.inventoryInput2        = new ItemStackHandlerTileEntity(1, 1,  1, false, "ItemsIn2", this);
        this.inventoryOutput        = new ItemStackHandlerTileEntity(2, 1, 64, false, "ItemsOut", this);

        this.itemHandlerBase        = this.inventoryInput1;

        //this.inventoryInputWrapper1 = new ItemHandlerWrapperInsertOnly(this.inventoryInput1);
        this.inventoryInputWrapper2 = new ItemHandlerWrapperInsertOnly(this.inventoryInput2);
    }

    private void initMuxerInventory(BlockMuxer.MuxerType type)
    {
        switch (type)
        {
            case PROGRAMMABLE:
                this.muxer = new ItemHandlerWrapperMuxer(
                        this.inventoryInput1,
                        this.inventoryInput2,
                        this.inventoryOutput,
                        this);

                this.itemHandlerExternal = this.muxer;
                break;

            case PRIORITY:
            case REDSTONE:
                this.inventoryInput1.setStackLimit(64);
                this.inventoryInput2.setStackLimit(64);
                this.itemHandlerExternal = new ItemHandlerWrapperMuxerSimple(this.inventoryInput1, this.inventoryOutput);
                break;
        }
    }

    public void setMuxerType(BlockMuxer.MuxerType type)
    {
        this.type = type;
        this.initMuxerInventory(type);
    }

    public BlockMuxer.MuxerType getMuxerType()
    {
        return this.type;
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 1:
                this.setInputSide(EnumFacing.getFront(value), false);
                return true;

            case 2:
                this.delay = value & 0xFF;
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    public void setInputSide(EnumFacing side, boolean force)
    {
        if (force || side != this.facing)
        {
            this.facingInput = side;
        }
    }

    @Override
    public void rotate(Rotation rotationIn)
    {
        this.setInputSide(rotationIn.rotate(this.facingInput), true);

        super.rotate(rotationIn);
    }

    /**
     * This returns the filter-out side's facing as what it would be if the non-match-out
     * side was North, which is the default rotation for the model.
     * That way the filter-out side's texture will be placed on the correct face
     * of the non-rotated model, before the primary facing's rotation is applied to the entire model.
     */
    public EnumFacing getInputRelativeFacing()
    {
        return PositionUtils.getRelativeFacing(this.getFacing(), this.facingInput);
    }

    public IItemHandler getInventoryInput1()
    {
        return this.inventoryInput1;
    }

    public IItemHandler getInventoryInput2()
    {
        return this.inventoryInput2;
    }

    public IItemHandler getInventoryOutput()
    {
        return this.inventoryOutput;
    }

    public ItemHandlerWrapperMuxer getInventoryMuxer()
    {
        return this.muxer;
    }

    public boolean getCurrentInputIsSecondary()
    {
        switch (this.type)
        {
            case PROGRAMMABLE:
                return this.muxer.secondaryInputActive();

            case REDSTONE:
                return this.redstoneState;

            case PRIORITY:
            default:
                return this.inventoryInput2.getStackInSlot(0).isEmpty() == false;
        }
    }

    /*
    public void setCurrentInputIsSecondary(boolean isSecondary)
    {
        this.currentInputSecondary = isSecondary;
    }
    */

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        ItemStack stack = player.getHeldItem(hand);

        if (stack.isEmpty() && player.isSneaking())
        {
            this.setInputSide(side, false);
            this.notifyBlockUpdate(this.getPos());
            this.markDirty();
            return true;
        }

        return false;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = this.pushItemsToAdjacentInventory(this.inventoryOutput, 0, this.posFront, this.facingOpposite, false);
        boolean movedIn = false;

        switch (this.type)
        {
            case PROGRAMMABLE:
                movedIn = this.muxer.moveItems();
                break;

            case PRIORITY:
            {
                IItemHandler inv = this.inventoryInput2.getStackInSlot(0).isEmpty() == false ? this.inventoryInput2 : this.inventoryInput1;
                movedIn = InventoryUtils.tryMoveStack(inv, 0, this.inventoryOutput, 0) != InvResult.MOVED_NOTHING;
                break;
            }

            case REDSTONE:
            {
                IItemHandler inv = this.redstoneState ? this.inventoryInput2 : this.inventoryInput1;
                movedIn = InventoryUtils.tryMoveStack(inv, 0, this.inventoryOutput, 0) != InvResult.MOVED_NOTHING;
                break;
            }
        }

        if (movedIn || movedOut)
        {
            this.scheduleUpdateIfNeeded(movedIn);
        }
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        this.scheduleUpdateIfNeeded(false);
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded(false);
    }

    private void scheduleUpdateIfNeeded(boolean force)
    {
        if (force || this.inventoryOutput.getStackInSlot(0).isEmpty() == false)
        {
            this.scheduleBlockUpdate(this.delay, false);
            return;
        }

        switch (this.type)
        {
            case PROGRAMMABLE:
                boolean secondary = this.muxer.secondaryInputActive();

                if ((secondary == false && this.inventoryInput1.getStackInSlot(0).isEmpty() == false) ||
                    (secondary          && this.inventoryInput2.getStackInSlot(0).isEmpty() == false))
                {
                    this.scheduleBlockUpdate(this.delay, false);
                }
                break;

            case PRIORITY:
                if (this.inventoryInput1.getStackInSlot(0).isEmpty() == false ||
                    this.inventoryInput2.getStackInSlot(0).isEmpty() == false)
                {
                    this.scheduleBlockUpdate(this.delay, false);
                }
                break;

            case REDSTONE:
                if ((this.redstoneState == false && this.inventoryInput1.getStackInSlot(0).isEmpty() == false) ||
                    (this.redstoneState          && this.inventoryInput2.getStackInSlot(0).isEmpty() == false))
                {
                    this.scheduleBlockUpdate(this.delay, false);
                }
                break;
        }
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        this.setInputSide(EnumFacing.getFront(nbt.getByte("InFacing")), false);
        this.setMuxerType(BlockMuxer.MuxerType.fromBlockMeta(nbt.getByte("Type")));
        this.delay = nbt.getByte("Delay");

        super.readFromNBTCustom(nbt);
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        this.inventoryInput1.deserializeNBT(nbt);
        this.inventoryInput2.deserializeNBT(nbt);
        this.inventoryOutput.deserializeNBT(nbt);

        if (this.muxer != null)
        {
            this.muxer.deserializeNBT(nbt);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("Type", (byte) this.type.getBlockMeta());
        nbt.setByte("InFacing", (byte) this.facingInput.getIndex());
        nbt.setByte("Delay", (byte) this.delay);

        return nbt;
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        nbt.merge(this.inventoryInput1.serializeNBT());
        nbt.merge(this.inventoryInput2.serializeNBT());
        nbt.merge(this.inventoryOutput.serializeNBT());

        if (this.muxer != null)
        {
            nbt.merge(this.muxer.serializeNBT());
        }
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag.setByte("f", (byte) ((this.facingInput.getIndex() << 4) | this.getFacing().getIndex()));
        tag.setByte("t", (byte) this.type.getBlockMeta());
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        int facings = tag.getByte("f");
        this.setFacing(EnumFacing.getFront(facings & 0x7));
        this.setInputSide(EnumFacing.getFront((facings >>> 4) & 0x7), false);
        this.setMuxerType(BlockMuxer.MuxerType.fromBlockMeta(tag.getByte("t")));

        this.notifyBlockUpdate(this.getPos());
    }

    @Override
    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput1);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput2);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutput);

        if (this.muxer != null)
        {
            this.muxer.dropAllItems(this.getWorld(), this.getPos());
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        this.scheduleUpdateIfNeeded(false);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            if (facing == this.facingInput)
            {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.inventoryInputWrapper2);
            }
            else
            {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.itemHandlerExternal);
            }
        }

        return super.getCapability(capability, facing);
    }

    private static class ItemHandlerWrapperMuxerSimple implements IItemHandler
    {
        private final IItemHandler inventoryIn;
        private final IItemHandler inventoryOut;

        private ItemHandlerWrapperMuxerSimple(IItemHandler inventoryIn, IItemHandler inventoryOut)
        {
            this.inventoryIn = inventoryIn;
            this.inventoryOut = inventoryOut;
        }

        @Override
        public int getSlots()
        {
            return 2;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return slot == 0 ? this.inventoryIn.getSlotLimit(0) : this.inventoryOut.getSlotLimit(0);
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return slot == 0 ? this.inventoryIn.getStackInSlot(0) : this.inventoryOut.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            return slot == 0 ? this.inventoryIn.insertItem(0, stack, simulate) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return slot == 1 ? this.inventoryOut.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        if (this.type == BlockMuxer.MuxerType.PROGRAMMABLE)
        {
            return new ContainerMuxerProgrammable(player, this);
        }
        else
        {
            return new ContainerMuxerSimple(player, this);
        }
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        if (this.type == BlockMuxer.MuxerType.PROGRAMMABLE)
        {
            return new GuiMuxerProgrammable(new ContainerMuxerProgrammable(player, this), this);
        }
        else
        {
            return new GuiMuxerSimple(new ContainerMuxerSimple(player, this), this);
        }
    }
}
