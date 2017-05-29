package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.IItemHandlerSize;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public abstract class ItemHandlerWrapperSequenceBase implements IItemHandler, IItemHandlerSize, INBTSerializable<NBTTagCompound>
{
    private final ItemStackHandlerTileEntity inventoryInput;
    private final SequenceMatcher sequenceReset;

    public ItemHandlerWrapperSequenceBase(int sequenceLength, ItemStackHandlerTileEntity inventoryInput)
    {
        this.inventoryInput = inventoryInput;
        this.sequenceReset   = new SequenceMatcher(sequenceLength, "SequenceReset");
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 1;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 1;
    }

    @Override
    public int getItemStackLimit(int slot, ItemStack stack)
    {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return this.inventoryInput.getStackInSlot(0);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        stack = this.inventoryInput.insertItem(0, stack, simulate);

        if (simulate == false && stack.isEmpty())
        {
            this.handleInputItem(this.inventoryInput.getStackInSlot(0));
        }

        return stack;
    }

    protected abstract void handleInputItem(ItemStack inputStack);

    public IItemHandler getInputInventory()
    {
        return this.inventoryInput;
    }

    public SequenceMatcher getResetSequence()
    {
        return this.sequenceReset;
    }

    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        this.sequenceReset.writeToNBT(tag);

        return tag;
    }

    protected void readFromNBT(NBTTagCompound tag)
    {
        this.sequenceReset.readFromNBT(tag);
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Splitter", this.writeToNBT(new NBTTagCompound()));

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        this.readFromNBT(nbt.getCompoundTag("Splitter"));
    }
}
