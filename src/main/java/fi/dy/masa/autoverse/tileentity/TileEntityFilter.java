package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiFilter;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerFilter;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFilter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntityFilter extends TileEntityAutoverseInventory
{
    protected ItemStackHandlerTileEntity inventoryInput;
    protected ItemStackHandlerTileEntity inventoryOutFiltered;
    protected ItemStackHandlerTileEntity inventoryOutNormal;
    protected ItemHandlerWrapperFilter filter;

    protected EnumFacing facingFilteredOut = EnumFacing.WEST;
    protected BlockPos posFilteredOut;
    protected int delay = 1;

    public TileEntityFilter()
    {
        this(ReferenceNames.NAME_BLOCK_FILTER);
    }

    public TileEntityFilter(String name)
    {
        super(name);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput         = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutFiltered   = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOutFiltered", this);
        this.inventoryOutNormal     = new ItemStackHandlerTileEntity(2, 1, 64, false, "ItemsOutNormal", this);
        this.itemHandlerBase        = this.inventoryInput;

        this.initFilterInventory();

        this.itemHandlerExternal = this.filter;
    }

    protected void initFilterInventory()
    {
        this.filter = new ItemHandlerWrapperFilter(
                                    this.inventoryInput,
                                    this.inventoryOutFiltered,
                                    this.inventoryOutNormal);
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 1:
                this.setFilterOutputSide(EnumFacing.getFront(value));
                return true;

            case 2:
                this.delay = value;
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    public IItemHandler getInventoryInput()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOutFiltered()
    {
        return this.inventoryOutFiltered;
    }

    public IItemHandler getInventoryOutNormal()
    {
        return this.inventoryOutNormal;
    }

    public ItemHandlerWrapperFilter getInventoryFilter()
    {
        return this.filter;
    }

    public void setFilterOutputSide(EnumFacing side)
    {
        if (side != this.facing)
        {
            this.facingFilteredOut = side;
            this.posFilteredOut = this.getPos().offset(side);
        }
    }

    /**
     * This returns the filter-out side's facing as what it would be if the non-match-out
     * side was North, which is the default rotation for the model.
     * That way the filter-out side's texture will be placed on the correct face
     * of the non-rotated model, before the primary facing's rotation is applied to the entire model.
     */
    public EnumFacing getFilterOutRelativeFacing()
    {
        return PositionUtils.getRelativeFacing(this.getFacing(), this.facingFilteredOut);
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        ItemStack stack = player.getHeldItem(hand);

        if (stack.isEmpty() && player.isSneaking())
        {
            if (side == this.facingFilteredOut)
            {
                side = side.getOpposite();
            }

            this.setFilterOutputSide(side);
            this.notifyBlockUpdate(this.getPos());
            this.markDirty();
            return true;
        }

        return false;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = false;
        movedOut |= this.pushItemsToAdjacentInventory(this.inventoryOutNormal, 0, this.posFront, this.facingOpposite, false);
        movedOut |= this.pushItemsToAdjacentInventory(this.inventoryOutFiltered, 0, this.posFilteredOut, this.facingFilteredOut.getOpposite(), false);
        boolean movedIn = this.filter.moveItems();

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

    protected void scheduleUpdateIfNeeded(boolean force)
    {
        if (force ||
            this.inventoryInput.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOutNormal.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOutFiltered.getStackInSlot(0).isEmpty() == false)
        {
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    public int getComparatorOutput()
    {
        int output = 0;

        if (this.filter.isFullyConfigured())
        {
            output |= 0x08;
        }

        if (InventoryUtils.getFirstNonEmptySlot(this.getInventoryOutFiltered()) != -1)
        {
            output |= 0x04;
        }

        if (InventoryUtils.getFirstNonEmptySlot(this.getInventoryOutNormal()) != -1)
        {
            output |= 0x02;
        }

        return output;
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.setFilterOutputSide(EnumFacing.getFront(nbt.getByte("FilterFacing")));
        this.delay = ((int) nbt.getByte("Delay")) & 0xFF;

        this.inventoryInput.deserializeNBT(nbt);
        this.inventoryOutNormal.deserializeNBT(nbt);
        this.inventoryOutFiltered.deserializeNBT(nbt);

        this.filter.deserializeNBT(nbt);
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("FilterFacing", (byte)this.facingFilteredOut.getIndex());
        nbt.setByte("Delay", (byte) this.delay);

        return nbt;
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        super.writeItemsToNBT(nbt);

        nbt.merge(this.inventoryInput.serializeNBT());
        nbt.merge(this.inventoryOutNormal.serializeNBT());
        nbt.merge(this.inventoryOutFiltered.serializeNBT());

        nbt.merge(this.filter.serializeNBT());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag.setByte("f", (byte)((this.facingFilteredOut.getIndex() << 4) | this.facing.getIndex()));
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        int facings = tag.getByte("f");
        this.setFacing(EnumFacing.getFront(facings & 0x7));
        this.setFilterOutputSide(EnumFacing.getFront(facings >> 4));

        super.handleUpdateTag(tag);
    }

    @Override
    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutNormal);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutFiltered);
        this.filter.dropAllItems(this.getWorld(), this.getPos());
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        this.scheduleUpdateIfNeeded(true);
    }

    @Override
    public ContainerFilter getContainer(EntityPlayer player)
    {
        return new ContainerFilter(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiFilter(this.getContainer(player), this);
    }
}
