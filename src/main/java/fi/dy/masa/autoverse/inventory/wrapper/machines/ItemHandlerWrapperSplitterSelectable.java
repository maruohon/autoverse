package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public class ItemHandlerWrapperSplitterSelectable extends ItemHandlerWrapperSplitter
{
    private final SequenceMatcher sequenceSwitch2;

    public ItemHandlerWrapperSplitterSelectable(ItemStackHandlerTileEntity inventoryInput)
    {
        super(inventoryInput);

        this.sequenceSwitch2 = new SequenceMatcher(4, "SequenceSwitch2");
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
                    this.getResetSequence().reset();
                    this.getSwitchSequence1().reset();
                    this.getSwitchSequence2().reset();
                    this.setSecondaryOutputActive(false);
                    this.setMode(Mode.CONFIGURE_RESET);
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

    public SequenceMatcher getSwitchSequence2()
    {
        return this.sequenceSwitch2;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);
        this.sequenceSwitch2.writeToNBT(tag);
        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        this.sequenceSwitch2.readFromNBT(tag);
    }
}
