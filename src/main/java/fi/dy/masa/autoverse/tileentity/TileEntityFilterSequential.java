package fi.dy.masa.autoverse.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
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
        this.inventoryFilteredBuffer = new ItemStackHandlerTileEntity(3, this.getFilterSlotCount(), 1, false, "ItemsFilteredBuffer", this);

        this.inventoryFilter = new ItemHandlerWrapperFilterSequential(
                                    this.getResetSlotCount(),
                                    this.getFilterSlotCount(),
                                    this.inventoryInput,
                                    this.inventoryOutFiltered,
                                    this.inventoryOutNormal,
                                    this.inventoryFilteredBuffer);
    }

    public IItemHandler getInventoryFilteredBuffer()
    {
        return this.inventoryFilteredBuffer;
    }

    @Override
    protected int getMaxFilterTier()
    {
        return 2;
    }

    @Override
    public int getResetSlotCount()
    {
        int tier = this.getFilterTier();

        switch (tier)
        {
            case 0: return 2;
            case 1: return 3;
            case 2: return 4;
            default: return 2;
        }
    }

    @Override
    public int getFilterSlotCount()
    {
        int tier = this.getFilterTier();

        switch (tier)
        {
            case 0: return 4;
            case 1: return 9;
            case 2: return 18;
            default: return 4;
        }
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
