package fi.dy.masa.autoverse.tileentity;

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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperContainer;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerAutoverse;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityAutoverseInventory extends TileEntityAutoverse
{
    protected ItemStackHandlerTileEntity itemHandlerBase;
    protected IItemHandler itemHandlerExternal;
    protected String customInventoryName;

    public TileEntityAutoverseInventory(String name)
    {
        super(name);
    }

    protected void initInventories()
    {
    }

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
     * Returns an inventory wrapper for use in Containers/Slots.<br>
     * <b>NOTE:</b> Override this for any TileEntity that doesn't have a valid
     * IItemHandler in the itemHandlerExternal field!!
     */
    public IItemHandler getWrappedInventoryForContainer()
    {
        return new ItemHandlerWrapperContainer(this.getBaseItemHandler(), this.itemHandlerExternal);
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

        if (nbt.hasKey("CustomName", Constants.NBT.TAG_STRING) == true)
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

        if (this.hasCustomName() == true)
        {
            nbt.setString("CustomName", this.customInventoryName);
        }

        return nbt;
    }

    /**
     * Tries to push items from the given inventory into an adjacent inventory.
     * If there is no adjacent inventory on the front facing side, then it will spawn the item as an EntityItem.
     * @return whether the action succeeded
     */
    protected boolean pushItemsToAdjacentInventory(IItemHandler invSrc, int slot, BlockPos pos, EnumFacing side, boolean spawnInWorld)
    {
        ItemStack stack = invSrc.extractItem(slot, 1, true);

        if (stack != null)
        {
            TileEntity te = this.getWorld().getTileEntity(pos);
            IItemHandler inv = te != null ? te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side) : null;

            if (inv != null)
            {
                // First simulate adding the item, if that succeeds, then actually extract it and insert it into the adjacent inventory
                stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, true);

                // Simulating item insertion succeeded
                if (stack == null)
                {
                    stack = invSrc.extractItem(slot, 1, false);
                    stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);

                    // Failed, try to return the item
                    if (stack != null)
                    {
                        invSrc.insertItem(slot, stack, false);
                    }

                    /*if (Configs.disableSounds == false)
                    {
                        this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.3f, 1f);
                    }*/

                    return true;
                }

                return false;
            }
            else if (spawnInWorld == true)
            {
                // No adjacent inventory, drop the item in world
                stack = invSrc.extractItem(slot, 1, false);

                if (stack == null)
                {
                    return false;
                }

                EntityUtils.dropItemStacksInWorld(this.getWorld(), this.getSpawnedItemPosition(), stack, -1, true, false);

                if (Configs.disableSounds == false)
                {
                    this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.3f, 1f);
                }

                return true;
            }
        }

        return false;
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

    public boolean isUseableByPlayer(EntityPlayer player)
    {
        if (this.getWorld().getTileEntity(this.getPos()) != this)
        {
            return false;
        }

        if (player.getDistanceSq(this.getPos()) >= 64.0d)
        {
            return false;
        }

        return true;
    }

    public void performGuiAction(EntityPlayer player, int action, int element)
    {
    }

    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return null;
    }

    @SideOnly(Side.CLIENT)
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return null;
    }
}
