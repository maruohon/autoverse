package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperFilter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcherVariable sequenceFilter;
    private final IItemHandler inventoryOutFiltered;
    private final IItemHandler inventoryOutNormal;
    private Mode mode = Mode.CONFIGURE_END_MARKER;
    @Nullable
    protected List<Integer> matchingSlots;

    public ItemHandlerWrapperFilter(
            ItemStackHandlerTileEntity inventoryInput,
            ItemStackHandlerTileEntity inventoryOutFiltered,
            ItemStackHandlerTileEntity inventoryOutNormal)
    {
        super(4, inventoryInput);

        this.inventoryOutFiltered = inventoryOutFiltered;
        this.inventoryOutNormal = inventoryOutNormal;
        this.sequenceFilter = new SequenceMatcherVariable(18, "SequenceFilter");
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
                    this.sequenceFilter.setSequenceEndMarker(inputStack);
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                break;

            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_FILTER);
                }
                break;

            case CONFIGURE_FILTER:
                if (this.getFilterSequence().configureSequence(inputStack))
                {
                    // The separate state is for handling the last configuration input item by
                    // moving it to the normal output, before moving to the sort state
                    // where it would be moved to the filtered output instead.
                    this.setMode(Mode.CONFIGURE_FILTER_DONE);
                }
                break;

            case SORT_ITEMS:
            case OUTPUT_ITEMS:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.onReset();
                }
                else
                {
                    this.matchingSlots = this.getMatchingSlots(inputStack);
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

        this.getFilterSequence().reset();
        this.setMode(Mode.CONFIGURE_END_MARKER);
    }

    /**
     * Moves the item from the input slot/inventory to where it needs to go.
     * @return true when the item was successfully moved, false if it couldn't be moved
     */
    @Override
    public boolean moveItems()
    {
        Mode mode = this.getMode();

        switch (mode)
        {
            case CONFIGURE_END_MARKER:
            case CONFIGURE_RESET:
            case CONFIGURE_FILTER:
            case CONFIGURE_FILTER_DONE:
                if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutNormal, 0) == InvResult.MOVED_ALL)
                {
                    if (mode == Mode.CONFIGURE_FILTER_DONE)
                    {
                        this.onFilterConfigured();
                        this.setMode(Mode.SORT_ITEMS);
                    }

                    return true;
                }
                break;

            case SORT_ITEMS:
                return this.sortItem();

            case OUTPUT_ITEMS:
                return this.outputItems();

            case RESET:
                return this.flushItems();

            default:
                break;
        }

        return false;
    }

    protected void onFilterConfigured()
    {
        this.createMatchingSlotsMap(this.getFilterSequence().getSequence());
    }

    protected boolean sortItem()
    {
        if (this.matchingSlots != null)
        {
            if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutFiltered, 0) == InvResult.MOVED_ALL)
            {
                this.matchingSlots = null;
                return true;
            }
        }
        else if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutNormal, 0) == InvResult.MOVED_ALL)
        {
            return true;
        }

        return false;
    }

    /**
     * Move items from a buffer to the appropriate outputs, if any, before
     * returning to the sort mode and continuing to handle more input items.
     * @return
     */
    protected boolean outputItems()
    {
        return true;
    }

    /**
     * Move all items from internal buffers to the output, before returning
     * to the programming phase for the next operation cycle.
     * @return
     */
    protected boolean flushItems()
    {
        return true;
    }

    public SequenceMatcherVariable getFilterSequence()
    {
        return this.sequenceFilter;
    }

    protected IItemHandler getInventoryNormalOut()
    {
        return this.inventoryOutNormal;
    }

    public boolean isFullyConfigured()
    {
        return this.getMode() == Mode.SORT_ITEMS || this.getMode() == Mode.OUTPUT_ITEMS;
    }

    protected Mode getMode()
    {
        return this.mode;
    }

    protected void setMode(Mode mode)
    {
        this.mode = mode;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("Mode", (byte) this.mode.getId());

        this.sequenceFilter.writeToNBT(tag);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.setMode(Mode.fromId(tag.getByte("Mode")));

        this.sequenceFilter.readFromNBT(tag);

        this.createMatchingSlotsMap(this.sequenceFilter.getSequence());
    }

    public enum Mode
    {
        CONFIGURE_END_MARKER    (0),
        CONFIGURE_RESET         (1),
        CONFIGURE_FILTER        (2),
        CONFIGURE_FILTER_DONE   (3),
        SORT_ITEMS              (4),
        OUTPUT_ITEMS            (5),
        RESET                   (6);

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
