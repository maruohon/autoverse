package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.INBTSerializable;
import fi.dy.masa.autoverse.inventory.IItemHandlerSize;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public class ItemHandlerWrapperFifo implements IItemHandlerSize, INBTSerializable<NBTTagCompound>
{
    protected final ItemStackHandlerTileEntity baseHandler;
    protected int insertSlot;
    protected int extractSlot;

    public ItemHandlerWrapperFifo(ItemStackHandlerTileEntity baseHandler)
    {
        this.baseHandler = baseHandler;
    }

    public int getInsertSlot()
    {
        return this.insertSlot;
    }

    public int getExtractSlot()
    {
        return this.extractSlot;
    }

    @Override
    public int getSlots()
    {
        return 2;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return this.baseHandler.getSlotLimit(slot);
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        if (slot == 0)
        {
            return this.baseHandler.getStackInSlot(this.extractSlot);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        int sizeOrig = stack.getCount();

        stack = this.baseHandler.insertItem(this.insertSlot, stack, simulate);

        if (simulate == false && (stack.isEmpty() || stack.getCount() != sizeOrig))
        {
            this.onSuccessfulInsert();
        }

        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        ItemStack stack = this.baseHandler.extractItem(this.extractSlot, amount, simulate);

        if (simulate == false && stack.isEmpty() == false)
        {
            this.onSuccessfulExtract();
        }

        return stack;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return this.baseHandler.getInventoryStackLimit();
    }

    @Override
    public int getItemStackLimit(int slot, ItemStack stack)
    {
        return this.baseHandler.getItemStackLimit(slot, stack);
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = this.baseHandler.serializeNBT();

        nbt.setByte("InsertPos", (byte)this.insertSlot);
        nbt.setByte("ExtractPos", (byte)this.extractSlot);

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        this.baseHandler.deserializeNBT(nbt);

        this.insertSlot = MathHelper.clamp(nbt.getByte("InsertPos"), 0, this.baseHandler.getSlots() - 1);
        this.extractSlot = MathHelper.clamp(nbt.getByte("ExtractPos"), 0, this.baseHandler.getSlots() - 1);
    }

    /**
     * Wrap the insert and extract positions to 0 if they are over the end of the inventory.
     * This is only needed when changing the inventory size via the GUI buttons.
     */
    public void wrapPositions()
    {
        if (this.insertSlot >= this.baseHandler.getSlots())
        {
            this.insertSlot = 0;
        }

        if (this.extractSlot >= this.baseHandler.getSlots())
        {
            this.extractSlot = 0;
        }
    }

    protected void onSuccessfulInsert()
    {
        if (++this.insertSlot >= this.baseHandler.getSlots())
        {
            this.insertSlot = 0;
        }
    }

    protected void onSuccessfulExtract()
    {
        if (++this.extractSlot >= this.baseHandler.getSlots())
        {
            this.extractSlot = 0;
        }
    }
}
