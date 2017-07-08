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
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.ItemType;

public abstract class ItemHandlerWrapperSequenceBase implements IItemHandler, IItemHandlerSize, INBTSerializable<NBTTagCompound>
{
    private final IItemHandler inventoryInput;
    private final IItemHandler inventoryOutput;
    private final SequenceMatcherVariable sequenceReset;
    private final SequenceMatcher sequenceEndMarker;
    private final SequenceManager sequenceManager;
    private final Map<ItemType, List<Integer>> matchingSlotsMap = new HashMap<ItemType, List<Integer>>();
    private State state = State.CONFIGURE;

    public ItemHandlerWrapperSequenceBase(int resetSequenceLength, IItemHandler inventoryInput, IItemHandler inventoryOutput)
    {
        this.inventoryInput  = inventoryInput;
        this.inventoryOutput = inventoryOutput;
        this.sequenceReset      = new SequenceMatcherVariable(resetSequenceLength, "SequenceReset");
        this.sequenceEndMarker  = new SequenceMatcher(1, "SequenceEnd");

        this.sequenceManager = new SequenceManager(true);
        this.sequenceManager.add(this.sequenceEndMarker);
        this.sequenceManager.addResetSequence(this.sequenceReset);
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
        return this.inventoryInput.insertItem(0, stack, simulate);
    }

    public boolean moveItems()
    {
        ItemStack inputStack;

        switch (this.getState())
        {
            case CONFIGURE:
                inputStack = this.getInputInventory().extractItem(0, 1, false);

                if (inputStack.isEmpty() == false)
                {
                    if (this.getSequenceManager().configure(inputStack))
                    {
                        this.onFullyConfigured();
                        this.setState(State.NORMAL);
                    }

                    return true;
                }
                return false;

            case NORMAL:
                inputStack = this.getInputInventory().getStackInSlot(0);

                if (inputStack.isEmpty() == false)
                {
                    if (this.getSequenceManager().checkForReset(inputStack))
                    {
                        this.onReset();
                    }
                    else
                    {
                        return this.moveItemNormal(inputStack);
                    }
                }
                break;

            // The point of this state is to move the reset item to the output first,
            // before starting to flush the sequences.
            case RESET:
                if (this.moveInputItemToOutput())
                {
                    this.setState(State.RESET_FLUSH);
                    return true;
                }
                break;

            case RESET_FLUSH:
                return this.resetFlushItems();

            default:
        }

        return false;
    }

    protected void onFullyConfigured()
    {
    }

    protected void onReset()
    {
        this.matchingSlotsMap.clear();
        this.setState(State.RESET);
    }

    protected void onResetFlushComplete()
    {
        this.setState(State.CONFIGURE);
    }

    protected abstract boolean moveItemNormal(ItemStack stack);

    protected boolean resetFlushItems()
    {
        InvResult result = this.sequenceManager.flushSequencesAndReset(this.inventoryOutput);

        if (result == InvResult.MOVED_ALL)
        {
            this.onResetFlushComplete();
        }

        return result != InvResult.MOVED_NOTHING;
    }

    protected boolean moveInputItemToOutput()
    {
        return this.moveInputItemToInventory(this.inventoryOutput);
    }

    protected boolean moveInputItemToInventory(IItemHandler inv)
    {
        return InventoryUtils.tryMoveStackToOtherInventory(this.getInputInventory(), inv, 0, false) != InvResult.MOVED_NOTHING;
    }

    protected InvResult flushAndResetSequences()
    {
        return this.getSequenceManager().flushSequencesAndReset(this.inventoryOutput);
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

    public IItemHandler getOutputInventory()
    {
        return this.inventoryOutput;
    }

    public IItemHandler getEndMarkerInventory()
    {
        return this.sequenceEndMarker.getSequenceInventory(false);
    }

    protected SequenceManager getSequenceManager()
    {
        return this.sequenceManager;
    }

    public SequenceMatcher getEndMarkerSequence()
    {
        return this.sequenceEndMarker;
    }

    public SequenceMatcherVariable getResetSequence()
    {
        return this.sequenceReset;
    }

    protected State getState()
    {
        return this.state;
    }

    protected void setState(State mode)
    {
        this.state = mode;
    }

    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        return this.sequenceManager.writeToNBT(tag);
    }

    protected void readFromNBT(NBTTagCompound tag)
    {
        this.sequenceManager.readFromNBT(tag);
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setByte("State", (byte) this.getState().getId());
        nbt.setTag("Sequences", this.writeToNBT(new NBTTagCompound()));

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        this.setState(State.fromId(nbt.getByte("State")));
        this.readFromNBT(nbt.getCompoundTag("Sequences"));
    }

    public enum State
    {
        CONFIGURE   (0),
        NORMAL      (1),
        RESET       (2),
        RESET_FLUSH (3);

        private final int id;

        private State (int id)
        {
            this.id = id;
        }

        public int getId()
        {
            return this.id;
        }

        public static State fromId(int id)
        {
            return values()[id % values().length];
        }
    }
}
