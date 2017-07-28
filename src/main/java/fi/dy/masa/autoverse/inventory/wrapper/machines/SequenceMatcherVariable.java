package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class SequenceMatcherVariable extends SequenceMatcher
{
    private ItemStack endMarker = ItemStack.EMPTY;
    private int configuredLength;
    private boolean allowEmptySequence;

    public SequenceMatcherVariable(int length, String tagName)
    {
        super(length, tagName);
    }

    /**
     * Sets whether the sequence can be configured as an empty sequence,
     * by terminating the programming phase with the first configuration item.
     * @param allowEmpty
     * @return
     */
    public SequenceMatcherVariable setAllowEmptySequence(boolean allowEmpty)
    {
        this.allowEmptySequence = allowEmpty;
        return this;
    }

    public void setSequenceEndMarker(ItemStack marker)
    {
        this.endMarker = marker.isEmpty() ? ItemStack.EMPTY : marker.copy();
    }

    public ItemStack getSequenceEndMarker()
    {
        return this.endMarker;
    }

    /**
     * @return the current total configured length of the sequence
     */
    @Override
    public int getCurrentSequenceLength()
    {
        return this.isConfigured() ? this.configuredLength : this.getPosition();
    }

    @Override
    public boolean configureSequence(ItemStack inputStack)
    {
        boolean finished = false;

        if ((this.allowEmptySequence || this.getPosition() > 0) &&
            InventoryUtils.areItemStacksEqual(inputStack, this.endMarker))
        {
            this.onSequenceConfigured(inputStack);
            finished = true;
        }
        else
        {
            finished = super.configureSequence(inputStack);
        }

        if (finished)
        {
            this.configuredLength = InventoryUtils.getFirstEmptySlot(this.getSequenceInventory(false));

            if (this.configuredLength == -1)
            {
                this.configuredLength = this.getMaxLength();
            }
        }

        return finished;
    }

    @Override
    public void flushStart()
    {
        super.flushStart();

        // This sequence has been terminated by an end marker item.
        // We also want to flush the end marker item, so we move that
        // to the end of the sequence when the flush starts.
        if (this.endMarker.isEmpty() == false && this.configuredLength < this.getMaxLength())
        {
            this.getSequence().set(this.configuredLength, this.endMarker);
            this.endMarker = ItemStack.EMPTY;
            this.configuredLength++;
        }
    }

    @Override
    public void dropAllItems(World world, BlockPos pos)
    {
        super.dropAllItems(world, pos);

        // Only drop the marker if the sequence has actually been configured already,
        // otherwise dropping it would just duplicate the marker item.
        if (this.isConfigured() && this.configuredLength < this.getMaxLength() && this.endMarker.isEmpty() == false)
        {
            EntityUtils.dropItemStacksInWorld(world, pos, this.endMarker, -1, true);
        }
    }

    @Override
    public void reset()
    {
        super.reset();

        this.configuredLength = 0;
        this.endMarker = ItemStack.EMPTY;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);
        NBTTagCompound tag = nbt.getCompoundTag(this.getTagName());
        tag.setByte("ConfiguredLength", (byte) this.configuredLength);

        if (this.endMarker.isEmpty() == false)
        {
            tag.setTag("EndMarker", this.endMarker.writeToNBT(new NBTTagCompound()));
        }

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);

        NBTTagCompound tag = nbt.getCompoundTag(this.getTagName());
        this.configuredLength = tag.getByte("ConfiguredLength");
        this.endMarker = new ItemStack(tag.getCompoundTag("EndMarker"));
    }
}
