package fi.dy.masa.autoverse.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockPipe;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.EntityUtils;
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
    public void rotate(Rotation rotation)
    {
        super.rotate(rotation);

        this.disabledInputSides = PositionUtils.rotateFacingMask(this.disabledInputSides, rotation);
        this.validInputSides = PositionUtils.rotateFacingMask(this.validInputSides, rotation);
    }

    @Override
    public BlockPipe.Connection getConnectionType(int sideIndex)
    {
        //System.out.printf("in: 0x%02X, conn: 0x%02X\n", this.validInputSides, this.connectedSides);
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

    /*
    @Override
    protected boolean canInputOnSide(EnumFacing side)
    {
        return false;
    }
    */

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
        return this.redstoneState == false;
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
    public void onLeftClickBlock(World world, BlockPos pos, EntityPlayer player)
    {
        if (world.isRemote == false && player.isSneaking())
        {
            IBlockState state = world.getBlockState(pos);
            RayTraceResult trace = EntityUtils.getRayTraceFromEntity(world, player, false);

            if (trace.typeOfHit == RayTraceResult.Type.BLOCK && pos.equals(trace.getBlockPos()))
            {
                EnumFacing targetSide = this.getActionTargetSide(world, pos, state, trace.sideHit, player);
                this.toggleInputOnSide(targetSide);
                this.scheduleCurrentWork(this.getDelay());
            }
        }
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking() &&
            player.getHeldItemMainhand().isEmpty() &&
            player.getHeldItemOffhand().isEmpty() == false)
        {
            if (world.isRemote == false)
            {
                EnumFacing targetSide = this.getActionTargetSide(world, pos, state, side, player);
                this.toggleInputOnSide(targetSide);
            }

            return true;
        }

        return super.onRightClickBlock(world, pos, state, side, player, hand, hitX, hitY, hitZ);
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

    /*
    @Override
    protected boolean reScheduleStuckItems()
    {
        int nextSheduledTick = -1;

        for (int slot = 0; slot < 6; slot++)
        {
            int delay = this.getDelayForSide(slot);

            System.out.printf("re-schedule stuck items @ %s, slot: %d, delay: %d\n", this.getPos(), slot, delay);
            if (delay < 0)
            {
                delay = this.getDelay();
                this.setDelayForSide(slot, delay);
            }

            // Get the soonest next scheduled update's delay
            if (delay >= 0 && (nextSheduledTick < 0 || delay < nextSheduledTick))
            {
                nextSheduledTick = delay;
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
    */

    @Override
    protected boolean tryMoveItemsForSide(World world, BlockPos pos, int slot)
    {
        boolean ret = super.tryMoveItemsForSide(world, pos, slot);

        //System.out.printf("%d - tryMoveItemsForSide() (EXTRACT) @ %s - slot: %d - start\n", world.getTotalWorldTime(), pos, slot);
        if (this.shouldOperatePull() && this.isAllowedToPullFromSide(EnumFacing.getFront(slot)))
        {
            InvResult result = this.tryPullInItemsFromSide(world, pos, slot);

            if (result == InvResult.NO_WORK)
            {
                //System.out.printf("%d - tryMoveItemsForSide() (EXTRACT) @ %s - slot: %d - NO WORK -> super\n", world.getTotalWorldTime(), pos, slot);
                //boolean ret = super.tryMoveItemsForSide(world, pos, slot);

                /*
                if (ret || this.itemHandlerBase.getStackInSlot(slot).isEmpty())
                {
                    System.out.printf("%d - tryMoveItemsForSide() (EXTRACT) @ %s - slot: %d - NO WORK -> super -> SCHED\n", world.getTotalWorldTime(), pos, slot);
                    return true;
                }
                */

                return ret || this.itemHandlerBase.getStackInSlot(slot).isEmpty();
            }
            else
            {
                //System.out.printf("%d - tryMoveItemsForSide() (EXTRACT) @ %s - SCHED\n", world.getTotalWorldTime(), pos);
                //this.scheduleCurrentWork(this.getDelay());
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
            IItemHandler inputInv = this.getInputInventory(slot);

            if (inputInv != null)
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
                        //System.out.printf("EXTRACTION tryPullInItemsFromSide(): pos: %s, slot: %d trying to pull...\n", posSelf, slot, side);
                        this.disableUpdateScheduling = true;
                        this.disableNeighorNotification = true;

                        InvResult result = InventoryUtils.tryMoveAllItems(inv, inputInv);

                        this.disableUpdateScheduling = false;
                        this.disableNeighorNotification = false;

                        if (result != InvResult.MOVED_NOTHING)
                        {
                            this.sendPacketInputItem(slot, this.itemHandlerBase.getStackInSlot(slot));
                        }

                        //if (result != InvResult.MOVED_NOTHING) System.out.printf("EXTRACTION tryPullInItemsFromSide(): pos: %s, slot: %d PULLED\n", posSelf, slot, side);
                        return result;
                    }
                }
            }
            //System.out.printf("EXTRACTION tryPullInItemsFromSide(): pos: %s, slot: %d - FAILED PULL\n", posSelf, slot);

            return InvResult.MOVED_NOTHING;
        }

        //System.out.printf("EXTRACTION tryPullInItemsFromSide(): pos: %s, slot: %d - NO WORK\n", posSelf, slot);
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
