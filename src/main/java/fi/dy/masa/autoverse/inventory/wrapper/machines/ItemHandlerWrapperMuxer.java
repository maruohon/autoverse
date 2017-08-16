package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerWrapperMuxer extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcher sequenceBitMarker;
    private final SequenceMatcherVariable sequenceLength1;
    private final SequenceMatcherVariable sequenceLength2;
    private final IItemHandler inventoryInput2;
    private int length1;
    private int length2;
    private int counter;
    private boolean inputIsSecondary;

    public ItemHandlerWrapperMuxer(
            IItemHandler inventoryInput1,
            IItemHandler inventoryInput2,
            IItemHandler inventoryOutput)
    {
        super(4, inventoryInput1, inventoryOutput);

        this.inventoryInput2 = inventoryInput2;

        this.sequenceBitMarker = new SequenceMatcher(1, "SequenceBitMarker");
        this.sequenceLength1   = new SequenceMatcherVariable(16, "SequenceLength1");
        this.sequenceLength2   = new SequenceMatcherVariable(16, "SequenceLength2");

        this.getSequenceManager().add(this.sequenceBitMarker, 1);
        this.getSequenceManager().add(this.sequenceLength1);
        this.getSequenceManager().add(this.sequenceLength2);
    }

    @Override
    protected void onFullyConfigured()
    {
        ItemStack stackHighBit = this.sequenceBitMarker.getSequence().get(0);

        this.length1 = this.sequenceLength1.parseValueFromSequence(stackHighBit);
        this.length2 = this.sequenceLength2.parseValueFromSequence(stackHighBit);
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.counter = 0;
        this.length1 = 0;
        this.length2 = 0;
    }

    @Override
    protected void onResetFlushStart()
    {
        super.onResetFlushStart();

        // Only reset the input side after the reset sequence's last item has been moved,
        // otherwise, if the reset command came (or more precisely, completed)
        // from side 2, it would get left in the slot.
        this.inputIsSecondary = false;
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        if (this.moveInputItemToOutput())
        {
            int max = this.inputIsSecondary ? this.length2 : this.length1;

            if (++this.counter >= max)
            {
                this.switchInputAndResetPosition();
            }

            return true;
        }

        return false;
    }

    public void switchInputAndResetPosition()
    {
        this.inputIsSecondary = ! this.inputIsSecondary;
        this.counter = 0;
    }

    @Override
    public IItemHandler getInputInventory()
    {
        return this.inputIsSecondary ? this.inventoryInput2 : super.getInputInventory();
    }

    public boolean secondaryInputActive()
    {
        return this.inputIsSecondary;
    }

    public SequenceMatcherVariable getSequenceLength1()
    {
        return this.sequenceLength1;
    }

    public SequenceMatcherVariable getSequenceLength2()
    {
        return this.sequenceLength2;
    }

    public IItemHandler getBitMarkerInventory()
    {
        return this.sequenceBitMarker.getSequenceInventory(false);
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setBoolean("Secondary", this.inputIsSecondary);
        tag.setShort("Length1", (short) this.length1);
        tag.setShort("Length2", (short) this.length2);
        tag.setShort("Counter", (short) this.counter);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.inputIsSecondary = tag.getBoolean("Secondary");
        this.length1 = tag.getShort("Length1");
        this.length2 = tag.getShort("Length2");
        this.counter = tag.getShort("Counter");
    }
}
