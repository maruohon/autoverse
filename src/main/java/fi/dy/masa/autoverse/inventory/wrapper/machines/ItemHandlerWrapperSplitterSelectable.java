package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public class ItemHandlerWrapperSplitterSelectable extends ItemHandlerWrapperSplitter
{
    private final SequenceMatcher sequenceSwitch2;

    public ItemHandlerWrapperSplitterSelectable(int sequenceLength, ItemStackHandlerTileEntity inventoryInput)
    {
        super(sequenceLength, inventoryInput);

        this.sequenceSwitch2 = new SequenceMatcher(sequenceLength, "ItemsSwitch2");
    }

    protected void handleInputItem(Mode mode, ItemStack inputStack)
    {
        switch (mode)
        {
            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_SWITCH_1);
                }
                break;

            case CONFIGURE_SWITCH_1:
                if (this.getSwitchSequence1().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_SWITCH_2);
                }
                break;

            case CONFIGURE_SWITCH_2:
                if (this.getSwitchSequence2().configureSequence(inputStack))
                {
                    this.setMode(Mode.NORMAL_OPERATION);
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.getResetSequence().reset();
                    this.getSwitchSequence1().reset();
                    this.getSwitchSequence2().reset();
                    this.setSecondaryOutputActive(false);
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                else
                {
                    if (this.getSwitchSequence1().checkInputItem(inputStack))
                    {
                        this.setSecondaryOutputActive(false);
                    }

                    if (this.getSwitchSequence2().checkInputItem(inputStack))
                    {
                        this.setSecondaryOutputActive(true);
                    }
                }
                break;

            default:
                break;
        }
    }

    public SequenceMatcher getSwitchSequence2()
    {
        return this.sequenceSwitch2;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);
        tag.setTag("SeqSwitch2", this.sequenceSwitch2.serializeNBT());
        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.sequenceSwitch2.deserializeNBT(tag.getCompoundTag("SeqSwitch2"));
    }
}
