package fi.dy.masa.autoverse.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFilterSequential;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityFilterSequential extends TileEntityFilter
{
    protected ItemStackHandlerTileEntity inventoryFilteredBuffer;

    public TileEntityFilterSequential()
    {
        this(ReferenceNames.NAME_BLOCK_FILTER_SEQUENTIAL);
    }

    public TileEntityFilterSequential(String name)
    {
        super(name);
    }

    @Override
    protected void initFilterInventory()
    {
        this.inventoryFilteredBuffer = new ItemStackHandlerTileEntity(3, 18, 1, false, "ItemsFilteredBuffer", this);

        this.inventoryFilter = new ItemHandlerWrapperFilterSequential(
                                    this.inventoryInput,
                                    this.inventoryOutFiltered,
                                    this.inventoryOutNormal,
                                    this.inventoryFilteredBuffer);
    }

    public ItemStackHandlerTileEntity getInventoryFilteredBuffer()
    {
        return this.inventoryFilteredBuffer;
    }

    @Override
    public int getComparatorOutput()
    {
        int output = super.getComparatorOutput();

        if (InventoryUtils.getFirstNonEmptySlot(this.inventoryFilteredBuffer) != -1)
        {
            output |= 0x01;
        }

        return output;
    }

    @Override
    public void dropInventories()
    {
        super.dropInventories();

        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryFilteredBuffer);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.inventoryFilteredBuffer.deserializeNBT(nbt);
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        super.writeItemsToNBT(nbt);

        nbt.merge(this.inventoryFilteredBuffer.serializeNBT());
    }
}
