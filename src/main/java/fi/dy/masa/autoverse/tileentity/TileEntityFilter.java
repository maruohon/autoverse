package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
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
    protected EnumFacing facingFilteredOutOpposite;
    protected BlockPos posFilteredOut;
    protected int filterTier;

    public TileEntityFilter()
    {
        this(ReferenceNames.NAME_TILE_ENTITY_FILTER);

        //this.setFilterTier(2); // just-in-case init
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
        this.inventoryInputManual   = new ItemStackHandlerTileEntity(9,             1,  1, false, "InputItems", this);
        this.inventoryReset         = new ItemStackHandlerTileEntity(0,           rst,  1, false, "ResetItems", this);
        this.inventoryFilterItems   = new ItemStackHandlerTileEntity(1,           flt,  1, false, "FilterItems", this);
        this.inventoryFilterered    = new ItemStackHandlerTileEntity(2,       flt + 9, 64, false, "FilteredItems", this);
        this.inventoryOtherOut      = new ItemStackHandlerTileEntity(3, rst + flt + 9, 64, false, "OutputItems", this);
        this.itemHandlerBase        = this.inventoryOtherOut;

        this.wrappedInventoryFilterered    = new ItemHandlerWrapperExtractOnly(this.inventoryFilterered);
        this.wrappedInventoryOtherOut      = new ItemHandlerWrapperExtractOnly(this.inventoryOtherOut);
    }

    protected void initFilterInventory()
    {
        this.inventoryInput = new ItemHandlerWrapperFilter(this.inventoryReset, this.inventoryFilterItems,
                                     this.inventoryFilterered, this.inventoryOtherOut, this);
    }

    protected int getNumResetSlots()
    {
        return 2 + this.getFilterTier();
    }

    protected int getNumFilterSlots()
    {
        int tier = this.getFilterTier();
        if (tier == 2)
        {
            return 18;
        }

        return tier == 1 ? 9 : 1;
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
        this.filterTier = MathHelper.clamp_int(tier, 0, 2);

        this.initInventories();
        this.initFilterInventory();
    }

    @Override
    public void setFacing(EnumFacing facing)
    {
        super.setFacing(facing);

        if (facing.getAxis().isHorizontal() == true)
        {
            this.facingFilteredOut = facing.rotateYCCW();
        }
        else
        {
            // FIXME add 24-way rotation?
            this.facingFilteredOut = EnumFacing.WEST;
        }

        this.facingFilteredOutOpposite = this.facingFilteredOut.getOpposite();
        this.posFilteredOut = this.getPos().offset(this.facingFilteredOut);
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
                        this.posFilteredOut, this.facingFilteredOutOpposite, this.redstoneState) == true)
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

        this.inventoryInputManual.deserializeNBT(tag);
        this.inventoryReset.deserializeNBT(tag);
        this.inventoryFilterItems.deserializeNBT(tag);
        this.inventoryFilterered.deserializeNBT(tag);
        this.inventoryOtherOut.deserializeNBT(tag);
        this.inventoryInput.deserializeNBT(tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);

        tag.setByte("Tier", (byte)this.getFilterTier());
        tag.merge(this.inventoryInput.serializeNBT());
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
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
    public NBTTagCompound getDescriptionPacketTag(NBTTagCompound nbt)
    {
        nbt = super.getDescriptionPacketTag(nbt);
        nbt.setByte("t", (byte)this.getFilterTier());
        nbt.setByte("m", (byte)this.inventoryInput.getMode().getId());
        return nbt;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet)
    {
        NBTTagCompound nbt = packet.getNbtCompound();

        this.setFilterTier(nbt.getByte("t"));
        this.inventoryInput.setMode(ItemHandlerWrapperFilter.EnumMode.fromId(nbt.getByte("m")));

        super.onDataPacket(net, packet);
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
