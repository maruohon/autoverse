package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.NBTUtils;

public class SequenceMatcher implements INBTSerializable<NBTTagCompound>
{
    private final NonNullList<ItemStack> sequence;
    private final String tagName;
    private int position;
    private boolean configured;

    public SequenceMatcher(int length, String tagName)
    {
        this.sequence = NonNullList.withSize(length, ItemStack.EMPTY);
        this.tagName = tagName;
    }

    /**
     * Configures the sequence by adding the current input item to it.
     * @return true if the full sequence has been configured
     */
    public boolean configureSequence(ItemStack inputStack)
    {
        // Empty stacks are also valid, but we don't need to copy them,
        // as the list is already filled with empty stacks.
        if (inputStack.isEmpty() == false)
        {
            this.sequence.set(this.position, inputStack.copy());
        }

        if (++this.position >= this.sequence.size())
        {
            this.position = 0;
            this.configured = true;
            return true;
        }

        return false;
    }

    /**
     * Checks the current input item against the configured sequence.
     * When a full sequence has been matched, true is returned, and the internal
     * position will reset back to 0, ready for the next sequence.
     * @return true if the full sequence matches, false otherwise
     */
    public boolean checkInputItem(ItemStack inputStack)
    {
        // The current item matches the sequence
        if (InventoryUtils.areItemStacksEqual(inputStack, this.sequence.get(this.position)))
        {
            if (++this.position >= this.sequence.size())
            {
                this.position = 0;
                return true;
            }
        }
        // The current item breaks an earlier sequence - try to shift the sequence
        // in case there are new valid sequence starts within the matched sequence.
        // Note that since there is no need to actually separately store the matched sequence,
        // this "shifting" is simply finding the new "current position" within the sequence.
        else if (this.position > 0)
        {
            this.shiftSequence(inputStack);
        }

        return false;
    }

    private void shiftSequence(ItemStack inputStack)
    {
        for (int start = 1; start < this.position; ++start)
        {
            for (int relIndex = 0; ; ++relIndex)
            {
                int absIndex = relIndex + start;

                if (InventoryUtils.areItemStacksEqual(this.sequence.get(relIndex), this.sequence.get(absIndex)) == false)
                {
                    break;
                }

                // Valid new sequence up to the matched items
                if (absIndex >= this.position)
                {
                    // If the current input item matches after the shifted sequence
                    // then the actual position can be set to reflect the shifted sequence.
                    if (InventoryUtils.areItemStacksEqual(inputStack, this.sequence.get(relIndex + 1)))
                    {
                        this.position = start;
                        return;
                    }

                    break;
                }
            }
        }

        // No new sequence matches above, reset the position
        this.position = 0;

        // And then still check the current input item against the sequence start
        if (InventoryUtils.areItemStacksEqual(inputStack, this.sequence.get(0)))
        {
            this.position++;
        }
    }

    /**
     * Clears any stored sequences, and resets the internal positions to 0.
     */
    public void reset()
    {
        this.sequence.clear();
        this.position = 0;
        this.configured = false;
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("State", (byte) ((this.configured ? 0x80 : 0x00) | this.position));
        NBTUtils.writeItemsToTag(tag, this.sequence, this.tagName, false);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        int state = nbt.getByte("State");
        this.position = state & 0x7F;
        this.configured = (state & 0x80) != 0;
        NBTUtils.readStoredItemsFromTag(nbt, this.sequence, this.tagName);
    }

    /**
     * Returns the internal position, which indicates the next
     * position in the sequence to be matched (= number of items already matched).
     * If the sequence hasn't yet been configured, then 0 is returned.
     * @return
     */
    public int getCurrentPosition()
    {
        return this.configured ? this.position : 0;
    }

    private NonNullList<ItemStack> getSequence()
    {
        return this.sequence;
    }

    public IItemHandler getSequenceInventory(boolean matched)
    {
        return new SequenceInventory(this, matched);
    }

    public static class SequenceInventory implements IItemHandlerModifiable
    {
        private final SequenceMatcher matcher;
        private final NonNullList<ItemStack> sequence;
        private final boolean matched;

        public SequenceInventory(SequenceMatcher matcher, boolean matched)
        {
            this.matcher = matcher;
            this.sequence = matcher.getSequence();
            this.matched = matched;
        }

        @Override
        public int getSlots()
        {
            return this.sequence.size();
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            if (this.matched)
            {
                return slot < this.matcher.getCurrentPosition() ? this.sequence.get(slot) : ItemStack.EMPTY;
            }
            else
            {
                return this.sequence.get(slot);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack)
        {
            this.sequence.set(slot, stack);
        }
    }
}
