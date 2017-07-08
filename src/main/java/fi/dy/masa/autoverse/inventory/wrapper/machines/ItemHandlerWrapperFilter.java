package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public class ItemHandlerWrapperFilter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcherVariable sequenceFilter;
    private final IItemHandler inventoryFilteredOut;
    @Nullable
    protected List<Integer> matchingSlots;

    public ItemHandlerWrapperFilter(
            ItemStackHandlerTileEntity inventoryInput,
            ItemStackHandlerTileEntity inventoryOutFiltered,
            ItemStackHandlerTileEntity inventoryOutNormal)
    {
        super(4, inventoryInput, inventoryOutNormal);

        this.sequenceFilter = new SequenceMatcherVariable(18, "SequenceFilter");

        this.getSequenceManager().add(this.sequenceFilter);

        this.inventoryFilteredOut = inventoryOutFiltered;
    }

    @Override
    protected void onFullyConfigured()
    {
        this.createMatchingSlotsMap(this.getFilterSequence().getSequence());
    }

    @Override
    protected boolean moveItemNormal(ItemStack stack)
    {
        if (this.matchingSlots == null)
        {
            this.matchingSlots = this.getMatchingSlots(stack);
        }

        if (this.matchingSlots != null)
        {
            if (this.moveInputItemToInventory(this.inventoryFilteredOut))
            {
                this.matchingSlots = null;
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
