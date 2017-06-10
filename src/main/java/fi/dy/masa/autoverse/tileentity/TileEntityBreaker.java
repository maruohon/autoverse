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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import fi.dy.masa.autoverse.event.BlockBreakDropsHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiBreaker;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBreaker;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperExtractOnly;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityBreaker extends TileEntityAutoverseInventory
{
    private BlockPos posBack = BlockPos.ORIGIN;
    private boolean isGreedy;
    private int delay = 4;

    public TileEntityBreaker()
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
            this.breakAdjacentBlocks(this.isGreedy);

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

    private void breakAdjacentBlocks(boolean allSides)
    {
        BlockBreakDropsHandler.setHarvestingInventory(this.itemHandlerBase);

        if (allSides)
        {
            // The Greedy variant breaks blocks on all sides except the back side
            for (EnumFacing facing : EnumFacing.values())
            {
                if (facing != this.facingOpposite)
                {
                    this.breakBlockOnPosition(this.getPos().offset(facing));
                }
            }
        }
        else
        {
            this.breakBlockOnPosition(this.getPos().offset(this.getFacing()));
        }

        BlockBreakDropsHandler.setHarvestingInventory(null);
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
        return new ContainerBreaker(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiBreaker(this.getContainer(player), this);
    }
}
