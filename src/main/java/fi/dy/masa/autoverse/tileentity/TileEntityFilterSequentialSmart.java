package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiFilterSequential;
import fi.dy.masa.autoverse.reference.ReferenceNames;

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
        //this.wrappedInventoryFilterered = new ExtractWrapper(this.inventoryOutputFiltered, this);
    }

    @Override
    protected void initFilterInventory()
    {
        super.initFilterInventory();
    }

    /*
    private class ExtractWrapper extends ItemHandlerWrapperExtractOnly
    {
        public ExtractWrapper(IItemHandler baseInventory, TileEntityFilterSequentialSmart te)
        {
            super(baseInventory);
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
            if (TileEntityFilterSequentialSmart.this.inventoryInput.getMode() != EnumMode.OUTPUT_ITEMS)
            {
                return ItemStack.EMPTY;
            }

            slot = InventoryUtils.getFirstNonEmptySlot(this.parent);
            return this.parent.extractItem(slot >= 0 ? slot : 0, amount, simulate);
        }
    }
    */

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiFilterSequential(this.getContainer(player), this, true);
    }
}
