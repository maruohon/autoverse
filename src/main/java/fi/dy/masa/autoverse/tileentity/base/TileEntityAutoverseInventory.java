package fi.dy.masa.autoverse.tileentity.base;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperContainer;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.NBTUtils;

public abstract class TileEntityAutoverseInventory extends TileEntityAutoverse
{
    protected ItemStackHandlerTileEntity itemHandlerBase;
    protected IItemHandler itemHandlerExternal;
    protected String customInventoryName;
    protected boolean useWrapperHandlerForContainerExtract;

    public TileEntityAutoverseInventory(String name)
    {
        this(name, false);
    }

    public TileEntityAutoverseInventory(String name, boolean useWrapperHandlerForContainerExtract)
    {
        super(name);

        this.useWrapperHandlerForContainerExtract = useWrapperHandlerForContainerExtract;
        this.initInventories();
    }

    protected void initInventories() { }

    public void setInventoryName(String name)
    {
        this.customInventoryName = name;
    }

    public boolean hasCustomName()
    {
        return this.customInventoryName != null && this.customInventoryName.length() > 0;
    }

    public String getName()
    {
        return this.hasCustomName() ? this.customInventoryName : Reference.MOD_ID + ".container." + this.tileEntityName;
    }

    /**
     * Returns the "base" IItemHandler that this TileEntity uses to store items into NBT when it saves.
     */
    public ItemStackHandlerBasic getBaseItemHandler()
    {
        return this.itemHandlerBase;
    }

    /**
     * Returns the inventory that the Inventory Readers should use to calculate their output signal
     * @param side
     * @return
     */
    public IItemHandler getInventoryForInventoryReader(EnumFacing side)
    {
        return this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
    }

    /**
     * Returns an inventory wrapper for use in Containers/Slots.<br>
     * <b>NOTE:</b> Override this for any TileEntity that doesn't have a valid
     * IItemHandler in the itemHandlerExternal field!!
     */
    public IItemHandler getWrappedInventoryForContainer(EntityPlayer player)
    {
        return new ItemHandlerWrapperContainer(this.getBaseItemHandler(),
                this.itemHandlerExternal, this.useWrapperHandlerForContainerExtract);
    }

    public boolean isCreative()
    {
        return false;
    }

    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        this.getBaseItemHandler().deserializeNBT(nbt);
    }

    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        nbt.merge(this.getBaseItemHandler().serializeNBT());
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        if (nbt.hasKey("CustomName", Constants.NBT.TAG_STRING))
        {
            this.customInventoryName = nbt.getString("CustomName");
        }

        this.readItemsFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        this.writeItemsToNBT(nbt);

        if (this.hasCustomName())
        {
            nbt.setString("CustomName", this.customInventoryName);
        }

        return nbt;
    }

    /**
     * Stores a cached snapshot of the current inventory in a compound tag <b>InvCache</b>.
     * It is meant for tooltip use in the ItemBlocks.
     * @param nbt
     * @return
     */
    public NBTTagCompound getCachedInventory(NBTTagCompound nbt, int maxEntries)
    {
        IItemHandler inv = this.getBaseItemHandler();

        if (inv != null)
        {
            nbt = NBTUtils.storeCachedInventory(nbt, inv, maxEntries);
        }

        return nbt;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return this.itemHandlerExternal != null;
        }

        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.itemHandlerExternal);
        }

        return super.getCapability(capability, facing);
    }

    public void inventoryChanged(int inventoryId, int slot) { }

    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.getBaseItemHandler());
    }

    /**
     * Tries to push 1 item from the given inventory and slot into an adjacent inventory.
     * If there is no adjacent inventory on the front facing side and spawnInWorld is true,
     * then it will spawn the item as an EntityItem.
     * @return whether the action succeeded
     */
    protected boolean pushItemsToAdjacentInventory(IItemHandler invSrc, int slot, BlockPos targetPos, EnumFacing targetSide, boolean spawnInWorld)
    {
        return this.pushItemsToAdjacentInventory(invSrc, slot, 1, targetPos, targetSide, spawnInWorld);
    }

    /**
     * Tries to push at most <b>maxAmount</b> items from the given inventory into an adjacent inventory.
     * If there is no adjacent inventory on the front facing side and spawnInWorld is true,
     * then it will spawn the item as an EntityItem.
     * @return whether the action succeeded
     */
    protected boolean pushItemsToAdjacentInventory(IItemHandler invSrc, int slot, int maxAmount, BlockPos targetPos, EnumFacing targetSide, boolean spawnInWorld)
    {
        if (this.getWorld().isBlockLoaded(targetPos, true) == false)
        {
            return false;
        }

        ItemStack stack = invSrc.extractItem(slot, maxAmount, true);

        if (stack.isEmpty() == false)
        {
            TileEntity te = this.getWorld().getTileEntity(targetPos);
            IItemHandler inv = te != null ? te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, targetSide) : null;

            if (inv != null)
            {
                int sizeOrig = stack.getCount();
                stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);
                int sizeNew = stack.getCount();

                // Successfully inserted at least some items
                if (stack.isEmpty() || sizeNew != sizeOrig)
                {
                    // Actually extract the items, based on how many were successfully inserted
                    invSrc.extractItem(slot, sizeOrig - sizeNew, false);

                    /*if (Configs.disableSounds == false)
                    {
                        this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.3f, 1f);
                    }*/

                    return true;
                }
            }
            else if (spawnInWorld)
            {
                // No adjacent inventory, drop the item in world
                stack = invSrc.extractItem(slot, maxAmount, false);

                if (stack.isEmpty() == false)
                {
                    EntityUtils.dropItemStacksInWorld(this.getWorld(), this.getSpawnedItemPosition(), stack, -1, true, false);

                    if (Configs.disableSounds == false)
                    {
                        this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.3f, 1f);
                    }

                    return true;
                }
            }
        }

        return false;
    }
}
