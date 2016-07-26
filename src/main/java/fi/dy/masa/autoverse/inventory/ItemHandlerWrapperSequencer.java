package fi.dy.masa.autoverse.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class ItemHandlerWrapperSequencer implements IItemHandler, INBTSerializable<NBTTagCompound>
{
    protected final ItemStackHandlerTileEntity baseHandler;
    protected int outputSlot;

    public ItemHandlerWrapperSequencer(ItemStackHandlerTileEntity baseHandler)
    {
        this.baseHandler = baseHandler;
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = this.baseHandler.serializeNBT();

        nbt.setByte("OutSlot", (byte) this.outputSlot);

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        this.baseHandler.deserializeNBT(nbt);

        this.outputSlot = nbt.getByte("OutSlot");
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return this.baseHandler.getStackInSlot(this.outputSlot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return InventoryUtils.tryInsertItemStackToInventory(this.baseHandler, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        ItemStack stackRet = this.baseHandler.extractItem(this.outputSlot, 1, simulate);

        if (simulate == false && stackRet != null && ++this.outputSlot >= this.baseHandler.getSlots())
        {
            this.outputSlot = 0;
        }

        return stackRet;
    }
}
