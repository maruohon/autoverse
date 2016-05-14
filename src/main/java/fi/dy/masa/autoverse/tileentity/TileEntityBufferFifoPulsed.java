package fi.dy.masa.autoverse.tileentity;

import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperSelective;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityBufferFifoPulsed extends TileEntityBufferFifo
{
    public TileEntityBufferFifoPulsed()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_PULSED);
        // Start extracting from slot 1, so that slot 0 will be extracted last (ie. after one full cycle through the inventory)
        this.extractSlot = 1;
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, NUM_SLOTS, 1, false, "Items", this);
        this.itemHandlerExternal = new ItemHandlerWrapperFifoPulsed(this.getBaseItemHandler());
    }

    @Override
    protected boolean onRedstonePulse()
    {
        ItemStack stack = this.itemHandlerExternal.getStackInSlot(0);
        TileEntity te = this.worldObj.getTileEntity(this.posFront);
        boolean ret = false;

        if (stack != null)
        {
            IItemHandler inv = te != null ? te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.facingOpposite) : null;

            if (inv != null)
            {
                // First simulate adding the item, if that succeeds, then actually extract it and insert it into the adjacent inventory
                // TODO Add a version of the method that doesn't try to stack first
                stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, true);

                if (stack == null)
                {
                    stack = this.itemHandlerExternal.extractItem(0, 1, false);
                    InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);

                    if (Configs.disableSounds == false)
                    {
                        this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.3f, 1f);
                    }

                    ret = true;
                }
            }
            else
            {
                // No adjacent inventory, drop the item in world
                stack = this.itemHandlerExternal.extractItem(0, 1, false);
                EntityUtils.dropItemStacksInWorld(this.worldObj, this.getItemPosition(), stack, -1, true, false);

                if (Configs.disableSounds == false)
                {
                    this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.3f, 1f);
                }

                ret = true;
            }
        }

        if (++this.insertSlot >= this.itemHandlerBase.getSlots())
        {
            this.insertSlot = 0;
        }

        if (++this.extractSlot >= this.itemHandlerBase.getSlots())
        {
            this.extractSlot = 0;
        }

        return ret;
    }

    private class ItemHandlerWrapperFifoPulsed extends ItemHandlerWrapperSelective
    {
        public ItemHandlerWrapperFifoPulsed(IItemHandler baseHandler)
        {
            super(baseHandler);
        }

        @Override
        public int getSlots()
        {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return super.getStackInSlot(TileEntityBufferFifoPulsed.this.extractSlot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            return super.insertItem(TileEntityBufferFifoPulsed.this.insertSlot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return super.extractItem(TileEntityBufferFifoPulsed.this.extractSlot, amount, simulate);
        }
    }
}
