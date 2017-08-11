package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.NBTUtils;

public class ItemHandlerWrapperFilterSequentialStrict extends ItemHandlerWrapperFilter
{
    private final NonNullList<ItemStack> buffer = NonNullList.create();
    private int filterLength;
    private int position;
    private int flushPosition;
    private int amountToFlush;
    private int subState = 0;

    public ItemHandlerWrapperFilterSequentialStrict(
            int maxFilterLength,
            ItemStackHandlerTileEntity inventoryInput,
            ItemStackHandlerTileEntity inventoryOutFiltered,
            ItemStackHandlerTileEntity inventoryOutNormal)
    {
        super(maxFilterLength, inventoryInput, inventoryOutFiltered, inventoryOutNormal);
    }

    @Override
    protected void onFullyConfigured()
    {
        this.filterLength = this.getFilterSequence().getCurrentSequenceLength();
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        // Store the filter sequence first, because we need to flush any buffered items AFTER
        // the sequences have been flushed and cleared already.
        NonNullList<ItemStack> sequence = this.getFilterSequence().getSequence();

        for (int slot = 0; slot < this.position; slot++)
        {
            this.buffer.add(sequence.get(slot).copy());
        }

        this.position = 0;
        this.flushPosition = 0;
        this.subState = 0;

        // First flush the filter sequence
        this.amountToFlush = this.filterLength;
    }

    @Override
    protected void onResetFlushComplete()
    {
        // Change to flush the filtered items buffer
        this.subState = 1;
        this.amountToFlush = this.buffer.size();
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        switch (this.subState)
        {
            case 0: // Sorting
                return this.sortItem(stack);

            default:
                return this.onScheduledTick();
        }
    }

    @Override
    protected boolean onScheduledTick()
    {
        switch (this.subState)
        {
            case 1: // Outputting a full sequence to the filter-out side
                return this.outputFilteredItems();

            case 2: // Outputting a partial sequence after a mismatch, to the normal output side
                return this.flushFilterBufferItems();

            default:
                return false;
        }
    }

    @Override
    protected boolean resetFlushItems()
    {
        switch (this.subState)
        {
            case 0:
                return super.resetFlushItems();

            case 1:
            {
                InvResult result = this.flushItemsFromSequence(this.buffer, this.getOutputInventory());

                if (result == InvResult.MOVED_ALL)
                {
                    this.buffer.clear();
                    this.setState(State.CONFIGURE);
                }

                return result != InvResult.MOVED_NOTHING;
            }
        }

        return false;
    }

    private boolean outputFilteredItems()
    {
        return this.flushBufferedItems(this.inventoryFilteredOut) != InvResult.MOVED_NOTHING;
    }

    private boolean flushFilterBufferItems()
    {
        return this.flushBufferedItems(this.getOutputInventory()) != InvResult.MOVED_NOTHING;
    }

    private boolean sortItem(ItemStack stack)
    {
        // Match the items against the current filter position.
        // Note: The item not actually stored anywhere separately, they are tracked using the position variable alone!
        if (InventoryUtils.areItemStacksEqual(stack, this.getFilterSequence().getStackInSlot(this.position)))
        {
            this.getInputInventory().extractItem(0, 1, false);

            if (++this.position >= this.filterLength)
            {
                this.position = 0;
                this.flushPosition = 0;
                this.amountToFlush = this.filterLength;
                this.subState = 1;
            }

            return true;
        }
        else if (this.position > 0)
        {
            int newPos = SequenceMatcher.shiftSequence(stack, this.getFilterSequence().getSequence(), this.position);

            if (newPos > 0)
            {
                // There will always be at least one item to flush out!
                // If the position didn't change at all, it was because the input item matches
                // the first item in the sequence, and the matched sequence was only one item long previously.
                this.amountToFlush = this.position - newPos + 1;

                // Valid new sequence partial match, consume (or "virtually store") the current input item
                this.getInputInventory().extractItem(0, 1, false);
            }
            else
            {
                this.amountToFlush = this.position;
            }

            this.position = newPos;
            this.subState = 2;
            this.flushPosition = 0;

            return true;
        }
        else
        {
            // If the item didn't fit or belong to the filtered buffer, move it to the normal output
            return this.moveInputItemToOutput();
        }
    }

    private InvResult flushBufferedItems(IItemHandler outputInv)
    {
        return this.flushItemsFromSequence(this.getFilterSequence().getSequence(), outputInv);
    }

    private InvResult flushItemsFromSequence(NonNullList<ItemStack> sequence, IItemHandler outputInv)
    {
        if (this.flushPosition >= this.amountToFlush || this.flushPosition >= sequence.size())
        {
            this.amountToFlush = 0;
            this.flushPosition = 0;
            this.subState = 0;
            return InvResult.MOVED_ALL;
        }

        ItemStack stack = sequence.get(this.flushPosition).copy();

        if (InventoryUtils.tryInsertItemStackToInventory(outputInv, stack, false).isEmpty())
        {
            if (++this.flushPosition >= this.amountToFlush)
            {
                this.amountToFlush = 0;
                this.flushPosition = 0;
                this.subState = 0;
                return InvResult.MOVED_ALL;
            }

            return InvResult.MOVED_SOME;
        }

        return InvResult.MOVED_NOTHING;
    }

    @Override
    public void dropAllItems(World world, BlockPos pos)
    {
        super.dropAllItems(world, pos);

        switch (this.getState())
        {
            case NORMAL:
                for (int slot = this.flushPosition; slot < this.position; slot++)
                {
                    EntityUtils.dropItemStacksInWorld(world, pos, this.getFilterSequence().getStackInSlot(slot), -1, true);
                }
                break;

            case RESET:
            case RESET_FLUSH:
                for (int slot = this.flushPosition; slot < this.buffer.size(); slot++)
                {
                    EntityUtils.dropItemStacksInWorld(world, pos, this.buffer.get(slot), -1, true);
                }
                break;

            default:
                break;
        }
    }

    public int getMatchedLength()
    {
        return this.getState() == State.NORMAL ? this.position : 0;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.subState = tag.getByte("SubState");
        this.position = tag.getByte("OutPos");
        this.filterLength = tag.getByte("FilterLength");
        this.amountToFlush = tag.getByte("FlushAmount");
        this.flushPosition = tag.getByte("FlushPos");

        if (tag.hasKey("FilterBuffer", Constants.NBT.TAG_LIST))
        {
            this.buffer.clear();
            this.buffer.addAll(NBTUtils.readStoredItemsFromTag(tag, "FilterBuffer"));
        }
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("SubState", (byte) this.subState);
        tag.setByte("OutPos", (byte) this.position);
        tag.setByte("FilterLength", (byte) this.filterLength);
        tag.setByte("FlushAmount", (byte) this.amountToFlush);
        tag.setByte("FlushPos", (byte) this.flushPosition);

        if (this.buffer.size() > 0)
        {
            NBTUtils.writeItemsToTag(tag, this.buffer, "FilterBuffer", false);
        }

        return tag;
    }
}
