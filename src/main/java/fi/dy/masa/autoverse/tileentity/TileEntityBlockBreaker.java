package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.event.BlockBreakDropsHandler;
import fi.dy.masa.autoverse.gui.client.GuiBlockBreaker;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBlockBreaker;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperExtractOnly;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityBlockBreaker extends TileEntityAutoverseInventory
{
    private boolean isGreedy;
    private boolean breakScheduled;
    private int delay = 4;
    private boolean preventUpdates;

    public TileEntityBlockBreaker()
    {
        super(ReferenceNames.NAME_BLOCK_BREAKER);

        this.itemHandlerBase = new ItemStackHandlerTileEntity(9, this);
        this.itemHandlerExternal = new ItemHandlerWrapperExtractOnly(this.itemHandlerBase);
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 1:
                this.setDelay(value);
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    @Override
    public int[] getProperties()
    {
        int[] values = super.getProperties();

        values[1] = this.delay;

        return values;
    }

    public void setDelay(int delay)
    {
        this.delay = MathHelper.clamp(delay & 0xFF, 1, 255);
    }

    public void setIsGreedy(boolean isGreedy)
    {
        this.isGreedy = isGreedy;
        this.markDirty();
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        if (this.getRedstoneState() == false)
        {
            this.breakScheduled = true;
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded();
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        if (this.preventUpdates == false)
        {
            this.scheduleUpdateIfNeeded();
        }
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        if (this.breakScheduled && this.getRedstoneState() == false)
        {
            this.breakAdjacentBlocks();
            this.breakScheduled = false;
        }

        if (this.tryPushOutItems())
        {
            // Schedule a new update when successfully pushed out some items,
            // to clear the inventory
            this.scheduleUpdateIfNeeded();
        }
    }

    private void scheduleUpdateIfNeeded()
    {
        if (InventoryUtils.getFirstNonEmptySlot(this.itemHandlerBase) != -1)
        {
            this.scheduleBlockUpdate(1, false);
        }
    }

    private boolean tryPushOutItems()
    {
        int slot = InventoryUtils.getFirstNonEmptySlot(this.itemHandlerBase);

        if (slot != -1)
        {
            final EnumFacing facingOpposite = this.getOppositeFacing();
            final EnumFacing facing = this.getFacing();
            BlockPos pos = this.getPos().offset(facingOpposite);
            TileEntity te = this.getWorld().getTileEntity(pos);

            // If there is no inventory directly behind the breaker, then offset the position one more,
            // in case there is an inventory behind a Frame block for example.
            if (te == null || te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing) == false)
            {
                pos = pos.offset(facingOpposite);
            }

            this.preventUpdates = true;
            boolean ret = this.pushItemsToAdjacentInventory(this.itemHandlerBase, slot, 64, pos, facing, false);
            this.preventUpdates = false;

            return ret;
        }

        return false;
    }

    private void breakAdjacentBlocks()
    {
        BlockBreakDropsHandler.setHarvestingInventory(this.itemHandlerBase);

        if (this.isGreedy)
        {
            this.breakBlocksGreedy();
        }
        else
        {
            this.breakBlockInPosition(this.getFrontPosition());
        }

        BlockBreakDropsHandler.setHarvestingInventory(null);
    }

    private void breakBlocksGreedy()
    {
        final EnumFacing facingOpposite = this.getOppositeFacing();
        final EnumFacing facing = this.getFacing();

        switch (Configs.blockBreakerPattern)
        {
            case SHAPE_3x3:
            case SHAPE_5x5:
                EnumFacing side1 = EnumFacing.byIndex((facingOpposite.getIndex() + 2) % 6);
                EnumFacing side2 = EnumFacing.byIndex((facingOpposite.getIndex() + 4) % 6);
                int r = Configs.blockBreakerPattern == Configs.BreakerPattern.SHAPE_5x5 ? 2 : 1;

                for (int front = 0; front <= r; front++)
                {
                    for (int off1 = -r; off1 <= r; off1++)
                    {
                        for (int off2 = -r; off2 <= r; off2++)
                        {
                            // Don't break self
                            if (front != 0 || off1 != 0 || off2 != 0)
                            {
                                int xDiff = side1.getXOffset() * off1 + side2.getXOffset() * off2 + facing.getXOffset() * front;
                                int yDiff = side1.getYOffset() * off1 + side2.getYOffset() * off2 + facing.getYOffset() * front;
                                int zDiff = side1.getZOffset() * off1 + side2.getZOffset() * off2 + facing.getZOffset() * front;
                                BlockPos pos = this.getPos().add(xDiff, yDiff, zDiff);
                                this.breakBlockInPosition(pos);
                            }
                        }
                    }
                }
                break;

            default:
                // By default the Greedy variant breaks blocks on all sides except the back side
                for (EnumFacing facingTmp : EnumFacing.values())
                {
                    if (facingTmp != facingOpposite)
                    {
                        this.breakBlockInPosition(this.getPos().offset(facingTmp));
                    }
                }
                break;
        }
    }

    private void breakBlockInPosition(BlockPos pos)
    {
        World world = this.getWorld();

        if (world.isBlockLoaded(pos, true))
        {
            IBlockState state = world.getBlockState(pos);

            if (state.getBlock().isAir(state, world, pos) == false && state.getBlockHardness(world, pos) >= 0f)
            {
                world.destroyBlock(pos, true);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);
        nbt.setBoolean("Greedy", this.isGreedy);
        nbt.setBoolean("Scheduled", this.breakScheduled);
        nbt.setByte("Delay", (byte) this.delay);
        return nbt;
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.setIsGreedy(nbt.getBoolean("Greedy"));
        this.breakScheduled = nbt.getBoolean("Scheduled");
        this.setDelay(nbt.getByte("Delay"));
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return new ContainerBlockBreaker(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiBlockBreaker(this.getContainer(player), this);
    }
}
