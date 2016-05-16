package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiFilter;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperSelective;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerFilter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityFilter extends TileEntityAutoverseInventory
{
    protected ItemStackHandlerTileEntity inventoryReset;
    protected ItemStackHandlerTileEntity inventoryFilterItems;

    protected ItemStackHandlerTileEntity inventoryFilterered;
    protected ItemStackHandlerTileEntity inventoryOtherOut;
    protected IItemHandler wrappedInventoryFilterered;
    protected IItemHandler wrappedInventoryOtherOut;
    protected ItemHandlerWrapperInput inventoryInput;

    protected EnumFacing facingFilteredOut;
    protected EnumFacing facingFilteredOutOpposite;
    protected BlockPos posFilteredOut;
    protected int filterTier;

    public TileEntityFilter()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_FILTER);
    }

    protected void initInventories()
    {
        System.out.printf("========= inv INIT ============\n");
        this.inventoryReset         = new ItemStackHandlerTileEntity(0, this.getNumResetSlots(),   1, false, "ResetItems", this);
        this.inventoryFilterItems   = new ItemStackHandlerTileEntity(1, this.getNumFilterSlots(),  1, false, "FilterItems", this);
        this.inventoryFilterered    = new ItemStackHandlerTileEntity(2,                       31, 64, false, "FilteredItems", this);
        this.inventoryOtherOut      = new ItemStackHandlerTileEntity(3,                       31, 64, false, "OutputItems", this);
        this.itemHandlerBase        = this.inventoryOtherOut;

        // 31 slots = 9 buffer slots + max 22 reset + filter-item slots when doing a reset cycle

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
        System.out.printf("========= set tier ============\n");
        this.filterTier = MathHelper.clamp_int(tier, 0, 2);

        this.initInventories();
    }

    @Override
    public void setFacing(EnumFacing facing)
    {
        System.out.printf("========= set facing ============\n");
        super.setFacing(facing);

        this.facingFilteredOut = this.getFacing().rotateYCCW();
        this.facingFilteredOutOpposite = this.facingFilteredOut.getOpposite();
        this.posFilteredOut = this.getPos().offset(this.facingFilteredOut);
    }

    @Override
    protected void onRedstoneChange(boolean state)
    {
        if (state == true)
        {
            this.scheduleBlockTick(1);
        }
    }

    @Override
    public void onBlockTick(IBlockState state, Random rand)
    {
        this.tickScheduled = false;

        int slot1 = InventoryUtils.getFirstNonEmptySlot(this.wrappedInventoryOtherOut);
        if (slot1 != -1)
        {
            this.pushItemsToAdjacentInventory(this.wrappedInventoryOtherOut, slot1,
                    this.posFront, this.facingOpposite, this.redstoneState);
        }

        int slot2 = InventoryUtils.getFirstNonEmptySlot(this.wrappedInventoryFilterered);
        if (slot2 != -1)
        {
            this.pushItemsToAdjacentInventory(this.wrappedInventoryFilterered, slot2,
                    this.posFilteredOut, this.facingFilteredOutOpposite, this.redstoneState);
        }

        // Lazy check for if there WERE some items, then schedule a new tick
        if (slot1 != -1 || slot2 != -1)
        {
            this.scheduleBlockTick(1);
        }
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        System.out.printf("========= readFromNBTCustom ============\n");
        super.readFromNBTCustom(tag);

        // Setting the tier and thus initializing the inventories needs to
        // happen before reading the inventories!
        this.setFilterTier(tag.getByte("Tier"));

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
        this.inventoryInput.setMode(EnumMode.fromId(nbt.getByte("m")));

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

    private class ItemHandlerWrapperInput implements IItemHandler, INBTSerializable<NBTTagCompound>
    {
        private final IItemHandler resetItems;
        private final IItemHandler filterItems;
        private final IItemHandler filteredOut;
        private final IItemHandler othersOut;
        private final ItemStackHandlerBasic resetSequenceBuffer;
        private int slotPosition;
        private int seqBufRead;
        private int seqBufWrite;
        private int seqBufLength;
        private EnumMode mode;

        public ItemHandlerWrapperInput(
                IItemHandler resetItems,
                IItemHandler filterItems,
                IItemHandler filteredOut,
                IItemHandler othersOut)
        {
            this.resetItems = resetItems;
            this.filterItems = filterItems;
            this.filteredOut = filteredOut;
            this.othersOut = othersOut;
            this.resetSequenceBuffer = new ItemStackHandlerBasic(this.resetItems.getSlots() + 1, 1, false, "InputItems");
            this.seqBufLength = this.resetItems.getSlots();
            this.slotPosition = 0;
            this.mode = EnumMode.ACCEPT_RESET_ITEMS;

            this.initMode();
        }

        public EnumMode getMode()
        {
            return this.mode;
        }

        public void setMode(EnumMode mode)
        {
            this.mode = mode;
        }

        private void initMode()
        {
            switch (this.mode)
            {
                case ACCEPT_RESET_ITEMS:
                    this.slotPosition = InventoryUtils.getFirstEmptySlot(this.resetItems);
                    if (this.slotPosition < 0)
                    {
                        this.mode = EnumMode.ACCEPT_FILTER_ITEMS;
                        this.slotPosition = 0;
                    }
                    break;
                case ACCEPT_FILTER_ITEMS:
                    this.slotPosition = InventoryUtils.getFirstEmptySlot(this.filterItems);
                    if (this.slotPosition < 0)
                    {
                        this.mode = EnumMode.SORT_ITEMS;
                        this.slotPosition = 0;
                    }
                    break;
                default:
            }
        }

        @Override
        public NBTTagCompound serializeNBT()
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("slotPos", (byte)this.slotPosition);
            tag.setByte("SeqRd", (byte)this.seqBufRead);
            tag.setByte("SeqWr", (byte)this.seqBufWrite);
            tag.merge(this.resetSequenceBuffer.serializeNBT());

            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound tag)
        {
            this.slotPosition = tag.getByte("slotPos");
            this.seqBufRead   = tag.getByte("SeqRd");
            this.seqBufWrite  = tag.getByte("SeqWr");
            this.resetSequenceBuffer.deserializeNBT(tag);
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

        private int incrementSequenceBufferIndex(int index)
        {
            return (index + 1) % this.seqBufLength;
        }

        private boolean sequenceBufferMatches(IItemHandler inv)
        {
            int rd = this.seqBufRead;

            for (int i = 0; i < inv.getSlots() && rd != this.seqBufWrite; i++)
            {
                if (InventoryUtils.areItemStacksEqual(this.resetSequenceBuffer.getStackInSlot(rd), inv.getStackInSlot(i)) == false)
                {
                    return false;
                }

                rd = this.incrementSequenceBufferIndex(rd);
            }

            return true;
        }

        private int getNextSequenceStartIndex(IItemHandler inv)
        {
            int rd = this.seqBufRead;

            for (int i = 0; i < inv.getSlots() && rd != this.seqBufWrite; i++)
            {
                if (InventoryUtils.areItemStacksEqual(this.resetSequenceBuffer.getStackInSlot(rd), inv.getStackInSlot(0)) == true)
                {
                    return rd;
                }

                rd = this.incrementSequenceBufferIndex(rd);
            }

            return rd;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            System.out.printf("%d : insert - simulate: %s slot: %d stack: %s mode: %s pos: %d\n", TileEntityFilter.this.getWorld().getTotalWorldTime(), simulate, slot, stack, this.mode, this.slotPosition);

            switch (this.mode)
            {
                case ACCEPT_RESET_ITEMS:
                    stack = this.resetItems.insertItem(this.slotPosition, stack, simulate);

                    if (simulate == false && ++this.slotPosition >= this.resetItems.getSlots())
                    {
                        this.mode = EnumMode.ACCEPT_FILTER_ITEMS;
                        this.slotPosition = 0;
                    }
                    break;

                case ACCEPT_FILTER_ITEMS:
                    stack = this.filterItems.insertItem(this.slotPosition, stack, simulate);

                    if (simulate == false && ++this.slotPosition >= this.filterItems.getSlots())
                    {
                        this.mode = EnumMode.SORT_ITEMS;
                        this.slotPosition = 0;
                    }
                    break;

                case SORT_ITEMS:
                    if (simulate == false)
                    {
                        if (InventoryUtils.areItemStacksEqual(stack, this.resetItems.getStackInSlot(this.slotPosition)) == true)
                        {
                            // Already storing an input sequence, or on-the-fly matching a reset sequence and the current item
                            // is a possible new start to a reset sequence in case the currently matched sequence doesn't match
                            if (this.seqBufRead != this.seqBufWrite || 
                                (this.slotPosition > 0 && InventoryUtils.areItemStacksEqual(stack, this.resetItems.getStackInSlot(0))))
                            {
                                this.resetSequenceBuffer.setStackInSlot(this.seqBufWrite, stack.copy());
                                this.seqBufWrite = this.incrementSequenceBufferIndex(this.seqBufWrite);
                            }

                            System.out.printf("%d : rst match, slot: %d stack: %s mode: %s pos: %d\n", TileEntityFilter.this.getWorld().getTotalWorldTime(), slot, stack, this.mode, this.slotPosition);
                            if (++this.slotPosition >= this.resetItems.getSlots())
                            {
                                System.out.printf("%d : RESET\n", TileEntityFilter.this.getWorld().getTotalWorldTime());
                                // Dump the reset sequence inventory and the filter item inventory into the output inventory
                                InventoryUtils.tryMoveAllItems(this.resetItems, this.othersOut);
                                InventoryUtils.tryMoveAllItems(this.filterItems, this.othersOut);
                                this.mode = EnumMode.RESET;
                                this.slotPosition = 0;
                                this.seqBufRead = 0;
                                this.seqBufWrite = 0;
                            }
                        }
                        // Encountered an item that breaks the currently monitored sequence
                        else if (this.slotPosition > 0)
                        {
                            while (this.seqBufRead != this.seqBufWrite)
                            {
                                if (this.sequenceBufferMatches(this.resetSequenceBuffer) == false)
                                {
                                    this.seqBufRead = this.getNextSequenceStartIndex(this.resetSequenceBuffer);
                                }
                            }

                            // Set the on-the-fly index to the amount of matching data in the sequence buffer
                            this.slotPosition = (this.seqBufWrite - this.seqBufRead) % this.seqBufLength;
                        }

                        TileEntityFilter.this.scheduleBlockTick(1);
                    }

                    if (InventoryUtils.getSlotOfFirstMatchingItemStack(this.filterItems, stack) != -1)
                    {
                        return InventoryUtils.tryInsertItemStackToInventory(this.filteredOut, stack, simulate);
                    }

                    stack = InventoryUtils.tryInsertItemStackToInventory(this.othersOut, stack, simulate);

                    break;

                case RESET:
                    if (InventoryUtils.isInventoryEmpty(this.filteredOut) && InventoryUtils.isInventoryEmpty(this.othersOut))
                    {
                        this.mode = EnumMode.ACCEPT_RESET_ITEMS;
                    }
                    break;
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
