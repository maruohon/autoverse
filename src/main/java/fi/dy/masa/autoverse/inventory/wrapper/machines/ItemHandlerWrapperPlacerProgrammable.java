package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.Arrays;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockPlacerProgrammable;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperPlacerProgrammable extends ItemHandlerWrapperSequenceBase
{
    private final TileEntityBlockPlacerProgrammable te;
    private final IItemHandler inventoryOutput;
    private final SequenceMatcher sequenceMarkerHighBit;
    private final SequenceMatcherVariable sequenceTrigger;
    private final SequenceMatcherVariable[] propertySequences;
    private final byte[] propertyValues;
    private Mode mode = Mode.CONFIGURE_END_MARKER;
    private int position;

    public ItemHandlerWrapperPlacerProgrammable(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            TileEntityBlockPlacerProgrammable te)
    {
        super(4, inventoryInput);

        this.te = te;
        this.inventoryOutput = inventoryOutput;
        this.sequenceMarkerHighBit  = new SequenceMatcher(1, "SequenceBitMarker");
        this.sequenceTrigger = (new SequenceMatcherVariable(4, "SequenceTrigger")).setAllowEmptySequence(true);

        this.propertySequences = new SequenceMatcherVariable[]
        {
                (new SequenceMatcherVariable(8, "SequenceProperty0")).setAllowEmptySequence(true),
                (new SequenceMatcherVariable(8, "SequenceProperty1")).setAllowEmptySequence(true),
                (new SequenceMatcherVariable(8, "SequenceProperty2")).setAllowEmptySequence(true)
        };

        this.propertyValues = new byte[this.propertySequences.length];
        Arrays.fill(this.propertyValues, (byte) -1);
    }

    @Override
    protected void handleInputItem(ItemStack inputStack)
    {
        switch (this.getMode())
        {
            case CONFIGURE_END_MARKER:
                if (this.getEndMarkerSequence().configureSequence(inputStack))
                {
                    this.getResetSequence().setSequenceEndMarker(inputStack);
                    this.sequenceTrigger.setSequenceEndMarker(inputStack);

                    for (SequenceMatcherVariable matcher : this.propertySequences)
                    {
                        matcher.setSequenceEndMarker(inputStack);
                    }

                    this.setMode(Mode.CONFIGURE_BIT_MARKER);
                }
                break;

            case CONFIGURE_BIT_MARKER:
                if (this.sequenceMarkerHighBit.configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                break;

            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_TRIGGER);
                }
                break;

            case CONFIGURE_TRIGGER:
                if (this.sequenceTrigger.configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_PROPERTIES);
                }
                break;

            case CONFIGURE_PROPERTIES:
                if (this.propertySequences[this.position].configureSequence(inputStack))
                {
                    if (++this.position >= this.propertySequences.length)
                    {
                        this.parsePropertyValues();
                        this.position = 0;
                        this.setMode(Mode.NORMAL_OPERATION);
                    }
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.onReset();
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.sequenceTrigger.reset();
        this.sequenceMarkerHighBit.reset();

        for (SequenceMatcherVariable matcher : this.propertySequences)
        {
            matcher.reset();
        }

        this.position = 0;
        this.setMode(Mode.CONFIGURE_END_MARKER);
    }

    @Override
    public boolean moveItems()
    {
        switch (this.getMode())
        {
            case NORMAL_OPERATION:
                ItemStack stack = this.getInputInventory().extractItem(0, 1, true);

                if (stack.isEmpty() == false)
                {
                    // Reusing the position variable to mark when the trigger sequence has matched, see above
                    if ((this.position != 0 || this.sequenceTrigger.isSequenceMatched()) && this.te.tryPlaceBlock(stack))
                    {
                        this.getInputInventory().extractItem(0, 1, false);
                        this.position = 0;
                        return true;
                    }
                    else
                    {
                        // Trigger match, the next item will be placed
                        if (this.sequenceTrigger.checkInputItem(stack))
                        {
                            // Just reusing this variable, since it's already being saved to NBT as well
                            this.position = 1;
                        }

                        return InventoryUtils.tryMoveStackToOtherInventory(this.getInputInventory(), this.inventoryOutput, 0, false) != InvResult.MOVED_NOTHING;
                    }
                }
                break;

            default:
                return InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutput, 0) != InvResult.MOVED_NOTHING;
        }

        return false;
    }

    private void parsePropertyValues()
    {
        ItemStack highBitMarker = this.sequenceMarkerHighBit.getSequence().get(0);

        for (int id = 0; id < this.propertySequences.length; id++)
        {
            this.propertyValues[id] = (byte) this.propertySequences[id].parseValueFromSequence(highBitMarker);
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

    protected Mode getMode()
    {
        return this.mode;
    }

    protected void setMode(Mode mode)
    {
        this.mode = mode;
    }

    @Override
    public int getSlots()
    {
        return 2;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return slot == 1 ? this.inventoryOutput.getSlotLimit(0) : this.getInputInventory().getSlotLimit(0);
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return slot == 1 ? this.inventoryOutput.getStackInSlot(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return slot == 0 ? super.insertItem(0, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return slot == 1 ? this.inventoryOutput.extractItem(0, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("State", (byte) this.mode.getId());
        tag.setByte("Position", (byte) this.position);
        tag.setByteArray("PropertyValues", this.propertyValues);

        this.sequenceMarkerHighBit.writeToNBT(tag);
        this.sequenceTrigger.writeToNBT(tag);

        for (SequenceMatcherVariable matcher : this.propertySequences)
        {
            matcher.writeToNBT(tag);
        }

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.setMode(Mode.fromId(tag.getByte("State")));
        this.position = tag.getByte("Position");
        byte[] props = tag.getByteArray("PropertyValues");

        for (int i = 0; i < props.length && i < this.propertyValues.length; i++)
        {
            this.propertySequences[i].readFromNBT(tag);
            this.propertyValues[i] = props[i];
        }

        this.sequenceMarkerHighBit.readFromNBT(tag);
        this.sequenceTrigger.readFromNBT(tag);
    }

    public enum Mode
    {
        CONFIGURE_END_MARKER    (0),
        CONFIGURE_BIT_MARKER    (1),
        CONFIGURE_RESET         (2),
        CONFIGURE_TRIGGER       (3),
        CONFIGURE_PROPERTIES    (4),
        NORMAL_OPERATION        (5);

        private final int id;

        private Mode (int id)
        {
            this.id = id;
        }

        public int getId()
        {
            return this.id;
        }

        public static Mode fromId(int id)
        {
            return values()[id % values().length];
        }
    }
}
