package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
    public void reset()
    {
        super.reset();

        this.endMarker = ItemStack.EMPTY;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);
        NBTTagCompound tag = nbt.getCompoundTag(this.getTagName());
        tag.setByte("ConfiguredLength", (byte) this.configuredLength);
        tag.setBoolean("AllowEmpty", this.allowEmptySequence);

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
        this.allowEmptySequence = tag.getBoolean("AllowEmpty");

        this.endMarker = new ItemStack(tag.getCompoundTag("EndMarker"));
    }
}
