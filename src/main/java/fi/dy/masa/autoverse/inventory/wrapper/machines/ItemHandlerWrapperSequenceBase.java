package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.IItemHandlerSize;
import fi.dy.masa.autoverse.util.ItemType;

public abstract class ItemHandlerWrapperSequenceBase implements IItemHandler, IItemHandlerSize, INBTSerializable<NBTTagCompound>
{
    private final IItemHandler inventoryInput;
    private final SequenceMatcherVariable sequenceReset;
    private final SequenceMatcher sequenceEndMarker;
    private final Map<ItemType, List<Integer>> matchingSlotsMap = new HashMap<ItemType, List<Integer>>();

    public ItemHandlerWrapperSequenceBase(int resetSequenceLength, IItemHandler inventoryInput)
    {
        this.inventoryInput = inventoryInput;
        this.sequenceReset      = new SequenceMatcherVariable(resetSequenceLength, "SequenceReset");
        this.sequenceEndMarker  = new SequenceMatcher(1, "SequenceEnd");
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

    public boolean moveItems()
    {
        return false;
    }

    protected void onReset()
    {
        this.sequenceEndMarker.reset();
        this.sequenceReset.reset();
        this.matchingSlotsMap.clear();
    }

    protected void createMatchingSlotsMap(NonNullList<ItemStack> items)
    {
        this.matchingSlotsMap.clear();
        final int length = items.size();

        for (int slot = 0; slot < length; ++slot)
        {
            this.addItemTypeToMap(slot, items.get(slot));
        }
    }

    private void addItemTypeToMap(int slot, ItemStack stack)
    {
        ItemType itemType = new ItemType(stack);
        List<Integer> list = this.matchingSlotsMap.get(itemType);

        if (list == null)
        {
            list = new ArrayList<Integer>();
            this.matchingSlotsMap.put(itemType, list);
        }

        list.add(slot);
    }

    @Nullable
    protected List<Integer> getMatchingSlots(ItemStack stack)
    {
        return this.matchingSlotsMap.get(new ItemType(stack));
    }

    public IItemHandler getInputInventory()
    {
        return this.inventoryInput;
    }

    public SequenceMatcher getEndMarkerSequence()
    {
        return this.sequenceEndMarker;
    }

    public SequenceMatcherVariable getResetSequence()
    {
        return this.sequenceReset;
    }

    public IItemHandler getEndMarkerInventory()
    {
        return this.sequenceEndMarker.getSequenceInventory(false);
    }

    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        this.sequenceEndMarker.writeToNBT(tag);
        this.sequenceReset.writeToNBT(tag);
        return tag;
    }

    protected void readFromNBT(NBTTagCompound tag)
    {
        this.sequenceEndMarker.readFromNBT(tag);
        this.sequenceReset.readFromNBT(tag);
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Sequences", this.writeToNBT(new NBTTagCompound()));

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        this.readFromNBT(nbt.getCompoundTag("Sequences"));
    }
}
