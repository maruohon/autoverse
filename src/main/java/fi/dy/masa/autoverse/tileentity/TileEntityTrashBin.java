package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.autoverse.gui.client.GuiTrashBin;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerTrashBin;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class TileEntityTrashBin extends TileEntityAutoverseInventory
{
    protected long trashedCount;

    public TileEntityTrashBin()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_TRASH_BIN);
    }

    public TileEntityTrashBin(String name)
    {
        super(name);
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemHandlerTrashBin(0, 1, this);
        this.itemHandlerExternal = this.itemHandlerBase;
    }

    public long getTrashedCount()
    {
        return this.trashedCount;
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);
        this.trashedCount = nbt.getLong("TrashedCount");
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt.setLong("TrashedCount", this.trashedCount);
        return super.writeToNBTCustom(nbt);
    }

    private class ItemHandlerTrashBin extends ItemStackHandlerTileEntity
    {
        public ItemHandlerTrashBin(int inventoryId, int invSize, TileEntityAutoverseInventory te)
        {
            super(inventoryId, invSize, te);
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (simulate == false)
            {
                TileEntityTrashBin.this.trashedCount += stack.getCount();
            }

            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ContainerTrashBin getContainer(EntityPlayer player)
    {
        return new ContainerTrashBin(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiTrashBin(this.getContainer(player), this, false);
    }
}
