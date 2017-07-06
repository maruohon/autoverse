package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public class ItemHandlerWrapperSplitter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcherVariable sequenceSwitch1;
    private final SequenceMatcherVariable sequenceSwitch2;
    private Mode mode = Mode.CONFIGURE_END_MARKER;
    private boolean outputIsSecondary;

    public ItemHandlerWrapperSplitter(ItemStackHandlerTileEntity inventoryInput)
    {
        super(4, inventoryInput);

        this.sequenceSwitch1 = new SequenceMatcherVariable(4, "SequenceSwitch1");
        this.sequenceSwitch2 = new SequenceMatcherVariable(4, "SequenceSwitch2");
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
                    this.sequenceSwitch1.setSequenceEndMarker(inputStack);
                    this.sequenceSwitch2.setSequenceEndMarker(inputStack);
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                break;

            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_SEQUENCE_1);
                }
                break;

            case CONFIGURE_SEQUENCE_1:
                if (this.getSwitchSequence1().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_SEQUENCE_2);
                }
                break;

            case CONFIGURE_SEQUENCE_2:
                if (this.getSwitchSequence2().configureSequence(inputStack))
                {
                    this.setMode(Mode.NORMAL_OPERATION);
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.onReset();
                }
                else
                {
                    if (this.secondaryOutputActive() && this.getSwitchSequence1().checkInputItem(inputStack))
                    {
                        this.setSecondaryOutputActive(false);
                    }
                    else if (this.secondaryOutputActive() == false && this.getSwitchSequence2().checkInputItem(inputStack))
                    {
                        this.setSecondaryOutputActive(true);
                    }
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

        this.getSwitchSequence1().reset();
        this.getSwitchSequence2().reset();

        this.setSecondaryOutputActive(false);
        this.setMode(Mode.CONFIGURE_END_MARKER);
    }

    protected void setSecondaryOutputActive(boolean secondaryActive)
    {
        this.outputIsSecondary = secondaryActive;
    }

    public boolean secondaryOutputActive()
    {
        return this.outputIsSecondary;
    }

    protected Mode getMode()
    {
        return this.mode;
    }

    protected void setMode(Mode mode)
    {
        this.mode = mode;
    }

    public SequenceMatcherVariable getSwitchSequence1()
    {
        return this.sequenceSwitch1;
    }

    public SequenceMatcherVariable getSwitchSequence2()
    {
        return this.sequenceSwitch2;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("State", (byte) ((this.outputIsSecondary ? 0x80 : 0x00) | this.mode.getId()));

        this.sequenceSwitch1.writeToNBT(tag);
        this.sequenceSwitch2.writeToNBT(tag);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        int state = tag.getByte("State");
        this.setMode(Mode.fromId(state & 0x7));
        this.setSecondaryOutputActive((state & 0x80) != 0);

        this.sequenceSwitch1.readFromNBT(tag);
        this.sequenceSwitch2.readFromNBT(tag);
    }

    public enum Mode
    {
        CONFIGURE_END_MARKER    (0),
        CONFIGURE_RESET         (1),
        CONFIGURE_SEQUENCE_1    (2),
        CONFIGURE_SEQUENCE_2    (3),
        NORMAL_OPERATION        (4);

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
