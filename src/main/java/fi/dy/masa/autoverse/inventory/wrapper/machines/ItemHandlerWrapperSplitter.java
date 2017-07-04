package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public class ItemHandlerWrapperSplitter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcher sequenceSwitch1;
    private Mode mode = Mode.CONFIGURE_RESET;
    private boolean outputIsSecondary;

    public ItemHandlerWrapperSplitter(ItemStackHandlerTileEntity inventoryInput)
    {
        super(4, inventoryInput);

        this.sequenceSwitch1 = new SequenceMatcher(4, "SequenceSwitch1");
    }

    @Override
    protected void handleInputItem(ItemStack inputStack)
    {
        switch (this.getMode())
        {
            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_SEQUENCE_1);
                }
                break;

            case CONFIGURE_SEQUENCE_1:
                if (this.getSwitchSequence1().configureSequence(inputStack))
                {
                    this.setMode(Mode.NORMAL_OPERATION);
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.getResetSequence().reset();
                    this.getSwitchSequence1().reset();
                    this.setSecondaryOutputActive(false);
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                else if (this.getSwitchSequence1().checkInputItem(inputStack))
                {
                    this.setSecondaryOutputActive(! this.secondaryOutputActive());
                }
                break;

            default:
                break;
        }
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

    public SequenceMatcher getSwitchSequence1()
    {
        return this.sequenceSwitch1;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("State", (byte) ((this.outputIsSecondary ? 0x80 : 0x00) | this.mode.getId()));
        this.sequenceSwitch1.writeToNBT(tag);

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
    }

    public enum Mode
    {
        CONFIGURE_RESET       (0),
        CONFIGURE_SEQUENCE_1  (1),
        CONFIGURE_SEQUENCE_2  (2),
        NORMAL_OPERATION      (3);

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
