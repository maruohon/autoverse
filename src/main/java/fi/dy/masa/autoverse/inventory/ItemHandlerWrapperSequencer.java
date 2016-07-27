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

    public int getOutputSlot()
    {
        return this.outputSlot;
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
        slot = InventoryUtils.getNextNonEmptySlot(this.baseHandler, this.outputSlot);
        return slot != -1 ? this.baseHandler.getStackInSlot(slot) : null;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return InventoryUtils.tryInsertItemStackToInventory(this.baseHandler, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        slot = InventoryUtils.getNextNonEmptySlot(this.baseHandler, this.outputSlot);

        if (slot == -1)
        {
            return null;
        }

        ItemStack stackRet = this.baseHandler.extractItem(slot, 1, simulate);

        if (simulate == false && stackRet != null)
        {
            slot = InventoryUtils.getNextNonEmptySlot(this.baseHandler, slot + 1);

            if (slot != -1)
            {
                this.outputSlot = slot;
            }
        }

        return stackRet;
    }
}
