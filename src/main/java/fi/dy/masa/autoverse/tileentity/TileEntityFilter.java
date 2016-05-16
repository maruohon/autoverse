package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiFilter;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperSelectiveModifiable;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerFilter;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityFilter extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryFilter;
    private ItemStackHandlerTileEntity inventoryOutput;
    private int filterTier;

    public TileEntityFilter()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_FILTER);

        this.initInventories();
    }

    protected void initInventories()
    {
        int confInvSize = this.getNumResetSlots() + this.getNumFilterSlots();
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, confInvSize, 1, false, "ItemsConf", this);
        this.inventoryFilter = new ItemStackHandlerTileEntity(1, 9, 64, false, "ItemsFilt", this);
        this.inventoryOutput = new ItemStackHandlerTileEntity(2, 9, 64, false, "ItemsOut", this);

        this.itemHandlerExternal = new ItemHandlerWrapperExternal(this.itemHandlerBase, this.inventoryFilter, this.inventoryOutput);
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer()
    {
        return this.getBaseItemHandler();
    }

    public IItemHandler getFilteredItemsInventory()
    {
        return this.inventoryFilter;
    }

    public IItemHandler getOutputInventory()
    {
        return this.inventoryOutput;
    }

    public int getNumResetSlots()
    {
        return 2 + this.getFilterTier();
    }

    public int getNumFilterSlots()
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
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
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
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        super.readItemsFromNBT(nbt);

        this.inventoryFilter.deserializeNBT(nbt);
        this.inventoryOutput.deserializeNBT(nbt);
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        super.writeItemsToNBT(nbt);

        nbt.merge(this.inventoryFilter.serializeNBT());
        nbt.merge(this.inventoryOutput.serializeNBT());
    }

    @Override
    public NBTTagCompound getDescriptionPacketTag(NBTTagCompound nbt)
    {
        nbt = super.getDescriptionPacketTag(nbt);
        nbt.setByte("t", (byte)this.getFilterTier());
        return nbt;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet)
    {
        NBTTagCompound nbt = packet.getNbtCompound();
        this.setFilterTier(nbt.getByte("t"));

        super.onDataPacket(net, packet);
    }

    private class ItemHandlerWrapperExternal extends ItemHandlerWrapperSelectiveModifiable
    {
        private final IItemHandlerModifiable filterOut;
        private final IItemHandlerModifiable othersOut;

        public ItemHandlerWrapperExternal(IItemHandlerModifiable configInv,
                IItemHandlerModifiable filterOut, IItemHandlerModifiable othersOut)
        {
            super(configInv);

            this.filterOut = filterOut;
            this.othersOut = othersOut;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return super.getStackInSlot(slot);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack)
        {
            //System.out.printf("setting slot %d (offset: %d) to: %s\n", slot, TileEntityBufferFifo.this.getOffsetSlot(slot), stack);
            super.setStackInSlot(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            //System.out.printf("inserting to slot %d (offset: %d) to: %s\n", slot, TileEntityBufferFifo.this.getOffsetSlot(slot), stack);
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
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
}
