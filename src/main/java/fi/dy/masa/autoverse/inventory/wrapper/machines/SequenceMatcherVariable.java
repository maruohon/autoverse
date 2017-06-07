package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class SequenceMatcherVariable extends SequenceMatcher
{
    private ItemStack endMarker = ItemStack.EMPTY;
    private int configuredLength;

    public SequenceMatcherVariable(int length, String tagName)
    {
        super(length, tagName);
    }

    public void setSequenceEndMarker(ItemStack marker)
    {
        this.endMarker = marker;
    }

    /**
     * Returns the current total configured length of the sequence
     * @return
     */
    @Override
    public int getCurrentSequenceLength()
    {
        if (this.isConfigured() == false)
        {
            return this.getPosition();
        }
        else
        {
            return this.configuredLength;
        }
    }

    @Override
    public boolean configureSequence(ItemStack inputStack)
    {
        boolean finished = false;

        if (InventoryUtils.areItemStacksEqual(inputStack, this.endMarker))
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
                this.configuredLength = this.getLength();
            }
        }

        return finished;
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
