package fi.dy.masa.autoverse.inventory.wrapper.machines;

import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.NBTUtils;

public class SequenceMatcher
{
    private final NonNullList<ItemStack> sequence;
    private final String tagName;
    private int position;
    private boolean configured;
    @Nullable
    protected ISequenceCallback callback;
    protected int callbackId;

    public SequenceMatcher(int length, String tagName)
    {
        this.sequence = NonNullList.withSize(length, ItemStack.EMPTY);
        this.tagName = tagName;
    }

    public SequenceMatcher setCallback(ISequenceCallback callback, int callbackId)
    {
        this.callback = callback;
        this.callbackId = callbackId;
        return this;
    }

    protected void onSequenceConfigured(ItemStack inputStack)
    {
        this.position = 0;
        this.configured = true;
    }

    /**
     * Called when the sequence is about to be reset, and flushing the items starts.
     * Sets the internal position to 0 to keep track of the flush progress.
     */
    public void flushStart()
    {
        this.position = 0;
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

    /**
     * Flushes all sequence items to the provided <b>inventoryOut</b>, and
     * after all of them have been moved, resets the matcher.
     * @param inventoryOut
     * @return MOVED_ALL after all items have been moved and the matcher reset, otherwise MOVED_NOTHING or MOVED_SOME depending on if an item was moved
     */
    public InvResult flushItemsAndReset(IItemHandler inventoryOut)
    {
        ItemStack stack = this.sequence.get(this.position);
        InvResult result = InvResult.MOVED_NOTHING;

        // Items in the sequence slot
        if (stack.isEmpty() == false)
        {
            stack = InventoryUtils.tryInsertItemStackToInventory(inventoryOut, stack, false);
            this.sequence.set(this.position, stack);

            if (stack.isEmpty())
            {
                result = InvResult.MOVED_SOME;
            }
        }

        // All items moved to the output inventory
        if (stack.isEmpty() && ++this.position >= this.getCurrentSequenceLength())
        {
            this.reset();
            return InvResult.MOVED_ALL;
        }

        return result;
    }

    /**
     * Drops all items from the sequence as EntityItems in the World
     * @param world
     * @param pos
     */
    public void dropAllItems(World world, BlockPos pos)
    {
        for (int slot = 0; slot < this.getCurrentSequenceLength(); slot++)
        {
            EntityUtils.dropItemStacksInWorld(world, pos, this.sequence.get(slot), -1, true);
        }
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

        if (this.callback != null)
        {
            this.callback.onConfigureSequenceSlot(this.callbackId, this.position, (this.position + 1) >= this.sequence.size());
        }

        if (++this.position >= this.sequence.size())
        {
            this.onSequenceConfigured(inputStack);
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
            if (++this.position >= this.getCurrentSequenceLength())
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

    /**
     * Returns whether the sequence has currently been matched. Note that
     * this is normally only ever true for 0-length variable-length sequences!
     * All normal sequence matchers get reset immediately upon match in {@link #checkInputItem(ItemStack)}
     * and thus the return value from that method is the only match indication.
     * @return true if the sequence has been matched
     */
    public boolean isSequenceMatched()
    {
        return this.configured ? this.position >= this.getCurrentSequenceLength() : false;
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
                if (absIndex >= (this.position - 1))
                {
                    // If the current input item matches after the shifted sequence
                    // then the actual position can be set to reflect the shifted sequence.
                    if (InventoryUtils.areItemStacksEqual(inputStack, this.sequence.get(relIndex + 1)))
                    {
                        this.position -= (start - 1);
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

    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("Configured", this.configured);
        tag.setByte("Position", (byte) this.position);
        NBTUtils.writeItemsToTag(tag, this.sequence, "Items", false);
        nbt.setTag(this.tagName, tag);
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt)
    {
        NBTTagCompound tag = nbt.getCompoundTag(this.tagName);
        this.configured = tag.getBoolean("Configured");
        this.position = tag.getByte("Position");
        NBTUtils.readStoredItemsFromTag(tag, this.sequence, "Items");
    }

    public final String getTagName()
    {
        return this.tagName;
    }

    public final boolean isConfigured()
    {
        return this.configured;
    }

    protected final int getPosition()
    {
        return this.position;
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

    public final NonNullList<ItemStack> getSequence()
    {
        return this.sequence;
    }

    public ItemStack getStackInSlot(int slot)
    {
        return this.sequence.get(slot);
    }

    public final int getMaxLength()
    {
        return this.sequence.size();
    }

    public int getCurrentSequenceLength()
    {
        return this.getMaxLength();
    }

    /**
     * Get the number of slots that should be rendered on the GUI for this sequence.
     * For a configured sequence this returns {@link getCurrentSequenceLength()}.
     * For sequences still being configured, the return value is initially the full maximum sequence length,
     * but after the first item has been configured, the value will be the number of items configured thus far.
     * This way, the GUI will initially indicate the maximum sequence length, but will then show the current
     * sequence length after the configuration of a sequence has begun.
     * @return
     */
    public int getSequenceLengthForRender()
    {
        return this.configured ? this.getCurrentSequenceLength() : (this.position == 0 ? this.getMaxLength() : this.position);
    }

    /**
     * Parses the binary value represented by this SequenceMatcher
     * @param highBitMarker the ItemStack that represents a high bit
     * @return the numerical value represented by this SequenceMatcher, or -1 if the sequence length is 0
     */
    public int parseValueFromSequence(ItemStack highBitMarker)
    {
        final int invSize = this.getCurrentSequenceLength();

        if (invSize == 0)
        {
            return -1;
        }
        else
        {
            int value = 0;

            for (int bit = 0; bit < invSize; bit++)
            {
                if (InventoryUtils.areItemStacksEqual(this.sequence.get(bit), highBitMarker))
                {
                    value |= (1 << bit);
                }
            }

            return value;
        }
    }

    public SequenceInventory getSequenceInventory(boolean matched)
    {
        return new SequenceInventory(this, matched);
    }

    public static class SequenceInventory implements IItemHandlerModifiable
    {
        private final SequenceMatcher matcher;
        private final boolean matched;
        private NonNullList<ItemStack> sequence;

        public SequenceInventory(SequenceMatcher matcher, boolean matched)
        {
            this.matcher = matcher;
            this.matched = matched;
            this.sequence = matcher.getSequence();
        }

        public void setSequence(NonNullList<ItemStack> sequence)
        {
            this.sequence = sequence;
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
