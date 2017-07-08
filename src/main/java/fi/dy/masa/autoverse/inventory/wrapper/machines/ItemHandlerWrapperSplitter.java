package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerWrapperSplitter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcherVariable sequenceSwitch1;
    private final SequenceMatcherVariable sequenceSwitch2;
    private final IItemHandler inventoryOutput2;
    private boolean outputIsSecondary;

    public ItemHandlerWrapperSplitter(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput1,
            IItemHandler inventoryOutput2)
    {
        super(4, inventoryInput, inventoryOutput1);

        this.inventoryOutput2 = inventoryOutput2;

        this.sequenceSwitch1 = new SequenceMatcherVariable(4, "SequenceSwitch1");
        this.sequenceSwitch2 = new SequenceMatcherVariable(4, "SequenceSwitch2");

        this.getSequenceManager().add(this.sequenceSwitch1);
        this.getSequenceManager().add(this.sequenceSwitch2);
    }

    @Override
    protected void onResetFlushStart()
    {
        // Switch the output only after the item that caused the reset has been moved out,
        // so that it ends up on the same output as the other items in the reset command.
        this.setSecondaryOutputActive(false);
    }

    @Override
    protected boolean moveInputItemToOutput()
    {
        IItemHandler inv = this.secondaryOutputActive() ? this.inventoryOutput2 : this.getOutputInventory();
        return this.moveInputItemToInventory(inv);
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        if (this.moveInputItemToOutput())
        {
            if (this.secondaryOutputActive() && this.getSwitchSequence1().checkInputItem(stack))
            {
                this.setSecondaryOutputActive(false);
            }
            else if (this.secondaryOutputActive() == false && this.getSwitchSequence2().checkInputItem(stack))
            {
                this.setSecondaryOutputActive(true);
            }

            return true;
        }

        return false;
    }

    protected void setSecondaryOutputActive(boolean secondaryActive)
    {
        this.outputIsSecondary = secondaryActive;
    }

    public boolean secondaryOutputActive()
    {
        return this.outputIsSecondary;
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

        tag.setBoolean("Secondary", this.outputIsSecondary);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.setSecondaryOutputActive(tag.getBoolean("Secondary"));
    }
}
