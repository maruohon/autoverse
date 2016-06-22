package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiFilterSequential;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperExtractOnly;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperFilter.EnumMode;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperFilterSequentialSmart;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityFilterSequentialSmart extends TileEntityFilterSequential
{
    public TileEntityFilterSequentialSmart()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_FILTER_SEQ_SMART);
    }

    @Override
    protected void initInventories()
    {
        super.initInventories();

        // Replace the wrapper with one that preserves the item output order
        this.wrappedInventoryFilterered = new ExtractWrapper(this.inventoryFilterered, this);
    }

    @Override
    protected void initFilterInventory()
    {
        this.inventoryInputSequential = new ItemHandlerWrapperFilterSequentialSmart(
                this.inventoryReset,
                this.inventoryFilterItems,
                this.inventoryFilterered,
                this.inventoryNonmatchOut,
                this);

        this.inventoryInput = this.inventoryInputSequential;
    }

    private class ExtractWrapper extends ItemHandlerWrapperExtractOnly
    {
        private final TileEntityFilterSequentialSmart te;

        public ExtractWrapper(IItemHandler baseInventory, TileEntityFilterSequentialSmart te)
        {
            super(baseInventory);
            this.te = te;
        }

        @Override
        public int getSlots()
        {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            slot = InventoryUtils.getFirstNonEmptySlot(this.parent);
            return this.parent.getStackInSlot(slot >= 0 ? slot : 0);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            // Only allow extracting after the filter sequence is complete and the filter enters reset mode
            if (this.te.inventoryInput.getMode() != EnumMode.OUTPUT_ITEMS)
            {
                return null;
            }

            slot = InventoryUtils.getFirstNonEmptySlot(this.parent);
            return this.parent.extractItem(slot >= 0 ? slot : 0, amount, simulate);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiFilterSequential(this.getContainer(player), this, true);
    }
}
