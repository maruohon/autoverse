package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class SequenceManager
{
    private final List<SequenceMatcher> sequences = new ArrayList<SequenceMatcher>();
    @Nullable
    private SequenceMatcher sequenceReset;
    private final boolean firstIsEndMarker;
    private int position;
    private int state;

    public SequenceManager(boolean firstSequenceIsEndMarker)
    {
        this.firstIsEndMarker = firstSequenceIsEndMarker;
    }

    public SequenceManager add(SequenceMatcher sequence)
    {
        this.sequences.add(sequence);
        return this;
    }

    public SequenceManager add(SequenceMatcher sequence, int position)
    {
        this.sequences.add(position, sequence);
        return this;
    }

    public SequenceManager addResetSequence(SequenceMatcher sequence)
    {
        this.sequenceReset = sequence;
        return this.add(sequence);
    }

    /**
     * Configures all sequences in the order the sequences were added to the manager.
     * If firstIsEndMarker is true, then after the first sequence has been configured,
     * any SequenceMatcherVariable type sequences will be configured with that end marker item.
     * @param stack
     * @return true after all sequences have been configured
     */
    public boolean configure(ItemStack stack)
    {
        if (this.sequences.get(this.position).configureSequence(stack))
        {
            if (this.position == 0 && this.firstIsEndMarker)
            {
                for (SequenceMatcher sequence : this.sequences)
                {
                    if (sequence instanceof SequenceMatcherVariable)
                    {
                        ((SequenceMatcherVariable) sequence).setSequenceEndMarker(stack);
                    }
                }
            }

            if (++this.position >= this.sequences.size())
            {
                this.position = 0;
                return true;
            }
        }

        return false;
    }

    public boolean checkForReset(ItemStack stack)
    {
        boolean result = this.sequenceReset != null && this.sequenceReset.checkInputItem(stack);

        if (result)
        {
            this.position = 0;
            this.state = 0;
        }

        return result;
    }

    /**
     * Flushes all items from all sequences to the given inventory <b>inventoryOut</b>.
     * After all the stacks from each sequence have been flushed out, the sequence is then reset. 
     * @param inventoryOut
     * @return MOVED_SOME if an item was moved to the output, MOVED_ALL after all sequences have been flushed and reset
     */
    public InvResult flushSequencesAndReset(IItemHandler inventoryOut)
    {
        if (this.state == 0)
        {
            for (SequenceMatcher sequence : this.sequences)
            {
                sequence.flushStart();
            }

            this.state = 1;
        }

        SequenceMatcher sequence = this.sequences.get(this.position);
        InvResult result = sequence.flushItemsAndReset(inventoryOut);

        if (result == InvResult.MOVED_ALL)
        {
            if (++this.position >= this.sequences.size())
            {
                this.position = 0;
                this.state = 0;
            }
            else
            {
                // Don't return MOVED_ALL until all the sequences have been flushed!
                result = InvResult.MOVED_SOME;
            }
        }

        return result;
    }

    public void dropAllItems(World world, BlockPos pos)
    {
        for (SequenceMatcher sequence : this.sequences)
        {
            sequence.dropAllItems(world, pos);
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag.setByte("Position", (byte) this.position);
        tag.setByte("State", (byte) this.state);

        for (SequenceMatcher sequence : this.sequences)
        {
            sequence.writeToNBT(tag);
        }

        return tag;
    }

    public void readFromNBT(NBTTagCompound tag)
    {
        this.position = tag.getByte("Position");
        this.state = tag.getByte("State");

        for (SequenceMatcher sequence : this.sequences)
        {
            sequence.readFromNBT(tag);
        }
    }
}
