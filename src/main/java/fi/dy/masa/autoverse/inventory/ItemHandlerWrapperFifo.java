package fi.dy.masa.autoverse.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerWrapperFifo implements IItemHandler, INBTSerializable<NBTTagCompound>
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
        return 1;
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

        this.insertSlot = nbt.getByte("InsertPos");
        this.extractSlot = nbt.getByte("ExtractPos");
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return this.baseHandler.getStackInSlot(this.extractSlot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        int sizeOrig = stack.stackSize;

        ItemStack stackRet = this.baseHandler.insertItem(this.insertSlot, stack, simulate);

        if (simulate == false && (stackRet == null || stackRet.stackSize != sizeOrig) && ++this.insertSlot >= this.baseHandler.getSlots())
        {
            this.insertSlot = 0;
        }

        return stackRet;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        ItemStack stackRet = this.baseHandler.extractItem(this.extractSlot, amount, simulate);

        if (simulate == false && stackRet != null && ++this.extractSlot >= this.baseHandler.getSlots())
        {
            this.extractSlot = 0;
        }

        return stackRet;
    }
}
