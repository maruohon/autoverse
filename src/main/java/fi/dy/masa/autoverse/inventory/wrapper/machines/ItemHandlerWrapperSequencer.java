package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
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
    public int getSlotLimit(int slot)
    {
        return this.baseHandler.getSlotLimit(slot);
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
        return slot != -1 ? this.baseHandler.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return InventoryUtils.tryInsertItemStackToInventory(this.baseHandler, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        int nextSlot = InventoryUtils.getNextNonEmptySlot(this.baseHandler, this.outputSlot);

        if (nextSlot == -1)
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = this.baseHandler.extractItem(nextSlot, 1, simulate);

        if (simulate == false && stack.isEmpty() == false)
        {
            if (++nextSlot >= this.baseHandler.getSlots())
            {
                nextSlot = 0;
            }

            this.outputSlot = nextSlot;
        }

        return stack;
    }
}
