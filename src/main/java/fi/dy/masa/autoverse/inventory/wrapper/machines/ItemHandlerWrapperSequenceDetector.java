package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.TileEntitySequenceDetector;

public class ItemHandlerWrapperSequenceDetector extends ItemHandlerWrapperSequenceBase
{
    private final TileEntitySequenceDetector te;
    private final SequenceMatcherVariable sequenceDetection;
    private Mode mode = Mode.CONFIGURE_END_MARKER;
    private int position;

    public ItemHandlerWrapperSequenceDetector(IItemHandler inventoryInput, TileEntitySequenceDetector te)
    {
        super(4, inventoryInput);

        this.te = te;
        this.sequenceDetection  = new SequenceMatcherVariable(45, "SequenceDetection");
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
                    this.sequenceDetection.setSequenceEndMarker(inputStack);
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                break;

            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_DETECTOR);
                }
                break;

            case CONFIGURE_DETECTOR:
                if (this.sequenceDetection.configureSequence(inputStack))
                {
                    this.setMode(Mode.NORMAL_OPERATION);
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.onReset();
                }
                else if (this.sequenceDetection.checkInputItem(inputStack))
                {
                    this.onSequenceMatch();
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

        this.sequenceDetection.reset();
        this.position = 0;

        this.setMode(Mode.CONFIGURE_END_MARKER);
    }

    public SequenceMatcherVariable getDetectionSequence()
    {
        return this.sequenceDetection;
    }

    public int getCurrentDetectionSequenceLength()
    {
        return this.sequenceDetection.getCurrentSequenceLength();
    }

    protected Mode getMode()
    {
        return this.mode;
    }

    protected void setMode(Mode mode)
    {
        this.mode = mode;
    }

    private void onSequenceMatch()
    {
        this.te.onSequenceMatch();
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("State", (byte) this.mode.getId());
        tag.setByte("Position", (byte) this.position);

        this.sequenceDetection.writeToNBT(tag);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.setMode(Mode.fromId(tag.getByte("State")));
        this.position = tag.getByte("Position");

        this.sequenceDetection.readFromNBT(tag);
    }

    public enum Mode
    {
        CONFIGURE_END_MARKER    (0),
        CONFIGURE_RESET         (1),
        CONFIGURE_DETECTOR      (2),
        NORMAL_OPERATION        (3);

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
