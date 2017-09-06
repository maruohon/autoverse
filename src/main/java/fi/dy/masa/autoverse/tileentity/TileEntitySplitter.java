package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
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
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockSplitter;
import fi.dy.masa.autoverse.gui.client.GuiSplitterLength;
import fi.dy.masa.autoverse.gui.client.GuiSplitterRedstone;
import fi.dy.masa.autoverse.gui.client.GuiSplitterSwitchable;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitterLength;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitterRedstone;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitterSwitchable;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperInsertOnly;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequenceBase;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSplitterLength;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSplitterSwitchable;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntitySplitter extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOut1;
    private ItemStackHandlerTileEntity inventoryOut2;
    private ItemHandlerWrapperSequenceBase splitter;

    private BlockSplitter.SplitterType type = BlockSplitter.SplitterType.REDSTONE;
    private EnumFacing facing2 = EnumFacing.EAST;
    private BlockPos posOut2;
    private int delay = 2;

    public TileEntitySplitter()
    {
        super(ReferenceNames.NAME_BLOCK_SPLITTER);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput     = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOut1      = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut1", this);
        this.inventoryOut2      = new ItemStackHandlerTileEntity(2, 1, 64, false, "ItemsOut2", this);
        this.itemHandlerBase    = this.inventoryInput;
    }

    private void initSplitterInventory(BlockSplitter.SplitterType type)
    {
        switch (type)
        {
            case SWITCHABLE:
                this.splitter = new ItemHandlerWrapperSplitterSwitchable(this.inventoryInput, this.inventoryOut1, this.inventoryOut2, this);
                this.itemHandlerExternal = this.splitter;
                break;

            case REDSTONE:
                this.itemHandlerExternal = new ItemHandlerWrapperInsertOnly(this.inventoryInput);
                break;

            case LENGTH:
                this.splitter = new ItemHandlerWrapperSplitterLength(this.inventoryInput, this.inventoryOut1, this.inventoryOut2);
                this.itemHandlerExternal = this.splitter;
                break;
        }
    }

    public IItemHandler getInventoryIn()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOut1()
    {
        return this.inventoryOut1;
    }

    public IItemHandler getInventoryOut2()
    {
        return this.inventoryOut2;
    }

    public ItemHandlerWrapperSplitterSwitchable getSplitterSwitchable()
    {
        return (ItemHandlerWrapperSplitterSwitchable) this.splitter;
    }

    public ItemHandlerWrapperSplitterLength getSplitterLength()
    {
        return (ItemHandlerWrapperSplitterLength) this.splitter;
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 1:
                this.setSecondOutputSide(EnumFacing.getFront(value), false);
                return true;

            case 2:
                this.delay = value;
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    @Override
    public int[] getProperties()
    {
        int[] values = super.getProperties();

        values[1] = this.facing2.getIndex();
        values[2] = this.delay;

        return values;
    }

    public void setSplitterType(BlockSplitter.SplitterType type)
    {
        this.type = type;
        this.initSplitterInventory(type);
    }

    public boolean outputIsSecondary()
    {
        switch (this.type)
        {
            case SWITCHABLE:
                return ((ItemHandlerWrapperSplitterSwitchable) this.splitter).secondaryOutputActive();

            case LENGTH:
                return ((ItemHandlerWrapperSplitterLength) this.splitter).secondaryOutputActive();

            case REDSTONE:
            default:
                return this.redstoneState;
        }
    }

    public EnumFacing getOutputFacing2()
    {
        return this.facing2;
    }

    public void setSecondOutputSide(EnumFacing side, boolean force)
    {
        if (force || side != this.facing)
        {
            this.facing2 = side;
        }

        this.posOut2 = this.getPos().offset(this.facing2);
    }

    public EnumFacing getSecondOutputRelativeFacing()
    {
        return PositionUtils.getRelativeFacing(this.getFacing(), this.facing2);
    }

    @Override
    public void rotate(Rotation rotationIn)
    {
        this.setSecondOutputSide(rotationIn.rotate(this.facing2), true);

        super.rotate(rotationIn);
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        if (super.onRightClickBlock(world, pos, state, side, player, hand, hitX, hitY, hitZ))
        {
            return true;
        }

        ItemStack stack = player.getHeldItem(hand);

        if (stack.isEmpty() && player.isSneaking())
        {
            if (side == this.facing2)
            {
                side = side.getOpposite();
            }

            this.setSecondOutputSide(side, false);
            this.markDirty();
            this.notifyBlockUpdate(this.getPos());
            return true;
        }

        return false;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = false;
        movedOut |= this.pushItemsToAdjacentInventory(this.inventoryOut1, 0, this.posFront, this.facingOpposite, false);
        movedOut |= this.pushItemsToAdjacentInventory(this.inventoryOut2, 0, this.posOut2, this.facing2.getOpposite(), false);
        boolean movedIn = false;

        if (this.type == BlockSplitter.SplitterType.REDSTONE)
        {
            IItemHandler inv = this.redstoneState ? this.inventoryOut2 : this.inventoryOut1;
            movedIn = InventoryUtils.tryMoveStack(this.inventoryInput, 0, inv, 0) != InvResult.MOVED_NOTHING;
        }
        else
        {
            movedIn = this.splitter.moveItems();
        }

        if (movedIn || movedOut)
        {
            this.scheduleUpdateIfNeeded(movedIn);
        }
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded(false);
    }

    private void scheduleUpdateIfNeeded(boolean force)
    {
        if (force ||
            this.inventoryInput.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOut1.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOut2.getStackInSlot(0).isEmpty() == false)
        {
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    @Override
    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOut1);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOut2);

        if (this.splitter != null)
        {
            this.splitter.dropAllItems(this.getWorld(), this.getPos());
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        this.scheduleUpdateIfNeeded(true);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        this.setSplitterType(BlockSplitter.SplitterType.fromMeta(tag.getByte("Type")));

        super.readFromNBTCustom(tag);

        this.setSecondOutputSide(EnumFacing.getFront(tag.getByte("Facing2")), false);
        this.delay = ((int) tag.getByte("Delay")) & 0xFF;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("Facing2", (byte)this.facing2.getIndex());
        nbt.setByte("Type", (byte) this.type.getMeta());
        nbt.setByte("Delay", (byte) this.delay);

        return nbt;
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound tag)
    {
        this.inventoryInput.deserializeNBT(tag);
        this.inventoryOut1.deserializeNBT(tag);
        this.inventoryOut2.deserializeNBT(tag);

        if (this.splitter != null)
        {
            this.splitter.deserializeNBT(tag);
        }
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        nbt.merge(this.inventoryInput.serializeNBT());
        nbt.merge(this.inventoryOut1.serializeNBT());
        nbt.merge(this.inventoryOut2.serializeNBT());

        if (this.splitter != null)
        {
            nbt.merge(this.splitter.serializeNBT());
        }
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag = super.getUpdatePacketTag(tag);
        tag.setByte("f", (byte) ((this.facing2.getIndex() << 4) | tag.getByte("f")));
        tag.setByte("typ", (byte) this.type.getMeta());
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        int facings = tag.getByte("f");
        this.setSecondOutputSide(EnumFacing.getFront((facings >>> 4) & 0x7), true);
        this.setSplitterType(BlockSplitter.SplitterType.fromMeta(tag.getByte("typ")));

        super.handleUpdateTag(tag);
    }

    @Override
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        if (action == 0)
        {
            switch (this.type)
            {
                case SWITCHABLE:
                    ((ItemHandlerWrapperSplitterSwitchable) this.splitter).switchOutputAndResetPosition(true);
                    break;

                case LENGTH:
                    ((ItemHandlerWrapperSplitterLength) this.splitter).switchOutputAndResetPosition();
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        switch (this.type)
        {
            case SWITCHABLE:
                return new ContainerSplitterSwitchable(player, this);

            case LENGTH:
                return new ContainerSplitterLength(player, this);

            case REDSTONE:
            default:
                return new ContainerSplitterRedstone(player, this);
        }
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        switch (this.type)
        {
            case SWITCHABLE:
                return new GuiSplitterSwitchable(new ContainerSplitterSwitchable(player, this), this);

            case LENGTH:
                return new GuiSplitterLength(new ContainerSplitterLength(player, this), this);

            case REDSTONE:
            default:
                return new GuiSplitterRedstone(new ContainerSplitterRedstone(player, this), this);
        }
    }
}
