package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.gui.client.GuiTrashBin;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerTrashBuffer;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityTrashBuffer extends TileEntityTrashBin
{
    public TileEntityTrashBuffer()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_TRASH_BUFFER);
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemHandlerTrashBuffer(0, 36, 1024, this);
        this.itemHandlerExternal = this.itemHandlerBase;
    }

    private class ItemHandlerTrashBuffer extends ItemStackHandlerTileEntity
    {
        private final TileEntityTrashBuffer te;

        public ItemHandlerTrashBuffer(int inventoryId, int invSize, int maxStackSize, TileEntityTrashBuffer te)
        {
            super(inventoryId, invSize, maxStackSize, true, "Items", te);

            this.te = te;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            ItemStack stackExisting = this.getStackInSlot(slot);

            // Don't try to stack into different items
            if (stackExisting.isEmpty() == false && InventoryUtils.areItemStacksEqual(stack, stackExisting) == false)
            {
                return stack;
            }

            stack = super.insertItem(slot, stack, simulate);

            if (simulate == false)
            {
                this.te.trashedCount += stack.getCount();
            }

            return ItemStack.EMPTY;
        }
    }

    @Override
    public ContainerTrashBuffer getContainer(EntityPlayer player)
    {
        return new ContainerTrashBuffer(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiTrashBin(this.getContainer(player), this, true);
    }
}
