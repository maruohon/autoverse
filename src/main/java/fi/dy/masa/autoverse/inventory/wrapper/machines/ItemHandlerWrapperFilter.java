package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperFilter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcher sequenceFilter;
    private final IItemHandler filterSequenceInventory;
    private final IItemHandler inventoryOutFiltered;
    private final IItemHandler inventoryOutNormal;
    private Mode mode = Mode.CONFIGURE_RESET;
    protected boolean matchesFilter;

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
                //System.out.printf("SORT_ITEMS\n");
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    //System.out.printf("SORT_ITEMS - reset\n");
                    this.getResetSequence().reset();
                    this.getFilterSequence().reset();
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                else
                {
                    this.matchesFilter = InventoryUtils.getSlotOfFirstMatchingItemStack(this.filterSequenceInventory, inputStack) != -1;
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
    public boolean moveInputItem()
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
                        //System.out.printf("moveInputItem - CONFIGURE_FILTER_DONE - done\n");
                        this.setMode(Mode.SORT_ITEMS);
                    }
                    //else System.out.printf("moveInputItem - done\n");

                    return true;
                }
                break;

            case SORT_ITEMS:
                //System.out.printf("moveInputItem - sorting\n");
                return this.sortItem();

            default:
                break;
        }

        return false;
    }

    protected boolean sortItem()
    {
        if (this.matchesFilter)
        {
            if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutFiltered, 0) == InvResult.MOVED_ALL)
            {
                this.matchesFilter = false;
                return true;
            }
        }
        else if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutNormal, 0) == InvResult.MOVED_ALL)
        {
            return true;
        }

        return false;
    }

    public SequenceMatcher getFilterSequence()
    {
        return this.sequenceFilter;
    }

    public IItemHandler getFilterSequenceInventory()
    {
        return this.filterSequenceInventory;
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
