package fi.dy.masa.autoverse.inventory;

import fi.dy.masa.autoverse.tileentity.TileEntityAutoverseInventory;

public class ItemStackHandlerTileEntity extends ItemStackHandlerBasic
{
    protected final TileEntityAutoverseInventory te;
    protected final int inventoryId;

    public ItemStackHandlerTileEntity(int invSize, TileEntityAutoverseInventory te)
    {
        this(0, invSize, te);
    }

    public ItemStackHandlerTileEntity(int inventoryId, int invSize, TileEntityAutoverseInventory te)
    {
        super(invSize);
        this.te = te;
        this.inventoryId = inventoryId;
    }

    public ItemStackHandlerTileEntity(int inventoryId, int invSize, int stackLimit, boolean allowCustomStackSizes, String tagName, TileEntityAutoverseInventory te)
    {
        super(invSize, stackLimit, allowCustomStackSizes, tagName);
        this.te = te;
        this.inventoryId = inventoryId;
    }

    @Override
    public void onContentsChanged(int slot)
    {
        super.onContentsChanged(slot);

        this.te.inventoryChanged(this.inventoryId, slot);
        this.te.markDirty();
    }
}
