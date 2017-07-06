package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitter;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class ItemHandlerWrapperRedstoneEmitter extends ItemHandlerWrapperSequenceBase
{
    private final TileEntityRedstoneEmitter te;
    private final SequenceMatcher sequenceMarkerItem;
    private final SequenceMatcherVariable sequenceSwitchOn;
    private final SequenceMatcherVariable sequenceSwitchOff;
    private Mode mode = Mode.CONFIGURE_END_MARKER;
    private boolean isOn;
    private int position;

    public ItemHandlerWrapperRedstoneEmitter(ItemStackHandlerTileEntity inventoryInput, TileEntityRedstoneEmitter te)
    {
        super(4, inventoryInput);
        this.te = te;

        this.sequenceMarkerItem = new SequenceMatcher(1, "SequenceMarker");
        this.sequenceSwitchOn   = new SequenceMatcherVariable(4, "SequenceOn");
        this.sequenceSwitchOff  = new SequenceMatcherVariable(4, "SequenceOff");
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
                    this.sequenceSwitchOn.setSequenceEndMarker(inputStack);
                    this.sequenceSwitchOff.setSequenceEndMarker(inputStack);
                    this.setMode(Mode.CONFIGURE_MARKER_ITEM);
                }
                break;

            case CONFIGURE_MARKER_ITEM:
                if (this.sequenceMarkerItem.configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                break;

            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_SIDES);
                }
                break;

            case CONFIGURE_SIDES:
                boolean enabled = InventoryUtils.areItemStacksEqual(inputStack, this.sequenceMarkerItem.getStackInSlot(0));

                this.te.setSideEnabled(this.position, enabled);

                if (++this.position >= 6)
                {
                    this.position = 0;
                    this.setMode(Mode.CONFIGURE_SEQUENCE_ON);
                }
                break;

            case CONFIGURE_SEQUENCE_ON:
                if (this.sequenceSwitchOn.configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_SEQUENCE_OFF);
                }
                break;

            case CONFIGURE_SEQUENCE_OFF:
                if (this.sequenceSwitchOff.configureSequence(inputStack))
                {
                    this.setMode(Mode.NORMAL_OPERATION);
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.onReset();
                }
                else if (this.isOn == false && this.sequenceSwitchOn.checkInputItem(inputStack))
                {
                    this.setIsOn(true);
                }
                else if (this.isOn && this.sequenceSwitchOff.checkInputItem(inputStack))
                {
                    this.setIsOn(false);
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

        this.sequenceMarkerItem.reset();
        this.sequenceSwitchOn.reset();
        this.sequenceSwitchOff.reset();

        this.position = 0;
        this.setIsOn(false);
        this.te.setSideMask(0);

        this.setMode(Mode.CONFIGURE_END_MARKER);
    }

    public IItemHandler getMarkerInventory()
    {
        return this.sequenceMarkerItem.getSequenceInventory(false);
    }

    public SequenceMatcher getSwitchOnSequence()
    {
        return this.sequenceSwitchOn;
    }

    public SequenceMatcher getSwitchOffSequence()
    {
        return this.sequenceSwitchOff;
    }

    protected Mode getMode()
    {
        return this.mode;
    }

    protected void setMode(Mode mode)
    {
        this.mode = mode;
    }

    private void setIsOn(boolean isOn)
    {
        this.isOn = isOn;
        this.te.setIsPowered(isOn);
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("State", (byte) ((this.isOn ? 0x80 : 0x00) | this.mode.getId()));
        tag.setByte("Position", (byte) this.position);

        this.sequenceMarkerItem.writeToNBT(tag);
        this.sequenceSwitchOn.writeToNBT(tag);
        this.sequenceSwitchOff.writeToNBT(tag);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        int state = tag.getByte("State");
        this.setMode(Mode.fromId(state & 0x7));
        this.setIsOn((state & 0x80) != 0);
        this.position = tag.getByte("Position");

        this.sequenceMarkerItem.readFromNBT(tag);
        this.sequenceSwitchOn.readFromNBT(tag);
        this.sequenceSwitchOff.readFromNBT(tag);
    }

    public enum Mode
    {
        CONFIGURE_END_MARKER    (0),
        CONFIGURE_MARKER_ITEM   (1),
        CONFIGURE_RESET         (2),
        CONFIGURE_SIDES         (3),
        CONFIGURE_SEQUENCE_ON   (4),
        CONFIGURE_SEQUENCE_OFF  (5),
        NORMAL_OPERATION        (6);

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
