package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.ItemType;

public class ItemHandlerWrapperFilter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcher sequenceFilter;
    private final IItemHandler filterSequenceInventory;
    private final IItemHandler inventoryOutFiltered;
    private final IItemHandler inventoryOutNormal;
    private final Map<ItemType, List<Integer>> matchingSlotsMap = new HashMap<ItemType, List<Integer>>();
    private Mode mode = Mode.CONFIGURE_RESET;
    @Nullable
    protected List<Integer> matchingSlots;

    public ItemHandlerWrapperFilter(
            int resetLength, int filterLength,
            ItemStackHandlerTileEntity inventoryInput,
            ItemStackHandlerTileEntity inventoryOutFiltered,
            ItemStackHandlerTileEntity inventoryOutNormal)
    {
        super(resetLength, inventoryInput);

        this.inventoryOutFiltered = inventoryOutFiltered;
        this.inventoryOutNormal = inventoryOutNormal;
        this.sequenceFilter = new SequenceMatcher(filterLength, "SequenceFilter");
        this.filterSequenceInventory = this.sequenceFilter.getSequenceInventory(false);
    }

    @Override
    protected void handleInputItem(ItemStack inputStack)
    {
        switch (this.getMode())
        {
            case CONFIGURE_RESET:
                //System.out.printf("CONFIGURE_RESET\n");
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    //System.out.printf("CONFIGURE_RESET - done\n");
                    this.setMode(Mode.CONFIGURE_FILTER);
                }
                break;

            case CONFIGURE_FILTER:
                //System.out.printf("CONFIGURE_FILTER\n");
                if (this.getFilterSequence().configureSequence(inputStack))
                {
                    //System.out.printf("CONFIGURE_FILTER - done\n");
                    // The separate state is for handling the last configuration input item by
                    // moving it to the normal output, before moving to the sort state
                    // where it would be moved to the filtered output instead.
                    this.setMode(Mode.CONFIGURE_FILTER_DONE);
                }
                break;

            case SORT_ITEMS:
            case OUTPUT_ITEMS:
                //System.out.printf("SORT_ITEMS\n");
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    //System.out.printf("SORT_ITEMS - reset\n");
                    this.getResetSequence().reset();
                    this.getFilterSequence().reset();
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

    /**
     * Moves the item from the input slot/inventory to where it needs to go.
     * @return true when the item was successfully moved, false if it couldn't be moved
     */
    public boolean moveItems()
    {
        Mode mode = this.getMode();

        switch (mode)
        {
            case CONFIGURE_RESET:
            case CONFIGURE_FILTER:
            case CONFIGURE_FILTER_DONE:
                //System.out.printf("moveInputItem - conf\n");
                if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutNormal, 0) == InvResult.MOVED_ALL)
                {
                    if (mode == Mode.CONFIGURE_FILTER_DONE)
                    {
                        this.createMatchingSlotsMap();
                        this.setMode(Mode.SORT_ITEMS);
                        //System.out.printf("moveInputItem - CONFIGURE_FILTER_DONE - done\n");
                    }
                    //else System.out.printf("moveInputItem - done\n");

                    return true;
                }
                break;

            case SORT_ITEMS:
                //System.out.printf("moveInputItem - sorting\n");
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

    /**
     * Called when an input item completes a reset sequence
     */
    protected void onReset()
    {
        this.setMode(Mode.CONFIGURE_RESET);
    }

    protected void createMatchingSlotsMap()
    {
        NonNullList<ItemStack> filterItems = this.getFilterSequence().getSequence();
        final int filterLength = filterItems.size();

        for (int slot = 0; slot < filterLength; ++slot)
        {
            this.addItemTypeToMap(slot, filterItems.get(slot));
        }
    }

    protected void addItemTypeToMap(int slot, ItemStack stack)
    {
        ItemType itemType = new ItemType(stack);
        List<Integer> list = this.matchingSlotsMap.get(itemType);

        if (list == null)
        {
            list = new ArrayList<Integer>();
            this.matchingSlotsMap.put(itemType, list);
        }

        list.add(slot);
    }

    @Nullable
    protected List<Integer> getMatchingSlots(ItemStack stack)
    {
        return this.matchingSlotsMap.get(new ItemType(stack));
    }

    public SequenceMatcher getFilterSequence()
    {
        return this.sequenceFilter;
    }

    public IItemHandler getFilterSequenceInventory()
    {
        return this.filterSequenceInventory;
    }

    protected IItemHandler getInventoryNormalOut()
    {
        return this.inventoryOutNormal;
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

        this.createMatchingSlotsMap();
    }

    public enum Mode
    {
        CONFIGURE_RESET         (0),
        CONFIGURE_FILTER        (1),
        CONFIGURE_FILTER_DONE   (2),
        SORT_ITEMS              (3),
        OUTPUT_ITEMS            (4),
        RESET                   (5);

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
