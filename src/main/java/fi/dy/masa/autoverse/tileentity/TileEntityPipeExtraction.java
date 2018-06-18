package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockPipe;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntityPipeExtraction extends TileEntityPipe
{
    private int disabledInputSides;
    private int validInputSides;

    public TileEntityPipeExtraction()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_PIPE_EXTRACTION);
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        if (propId == 3)
        {
            this.setInputMask(value);
            return true;
        }
        else
        {
            return super.applyProperty(propId, value);
        }
    }

    @Override
    public int[] getProperties()
    {
        int[] values = super.getProperties();

        values[3] = this.disabledInputSides;

        return values;
    }

    @Override
    public void rotate(Rotation rotation)
    {
        if (rotation != Rotation.NONE)
        {
            this.disabledInputSides = PositionUtils.rotateFacingMask(this.disabledInputSides, rotation);
            this.validInputSides = PositionUtils.rotateFacingMask(this.validInputSides, rotation);
        }

        super.rotate(rotation);
    }

    @Override
    public BlockPipe.Connection getConnectionType(int sideIndex)
    {
        if ((this.validInputSides & (1 << sideIndex)) != 0)
        {
            return BlockPipe.Connection.TYPE;
        }
        else if ((this.getConnectedSidesMask() & (1 << sideIndex)) != 0)
        {
            return BlockPipe.Connection.BASIC;
        }

        return BlockPipe.Connection.NONE;
    }

    private boolean canPullFromSide(EnumFacing side)
    {
        if (this.isAllowedToPullFromSide(side) && super.canInputOnSide(side))
        {
            TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(side));

            return (te instanceof TileEntityPipe) == false && te != null &&
                    te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
        }

        return false;
    }

    private boolean isAllowedToPullFromSide(EnumFacing side)
    {
        return (this.disabledInputSides & (1 << side.getIndex())) == 0;
    }

    @Override
    protected boolean hasWorkOnSide(int slot)
    {
        return super.hasWorkOnSide(slot) ||
                (this.shouldOperatePull() && (this.validInputSides & (1 << slot)) != 0 &&
                 this.itemHandlerBase.getStackInSlot(slot).isEmpty());
    }

    private boolean shouldOperatePull()
    {
        return this.getRedstoneState() == false;
    }

    @Override
    public boolean updateConnectedSides(boolean notify)
    {
        boolean dirty = super.updateConnectedSides(false);
        int mask = 0;

        for (EnumFacing side : EnumFacing.values())
        {
            if (this.canPullFromSide(side))
            {
                mask |= (1 << side.getIndex());
            }
        }

        if (mask != this.validInputSides)
        {
            this.validInputSides = mask;
            dirty = true;
        }

        if (dirty)
        {
            this.notifyBlockUpdate(this.getPos());
        }

        return dirty;
    }

    @Override
    public boolean onLeftClickBlock(World world, BlockPos pos, EnumFacing side, EntityPlayer player)
    {
        if (super.onLeftClickBlock(world, pos, side, player))
        {
            return true;
        }
        else if (player.isSneaking())
        {
            if (world.isRemote == false)
            {
                this.toggleInputOnSide(side);
            }

            return true;
        }

        return false;
    }

    private void toggleInputOnSide(EnumFacing side)
    {
        this.setInputMask(this.disabledInputSides ^ (1 << side.getIndex()));
        this.scheduleCurrentWork(this.getDelay());
    }

    private void setInputMask(int mask)
    {
        this.disabledInputSides = mask & 0x3F;
        this.updateConnectedSides(true);
        this.markDirty();

        this.getWorld().notifyNeighborsOfStateChange(this.getPos(), this.getBlockType(), false);
        this.notifyBlockUpdate(this.getPos());
    }

    @Override
    protected boolean tryMoveItemsForSide(World world, BlockPos pos, int slot)
    {
        boolean ret = super.tryMoveItemsForSide(world, pos, slot);

        if (this.shouldOperatePull() && this.isAllowedToPullFromSide(EnumFacing.getFront(slot)))
        {
            InvResult result = this.tryPullInItemsFromSide(world, pos, slot);

            if (result == InvResult.NO_WORK)
            {
                return ret || this.itemHandlerBase.getStackInSlot(slot).isEmpty();
            }
            else
            {
                return ret || result != InvResult.MOVED_NOTHING;
            }
        }
        // shouldOperate() is only used for pulling in items, not pushing items out
        else
        {
            return ret;
        }
    }

    /**
     * Tries to pull in items from the given side, if the input slot for that side is currently empty.
     * @param world
     * @param posSelf
     * @param slot
     * @return true if the input slot WAS empty, and thus no need to try to push out items
     */
    private InvResult tryPullInItemsFromSide(World world, BlockPos posSelf, int slot)
    {
        // Empty input slot, try to pull in items
        if (this.itemHandlerBase.getStackInSlot(slot).isEmpty())
        {
            EnumFacing side = EnumFacing.getFront(slot);
            TileEntity te = world.getTileEntity(posSelf.offset(side));

            if (te != null &&
                (te instanceof TileEntityPipe) == false &&
                te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite()))
            {
                IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());

                if (inv != null)
                {
                    this.disableUpdateScheduling = true;
                    this.disableNeighorNotification = true;

                    ItemStack stack = InventoryUtils.getItemsFromFirstNonEmptySlot(inv, this.itemHandlerBase.getInventoryStackLimit(), false);
                    InvResult result = InvResult.MOVED_NOTHING;

                    if (stack.isEmpty() == false)
                    {
                        this.itemHandlerBase.insertItem(slot, stack, false);
                        result = InvResult.MOVED_SOME;
                    }

                    this.disableUpdateScheduling = false;
                    this.disableNeighorNotification = false;

                    if (result != InvResult.MOVED_NOTHING)
                    {
                        this.sendPacketInputItem(slot, this.itemHandlerBase.getStackInSlot(slot));
                    }

                    return result;
                }
            }

            return InvResult.MOVED_NOTHING;
        }

        return InvResult.NO_WORK;
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.disabledInputSides = nbt.getByte("DIn");
        this.validInputSides = nbt.getByte("Ins");
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt = super.writeToNBTCustom(nbt);

        nbt.setByte("DIn", (byte) this.disabledInputSides);
        nbt.setByte("Ins", (byte) this.validInputSides);

        return nbt;
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound nbt)
    {
        nbt.setShort("sd", (short) ((this.validInputSides << 6) | this.getConnectedSidesMask()));

        return nbt;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        int mask = tag.getShort("sd");
        this.setConnectedSidesMask(mask & 0x3F);
        this.validInputSides = mask >>> 6;

        this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
    }
}
