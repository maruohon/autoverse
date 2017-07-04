package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.TileEntitySequenceDetector;

public class ItemHandlerWrapperSequenceDetector extends ItemHandlerWrapperSequenceBase
{
    private final TileEntitySequenceDetector te;
    private final SequenceMatcher sequenceMarkerItem;
    private final SequenceMatcherVariable sequenceDetection;
    private final IItemHandler markerInventory;
    private Mode mode = Mode.CONFIGURE_RESET;
    private int position;

    public ItemHandlerWrapperSequenceDetector(IItemHandler inventoryInput, TileEntitySequenceDetector te)
    {
        super(4, inventoryInput);

        this.te = te;
        this.sequenceMarkerItem = new SequenceMatcher(1, "SequenceMarker");
        this.sequenceDetection  = new SequenceMatcherVariable(45, "SequenceDetection");

        this.markerInventory = this.sequenceMarkerItem.getSequenceInventory(false);
    }

    @Override
    protected void handleInputItem(ItemStack inputStack)
    {
        switch (this.getMode())
        {
            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_MARKER);
                }
                break;

            case CONFIGURE_MARKER:
                if (this.sequenceMarkerItem.configureSequence(inputStack))
                {
                    this.sequenceDetection.setSequenceEndMarker(inputStack);
                    this.setMode(Mode.CONFIGURE_SEQUENCE);
                }
                break;

            case CONFIGURE_SEQUENCE:
                if (this.sequenceDetection.configureSequence(inputStack))
                {
                    this.setMode(Mode.NORMAL_OPERATION);
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.getResetSequence().reset();
                    this.sequenceMarkerItem.reset();
                    this.sequenceDetection.reset();
                    this.position = 0;
                    this.setMode(Mode.CONFIGURE_RESET);
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

    public IItemHandler getMarkerInventory()
    {
        return this.markerInventory;
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

        this.sequenceMarkerItem.writeToNBT(tag);
        this.sequenceDetection.writeToNBT(tag);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.setMode(Mode.fromId(tag.getByte("State")));
        this.position = tag.getByte("Position");

        this.sequenceMarkerItem.readFromNBT(tag);
        this.sequenceDetection.readFromNBT(tag);
    }

    public enum Mode
    {
        CONFIGURE_RESET         (0),
        CONFIGURE_MARKER        (1),
        CONFIGURE_SEQUENCE      (2),
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
