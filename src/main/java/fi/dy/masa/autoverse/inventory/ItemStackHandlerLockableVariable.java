package fi.dy.masa.autoverse.inventory;

import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class ItemStackHandlerLockableVariable extends ItemStackHandlerLockable
{
    private int invSize;

    public ItemStackHandlerLockableVariable(int inventoryId, int invSize, int stackLimit, boolean allowCustomStackSizes,
            String tagName, TileEntityAutoverseInventory te)
    {
        super(inventoryId, invSize, stackLimit, allowCustomStackSizes, tagName, te);

        this.invSize = invSize;
    }

    @Override
    public int getSlots()
    {
        return this.invSize;
    }

    public void setInventorySize(int invSize)
    {
        this.invSize = invSize;
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = super.serializeNBT();
        nbt.setByte("SlotCount", (byte) this.invSize);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        super.deserializeNBT(nbt);

        this.invSize = nbt.getByte("SlotCount");
    }
}
