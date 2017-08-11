package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public class ItemHandlerWrapperFilter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcherVariable sequenceFilter;
    protected final IItemHandler inventoryFilteredOut;

    public ItemHandlerWrapperFilter(
            int maxFilterLength,
            ItemStackHandlerTileEntity inventoryInput,
            ItemStackHandlerTileEntity inventoryOutFiltered,
            ItemStackHandlerTileEntity inventoryOutNormal)
    {
        super(4, inventoryInput, inventoryOutNormal);

        this.sequenceFilter = new SequenceMatcherVariable(maxFilterLength, "SequenceFilter");

        this.getSequenceManager().add(this.sequenceFilter);

        this.inventoryFilteredOut = inventoryOutFiltered;
    }

    @Override
    protected void onFullyConfigured()
    {
        this.createMatchingSlotsMap(this.getFilterSequence().getSequence());
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        if (this.getMatchingSlots(stack) != null)
        {
            if (this.moveInputItemToInventory(this.inventoryFilteredOut))
            {
                return true;
            }
        }
        else
        {
            return this.moveInputItemToOutput();
        }

        return false;
    }

    public SequenceMatcherVariable getFilterSequence()
    {
        return this.sequenceFilter;
    }

    public boolean isFullyConfigured()
    {
        return this.getState() == State.NORMAL;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.createMatchingSlotsMap(this.sequenceFilter.getSequence());
    }
}
