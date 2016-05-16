package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiFilter;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperSelective;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerFilter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityFilter extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryReset;
    private ItemStackHandlerTileEntity inventoryFilterItems;

    private ItemStackHandlerTileEntity inventoryFilterered;
    private ItemStackHandlerTileEntity inventoryOtherOut;
    private IItemHandler wrappedInventoryFilterered;
    private IItemHandler wrappedInventoryOtherOut;
    private IItemHandler inventoryInput;

    private EnumFacing facingFilteredOut;
    private int filterTier;
    private EnumMode mode;

    public TileEntityFilter()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_FILTER);
        this.mode = EnumMode.ACCEPT_FILTER_ITEMS;
    }

    protected void initInventories()
    {
        this.inventoryReset         = new ItemStackHandlerTileEntity(0, this.getNumResetSlots(),   1, false, "ResetItems", this);
        this.inventoryFilterItems   = new ItemStackHandlerTileEntity(1, this.getNumFilterSlots(),  1, false, "FilterItems", this);
        this.inventoryFilterered    = new ItemStackHandlerTileEntity(2,                        9, 64, false, "FilteredItems", this);
        this.inventoryOtherOut      = new ItemStackHandlerTileEntity(3,                        9, 64, false, "OutputItems", this);
        this.itemHandlerBase        = this.inventoryOtherOut;

        this.wrappedInventoryFilterered    = new ItemHandlerWrapperOutputBuffer(this.inventoryFilterered);
        this.wrappedInventoryOtherOut      = new ItemHandlerWrapperOutputBuffer(this.inventoryOtherOut);
        this.inventoryInput                = new ItemHandlerWrapperInput(
                                                this.inventoryReset,
                                                this.inventoryFilterItems,
                                                this.inventoryFilterered,
                                                this.inventoryOtherOut);
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer()
    {
        return this.getBaseItemHandler();
    }

    public IItemHandler getResetInventory()
    {
        return this.inventoryReset;
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

    private int getNumResetSlots()
    {
        return 2 + this.getFilterTier();
    }

    private int getNumFilterSlots()
    {
        int tier = this.getFilterTier();
        if (tier == 2)
        {
            return 18;
        }

        return tier == 1 ? 9 : 1;
    }

    public int getFilterTier()
    {
        return this.filterTier;
    }

    public void setFilterTier(int tier)
    {
        this.filterTier = MathHelper.clamp_int(tier, 0, 2);

        this.initInventories();
    }

    @Override
    public void setFacing(EnumFacing facing)
    {
        super.setFacing(facing);

        this.facingFilteredOut = this.getFacing().rotateYCCW();
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        // The order of this is important, it needs to be before initInventories() (called via setFilterTier())
        this.mode = EnumMode.fromId(nbt.getByte("Mode"));

        // Setting the tier and thus initializing the inventories needs to
        // happen before reading the inventories!
        this.setFilterTier(nbt.getByte("Tier"));

        super.readFromNBTCustom(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("Tier", (byte)this.getFilterTier());
        nbt.setByte("Mode", (byte)this.mode.getId());
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        super.readItemsFromNBT(nbt);

        this.inventoryFilterered.deserializeNBT(nbt);
        this.inventoryOtherOut.deserializeNBT(nbt);
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        super.writeItemsToNBT(nbt);

        nbt.merge(this.inventoryFilterered.serializeNBT());
        nbt.merge(this.inventoryOtherOut.serializeNBT());
    }

    @Override
    public NBTTagCompound getDescriptionPacketTag(NBTTagCompound nbt)
    {
        nbt = super.getDescriptionPacketTag(nbt);
        nbt.setByte("t", (byte)this.getFilterTier());
        nbt.setByte("m", (byte)this.mode.getId());
        return nbt;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet)
    {
        NBTTagCompound nbt = packet.getNbtCompound();
        this.mode = EnumMode.fromId(nbt.getByte("m"));

        this.setFilterTier(nbt.getByte("t"));

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

    private class ItemHandlerWrapperInput extends ItemHandlerWrapperSelective
    {
        private final IItemHandler resetItems;
        private final IItemHandler filterItems;
        private final IItemHandler filteredOut;
        private final IItemHandler othersOut;
        private int slotPosition;

        public ItemHandlerWrapperInput(
                IItemHandler resetItems,
                IItemHandler filterItems,
                IItemHandler filteredOut,
                IItemHandler othersOut)
        {
            super(othersOut);

            this.resetItems = resetItems;
            this.filterItems = filterItems;
            this.filteredOut = filteredOut;
            this.othersOut = othersOut;
            this.slotPosition = 0;

            this.initMode();
        }

        private void initMode()
        {
            switch (TileEntityFilter.this.mode)
            {
                case ACCEPT_RESET_ITEMS:
                    this.slotPosition = InventoryUtils.getFirstEmptySlot(this.resetItems);
                    if (this.slotPosition < 0)
                    {
                        TileEntityFilter.this.mode = EnumMode.ACCEPT_FILTER_ITEMS;
                        this.slotPosition = 0;
                    }
                    break;
                case ACCEPT_FILTER_ITEMS:
                    this.slotPosition = InventoryUtils.getFirstEmptySlot(this.filterItems);
                    if (this.slotPosition < 0)
                    {
                        TileEntityFilter.this.mode = EnumMode.SORT_ITEMS;
                        this.slotPosition = 0;
                    }
                    break;
                default:
            }
        }

        @Override
        public int getSlots()
        {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return null;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return null;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            System.out.printf("%d : insert - simulate: %s slot: %d stack: %s mode: %s pos: %d\n", TileEntityFilter.this.getWorld().getTotalWorldTime(), simulate, slot, stack, TileEntityFilter.this.mode, this.slotPosition);

            switch (TileEntityFilter.this.mode)
            {
                case ACCEPT_RESET_ITEMS:
                    stack = this.resetItems.insertItem(this.slotPosition, stack, simulate);

                    if (simulate == false && ++this.slotPosition >= this.resetItems.getSlots())
                    {
                        TileEntityFilter.this.mode = EnumMode.ACCEPT_FILTER_ITEMS;
                        this.slotPosition = 0;
                    }
                    break;

                case ACCEPT_FILTER_ITEMS:
                    stack = this.filterItems.insertItem(this.slotPosition, stack, simulate);

                    if (simulate == false && ++this.slotPosition >= this.filterItems.getSlots())
                    {
                        TileEntityFilter.this.mode = EnumMode.SORT_ITEMS;
                        this.slotPosition = 0;
                    }
                    break;

                case SORT_ITEMS:
                    // FIXME this won't work, we need a temporary compare buffer
                    if (simulate == false)
                    {
                        if (InventoryUtils.areItemStacksEqual(stack, this.resetItems.getStackInSlot(this.slotPosition)) == true)
                        {
                            System.out.printf("%d : rst match, slot: %d stack: %s mode: %s pos: %d\n", TileEntityFilter.this.getWorld().getTotalWorldTime(), slot, stack, TileEntityFilter.this.mode, this.slotPosition);
                            if (++this.slotPosition >= this.resetItems.getSlots())
                            {
                                System.out.printf("%d : RESET\n", TileEntityFilter.this.getWorld().getTotalWorldTime());
                                TileEntityFilter.this.mode = EnumMode.RESET;
                                this.slotPosition = 0;
                            }
                        }
                        else
                        {
                            this.slotPosition = 0;
                        }
                    }

                    if (InventoryUtils.getSlotOfFirstMatchingItemStack(this.filterItems, stack) != -1)
                    {
                        return InventoryUtils.tryInsertItemStackToInventory(this.filteredOut, stack, simulate);
                    }

                    stack = InventoryUtils.tryInsertItemStackToInventory(this.othersOut, stack, simulate);

                default:
            }

            //System.out.printf("inserting to slot %d (offset: %d) to: %s\n", slot, TileEntityBufferFifo.this.getOffsetSlot(slot), stack);
            return stack;
        }
    }

    private class ItemHandlerWrapperOutputBuffer extends ItemHandlerWrapperSelective
    {
        public ItemHandlerWrapperOutputBuffer(IItemHandler baseInventory)
        {
            super(baseInventory);
        }

        @Override
        public int getSlots()
        {
            return super.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            // TODO: cache the index?
            //slot = InventoryUtils.getFirstNonEmptySlot(this.baseHandler);
            return super.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            //System.out.printf("inserting to slot %d (offset: %d) to: %s\n", slot, TileEntityBufferFifo.this.getOffsetSlot(slot), stack);
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            // TODO: cache the index?
            //slot = InventoryUtils.getFirstNonEmptySlot(this.baseHandler);
            return super.extractItem(slot, amount, simulate);
        }
    }

    @Override
    public ContainerFilter getContainer(EntityPlayer player)
    {
        return new ContainerFilter(player, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiFilter(this.getContainer(player), this);
    }

    public enum EnumMode
    {
        ACCEPT_RESET_ITEMS (0),
        ACCEPT_FILTER_ITEMS (1),
        SORT_ITEMS (2),
        RESET (3);

        private final int id;

        EnumMode(int id)
        {
            this.id = id;
        }

        public int getId()
        {
            return this.id;
        }

        public static EnumMode fromId(int id)
        {
            return values()[MathHelper.clamp_int(id, 0, values().length - 1)];
        }
    }
}
