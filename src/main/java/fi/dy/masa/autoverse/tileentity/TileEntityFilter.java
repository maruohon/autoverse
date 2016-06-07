package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiFilter;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperExtractOnly;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperFilter;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerFilter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntityFilter extends TileEntityAutoverseInventory
{
    protected ItemStackHandlerTileEntity inventoryInputManual;
    protected ItemStackHandlerTileEntity inventoryReset;
    protected ItemStackHandlerTileEntity inventoryFilterItems;

    protected ItemStackHandlerTileEntity inventoryFilterered;
    protected ItemStackHandlerTileEntity inventoryOtherOut;
    protected IItemHandler wrappedInventoryFilterered;
    protected IItemHandler wrappedInventoryOtherOut;
    protected ItemHandlerWrapperFilter inventoryInput;

    protected EnumFacing facingFilteredOut;
    protected BlockPos posFilteredOut;
    protected int filterTier;

    public TileEntityFilter()
    {
        this(ReferenceNames.NAME_TILE_ENTITY_FILTER);

        this.facingFilteredOut = EnumFacing.WEST;
        this.initInventories();
    }

    public TileEntityFilter(String name)
    {
        super(name);
    }

    @Override
    protected void initInventories()
    {
        int rst = this.getNumResetSlots();
        int flt = this.getNumFilterSlots();
        int fltBuf = this.getFilterBufferSize();
        this.inventoryInputManual   = new ItemStackHandlerTileEntity(9,             1,  1, false, "InputItems", this);
        this.inventoryReset         = new ItemStackHandlerTileEntity(0,           rst,  1, false, "ResetItems", this);
        this.inventoryFilterItems   = new ItemStackHandlerTileEntity(1,           flt,  1, false, "FilterItems", this);
        this.inventoryFilterered    = new ItemStackHandlerTileEntity(2,        fltBuf,  1, false, "FilteredItems", this);
        this.inventoryOtherOut      = new ItemStackHandlerTileEntity(3, rst + flt + 9,  1, false, "OutputItems", this);
        this.itemHandlerBase        = this.inventoryOtherOut;

        this.wrappedInventoryFilterered    = new ItemHandlerWrapperExtractOnly(this.inventoryFilterered);
        this.wrappedInventoryOtherOut      = new ItemHandlerWrapperExtractOnly(this.inventoryOtherOut);
    }

    protected void initFilterInventory()
    {
        this.inventoryInput = new ItemHandlerWrapperFilter(this.inventoryReset, this.inventoryFilterItems,
                                     this.inventoryFilterered, this.inventoryOtherOut, this);
    }

    public int getNumResetSlots()
    {
        int tier = this.getFilterTier();

        switch (tier)
        {
            case 0: return 2;
            case 1: return 2;
            case 2: return 3;
            case 3: return 4;
            case 4: return 4;
            default: return 1;
        }
    }

    protected int getFilterBufferSize()
    {
        return this.getNumFilterSlots();
    }

    public int getNumFilterSlots()
    {
        int tier = this.getFilterTier();

        switch (tier)
        {
            case 0: return 1;
            case 1: return 3;
            case 2: return 6;
            case 3: return 9;
            case 4: return 18;
            default: return 1;
        }
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer()
    {
        return this.getBaseItemHandler();
    }

    public IItemHandler getInputInventory()
    {
        return this.inventoryInputManual;
    }

    public IItemHandler getResetInventory()
    {
        return this.inventoryReset;
    }

    public IItemHandler getResetSequenceBuffer()
    {
        return this.inventoryInput.getSequenceBuffer();
    }

    public IItemHandler getFilterItemsInventory()
    {
        return this.inventoryFilterItems;
    }

    public IItemHandler getFilteredItemsInventory()
    {
        return this.inventoryFilterered;
    }

    public IItemHandler getOutputInventory()
    {
        return this.inventoryOtherOut;
    }

    public int getFilterTier()
    {
        return this.filterTier;
    }

    public void setFilterTier(int tier)
    {
        this.filterTier = MathHelper.clamp_int(tier, 0, 4);

        this.initInventories();
        this.initFilterInventory();
    }

    public void setFilterOutputSide(EnumFacing side)
    {
        if (side != this.facing)
        {
            this.facingFilteredOut = side;
            this.posFilteredOut = this.getPos().offset(side);
        }
    }

    public EnumFacing getFilterOutRelativeFacing()
    {
        switch (this.facing)
        {
            // North is the default model rotation, don't modify the filter-out for this facing
            case NORTH: return this.facingFilteredOut;
            case SOUTH:
                if (this.facingFilteredOut.getAxis().isHorizontal())
                {
                    return this.facingFilteredOut.getOpposite();
                }
                return this.facingFilteredOut;
            default:
                EnumFacing axis = PositionUtils.getCWRotationAxis(EnumFacing.NORTH, this.facing).getOpposite();

                if (this.facingFilteredOut.getAxis() != axis.getAxis())
                {
                    EnumFacing result = PositionUtils.rotateAround(this.facingFilteredOut, axis);
                    //System.out.printf("facing: %s axis: %s filter: %s result: %s\n", facing, axis, facingFilteredOut, result);
                    return result;
                }

                return facingFilteredOut;
        }
    }

    @Override
    protected void onRedstoneChange(boolean state)
    {
        if (state == true)
        {
            this.scheduleBlockTick(1, true);
        }
    }

    protected boolean tryOutputNonMatchingItems()
    {
        int slot = InventoryUtils.getFirstNonEmptySlot(this.wrappedInventoryOtherOut);

        if (slot != -1)
        {
            for ( ; slot < this.wrappedInventoryOtherOut.getSlots(); slot++)
            {
                if (this.pushItemsToAdjacentInventory(this.wrappedInventoryOtherOut, slot,
                        this.posFront, this.facingOpposite, this.redstoneState) == true)
                {
                    break;
                }
            }
        }

        return slot != -1;
    }

    protected boolean tryOutputFilteredItems()
    {
        int slot = InventoryUtils.getFirstNonEmptySlot(this.wrappedInventoryFilterered);

        if (slot != -1)
        {
            //System.out.printf("block tick - pos: %s\n", this.getPos());
            for ( ; slot < this.wrappedInventoryFilterered.getSlots(); slot++)
            {
                if (this.pushItemsToAdjacentInventory(this.wrappedInventoryFilterered, slot,
                        this.posFilteredOut, this.facingFilteredOut.getOpposite(), this.redstoneState) == true)
                {
                    break;
                }
            }
        }

        return slot != -1;
    }

    @Override
    public void onBlockTick(IBlockState state, Random rand)
    {
        super.onBlockTick(state, rand);

        // Items in the manual input inventory, try to pull them in
        if (this.inventoryInputManual.getStackInSlot(0) != null)
        {
            InventoryUtils.tryMoveAllItems(this.inventoryInputManual, this.inventoryInput);
        }

        boolean flag1 = this.tryOutputNonMatchingItems();
        boolean flag2 = this.tryOutputFilteredItems();

        // Lazy check for if there WERE some items, then schedule a new tick
        if (flag1 || flag2)
        {
            this.scheduleBlockTick(4, false);
        }
        // The usefulness of this is questionable... It speeds up the transition from RESET by one item insertion attempt
        else if (this.inventoryInput.getMode() == ItemHandlerWrapperFilter.EnumMode.RESET)
        {
            this.inventoryInput.setMode(ItemHandlerWrapperFilter.EnumMode.ACCEPT_RESET_ITEMS);
        }
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        super.readFromNBTCustom(tag);

        // Setting the tier and thus initializing the inventories needs to
        // happen before reading the inventories!
        this.setFilterTier(tag.getByte("Tier"));
        this.setFilterOutputSide(EnumFacing.getFront(tag.getByte("FilterFacing")));

        this.inventoryInputManual.deserializeNBT(tag);
        this.inventoryReset.deserializeNBT(tag);
        this.inventoryFilterItems.deserializeNBT(tag);
        this.inventoryFilterered.deserializeNBT(tag);
        this.inventoryOtherOut.deserializeNBT(tag);
        this.inventoryInput.deserializeNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("Tier", (byte)this.getFilterTier());
        nbt.setByte("FilterFacing", (byte)this.facingFilteredOut.getIndex());
        nbt.merge(this.inventoryInput.serializeNBT());

        return nbt;
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        // Do nothing here, see readFromNBTCustom() above...
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        super.writeItemsToNBT(nbt);

        nbt.merge(this.inventoryInputManual.serializeNBT());
        nbt.merge(this.inventoryReset.serializeNBT());
        nbt.merge(this.inventoryFilterItems.serializeNBT());
        nbt.merge(this.inventoryFilterered.serializeNBT());
        nbt.merge(this.inventoryOtherOut.serializeNBT());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        //tag.setByte("m", (byte)this.inventoryInput.getMode().getId());
        tag.setByte("f", (byte)((this.facingFilteredOut.getIndex() << 4) | this.facing.getIndex()));
        tag.setByte("t", (byte)this.getFilterTier());
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        //this.inventoryInput.setMode(ItemHandlerWrapperFilter.EnumMode.fromId(tag.getByte("m")));
        int facings = tag.getByte("f");
        this.setFacing(EnumFacing.getFront(facings & 0x7));
        this.setFilterOutputSide(EnumFacing.getFront(facings >> 4));

        this.setFilterTier(tag.getByte("t"));

        IBlockState state = this.worldObj.getBlockState(this.getPos());
        this.worldObj.notifyBlockUpdate(this.getPos(), state, state, 3);

        super.handleUpdateTag(tag);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return true;
        }

        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            if (facing == this.facing)
            {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.wrappedInventoryOtherOut);
            }

            if (facing == this.facingFilteredOut)
            {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.wrappedInventoryFilterered);
            }

            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.inventoryInput);
        }

        return super.getCapability(capability, facing);
    }

    public void dropInventories()
    {
        EntityUtils.dropAllItemInWorld(this.getWorld(), this.getPos(), this.inventoryReset, true, true);
        EntityUtils.dropAllItemInWorld(this.getWorld(), this.getPos(), this.inventoryFilterItems, true, true);
        EntityUtils.dropAllItemInWorld(this.getWorld(), this.getPos(), this.inventoryFilterered, true, true);
        EntityUtils.dropAllItemInWorld(this.getWorld(), this.getPos(), this.inventoryOtherOut, true, true);
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        // Manual input inventory
        if (inventoryId == 9)
        {
            this.scheduleBlockTick(1, true);
        }
    }

    @Override
    public ContainerFilter getContainer(EntityPlayer player)
    {
        return new ContainerFilter(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiFilter(this.getContainer(player), this);
    }
}
