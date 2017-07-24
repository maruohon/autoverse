package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
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
    private BlockPos posBack = BlockPos.ORIGIN;
    private boolean isGreedy;
    private int delay = 4;

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
                this.delay = value;
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    public void setIsGreedy(boolean isGreedy)
    {
        this.isGreedy = isGreedy;
        this.markDirty();
    }

    @Override
    public void setPos(BlockPos posIn)
    {
        super.setPos(posIn);

        this.posBack = this.getPos().offset(this.facingOpposite);
    }

    @Override
    public void setFacing(EnumFacing facing)
    {
        super.setFacing(facing);

        this.posBack = this.getPos().offset(this.facingOpposite);
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        if (this.redstoneState == false)
        {
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        if (this.redstoneState == false)
        {
            this.breakAdjacentBlocks();

            if (this.tryPushOutItems())
            {
                this.scheduleBlockUpdate(this.delay, false);
            }
        }
    }

    private boolean tryPushOutItems()
    {
        int slot = InventoryUtils.getFirstNonEmptySlot(this.itemHandlerBase);

        if (slot != -1)
        {
            BlockPos pos = this.posBack;
            TileEntity te = this.getWorld().getTileEntity(pos);

            // If there is no inventory directly behind the breaker, then offset the position one more,
            // in case there is an inventory behind a Frame block for example.
            if (te == null || te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.facing) == false)
            {
                pos = pos.offset(this.facingOpposite);
            }

            return this.pushItemsToAdjacentInventory(this.itemHandlerBase, slot, 64, pos, this.facing, false);
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
            this.breakBlockOnPosition(this.getPos().offset(this.getFacing()));
        }

        BlockBreakDropsHandler.setHarvestingInventory(null);
    }

    private void breakBlocksGreedy()
    {
        switch (Configs.blockBreakerPattern)
        {
            case SHAPE_3x3:
            case SHAPE_5x5:
                EnumFacing side1 = EnumFacing.getFront((this.facingOpposite.getIndex() + 2) % 6);
                EnumFacing side2 = EnumFacing.getFront((this.facingOpposite.getIndex() + 4) % 6);
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
                                int xDiff = side1.getFrontOffsetX() * off1 + side2.getFrontOffsetX() * off2 + this.facing.getFrontOffsetX() * front;
                                int yDiff = side1.getFrontOffsetY() * off1 + side2.getFrontOffsetY() * off2 + this.facing.getFrontOffsetY() * front;
                                int zDiff = side1.getFrontOffsetZ() * off1 + side2.getFrontOffsetZ() * off2 + this.facing.getFrontOffsetZ() * front;
                                BlockPos pos = this.getPos().add(xDiff, yDiff, zDiff);
                                this.breakBlockOnPosition(pos);
                            }
                        }
                    }
                }
                break;

            default:
                // By default the Greedy variant breaks blocks on all sides except the back side
                for (EnumFacing facing : EnumFacing.values())
                {
                    if (facing != this.facingOpposite)
                    {
                        this.breakBlockOnPosition(this.getPos().offset(facing));
                    }
                }
                break;
        }
    }

    private void breakBlockOnPosition(BlockPos pos)
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
        return nbt;
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.setIsGreedy(nbt.getBoolean("Greedy"));
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
