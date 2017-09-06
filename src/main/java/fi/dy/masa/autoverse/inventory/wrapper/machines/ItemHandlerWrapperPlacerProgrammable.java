package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.Arrays;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockPlacerProgrammable;

public class ItemHandlerWrapperPlacerProgrammable extends ItemHandlerWrapperSequenceBase
{
    public static final int NUM_PROPERTIES = 4;
    private final TileEntityBlockPlacerProgrammable te;
    private final SequenceMatcher sequenceMarkerHighBit;
    private final SequenceMatcherVariable sequenceTrigger;
    private final SequenceMatcherVariable sequenceOffset;
    private final SequenceMatcherVariable[] propertySequences;
    private final int[] propertyValues;
    private int subState;

    public ItemHandlerWrapperPlacerProgrammable(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            TileEntityBlockPlacerProgrammable te)
    {
        super(4, inventoryInput, inventoryOutput);

        this.te = te;
        this.sequenceMarkerHighBit  = new SequenceMatcher(1, "SequenceBitMarker");
        this.sequenceTrigger = (new SequenceMatcherVariable(4, "SequenceTrigger")).setAllowEmptySequence(true);
        this.sequenceOffset  = (new SequenceMatcherVariable(4, "SequenceOffset")).setAllowEmptySequence(true);
        this.propertySequences = new SequenceMatcherVariable[NUM_PROPERTIES];

        this.getSequenceManager().add(this.sequenceMarkerHighBit, 1);
        this.getSequenceManager().add(this.sequenceTrigger);
        this.getSequenceManager().add(this.sequenceOffset);

        for (int i = 0; i < this.propertySequences.length; i++)
        {
            this.propertySequences[i] = (new SequenceMatcherVariable(8, "SequenceProperty" + i)).setAllowEmptySequence(true);
            this.getSequenceManager().add(this.propertySequences[i]);
        }

        this.propertyValues = new int[this.propertySequences.length];
        Arrays.fill(this.propertyValues, -1);
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.subState = 0;
    }

    @Override
    protected void onFullyConfigured()
    {
        this.parsePropertyValues();
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        if ((this.subState == 1 || this.sequenceTrigger.isSequenceMatched()) && this.te.tryPlaceBlock(stack))
        {
            this.getInputInventory().extractItem(0, 1, false);
            this.subState = 0;
            return true;
        }
        else
        {
            if (this.moveInputItemToOutput())
            {
                if (this.sequenceTrigger.checkInputItem(stack))
                {
                    this.subState = 1;
                }

                return true;
            }

            return false;
        }
    }

    public boolean canConfigureProperties()
    {
        // In configuration phase, and just starting to configure the first block property
        return this.getState() == State.CONFIGURE &&
               this.getSequenceManager().getCurrentSequenceIndex() == 5 &&
               this.propertySequences[0].getPosition() == 0;
    }

    private void parsePropertyValues()
    {
        ItemStack highBitMarker = this.sequenceMarkerHighBit.getSequence().get(0);
        this.te.setPlacementOffset(this.sequenceOffset.parseValueFromSequence(highBitMarker));

        for (int id = 0; id < this.propertySequences.length; id++)
        {
            this.propertyValues[id] = this.propertySequences[id].parseValueFromSequence(highBitMarker);
        }
    }

    public IItemHandler getHighBitMarkerInventory()
    {
        return this.sequenceMarkerHighBit.getSequenceInventory(false);
    }

    public SequenceMatcher getTriggerSequence()
    {
        return this.sequenceTrigger;
    }

    public SequenceMatcher getOffsetSequence()
    {
        return this.sequenceOffset;
    }

    public SequenceMatcher getPropertySequence(int id)
    {
        return this.propertySequences[id];
    }

    public int getPropertyValue(int id)
    {
        return this.propertyValues[id];
    }

    public int getPropertyCount()
    {
        return this.propertyValues.length;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return slot == 1 ? this.getOutputInventory().getSlotLimit(0) : this.getInputInventory().getSlotLimit(0);
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("SubState", (byte) this.subState);
        tag.setIntArray("PropertyValues", this.propertyValues);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.subState = tag.getByte("SubState");
        int[] props = tag.getIntArray("PropertyValues");

        for (int i = 0; i < props.length && i < this.propertyValues.length; i++)
        {
            this.propertyValues[i] = props[i];
        }
    }
}
