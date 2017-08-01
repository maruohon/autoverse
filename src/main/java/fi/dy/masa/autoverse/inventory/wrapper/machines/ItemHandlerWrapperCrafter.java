package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.inventory.wrapper.InventoryCraftingWrapper;
import fi.dy.masa.autoverse.inventory.wrapper.machines.SequenceMatcher.SequenceInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperCrafter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcher sequenceEmpty;
    private final SequenceMatcher sequenceRecipeConfig;
    private final SequenceMatcher sequenceRecipe;
    private final SequenceInventory inventoryRecipePattern;
    private final IItemHandlerModifiable inventoryCraftingGrid;
    private final IItemHandler inventoryCraftingOutput;
    private final InventoryCraftingWrapper inventoryCrafting;
    private ItemStack resultStackTemplate = ItemStack.EMPTY;
    private int outputPosition;
    private int subState;

    public ItemHandlerWrapperCrafter(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            IItemHandlerModifiable inventoryCraftingGridBase,
            IItemHandler inventoryCraftingOutput,
            InventoryCraftingWrapper inventoryCraftingWrapper)
    {
        super(4, inventoryInput, inventoryOutput);

        this.inventoryCrafting = inventoryCraftingWrapper;
        this.inventoryCraftingGrid = inventoryCraftingGridBase;
        this.inventoryCraftingOutput = inventoryCraftingOutput;

        this.sequenceEmpty  = new SequenceMatcher(1, "SequenceEmpty");

        // This is the configured recipe sequence, including the items that represent empty slots
        this.sequenceRecipeConfig = new SequenceMatcher(9, "SequenceRecipe");

        // This is the final recipe sequence, and is not saved or added to the manager, but used internally
        this.sequenceRecipe = new SequenceMatcher(9, "SequenceRecipeMasked");

        this.inventoryRecipePattern = this.sequenceRecipeConfig.getSequenceInventory(false);

        this.getSequenceManager().add(this.sequenceEmpty, 1);
        this.getSequenceManager().add(this.sequenceRecipeConfig);
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.outputPosition = 0;

        // Switch the underlying sequence to the full, empty-marker-including one for
        // the duration of the reset and the following configuration phase (for GUI purposes).
        this.inventoryRecipePattern.setSequence(this.sequenceRecipeConfig.getSequence());
    }

    @Override
    protected void onResetFlushComplete()
    {
        this.sequenceRecipe.reset();
        this.subState = 1;
    }

    @Override
    protected void onFullyConfigured()
    {
        this.setRecipeFromConfigurationSequence();

        // Set the grid contents to the recipe pattern, for the duration of getting the result item
        InventoryUtils.setInventoryContents(this.inventoryCraftingGrid, this.sequenceRecipe.getSequence(), false);

        this.updateCraftingOutput();
        this.resultStackTemplate = this.inventoryCraftingOutput.getStackInSlot(0).copy();

        // Clear the temporarily set recipe pattern stacks from the inventory
        InventoryUtils.clearInventory(this.inventoryCraftingGrid);

        // Clear the crafting output after the template has been stored
        this.updateCraftingOutput();

        // Switch to the empty-markers-removed sequence for normal mode (only used for GUI purposes)
        this.inventoryRecipePattern.setSequence(this.sequenceRecipe.getSequence());
    }

    public void onLoad()
    {
        // When loading from disk, init the crafting patterns and the crafting result template
        if (this.getState() == State.NORMAL)
        {
            // Store the current contents of the grid first, as they will be temporarily overridden by the pattern 
            NonNullList<ItemStack> stacks = InventoryUtils.createInventorySnapshot(this.inventoryCraftingGrid);
            this.onFullyConfigured();
            InventoryUtils.setInventoryContents(this.inventoryCraftingGrid, stacks, false);
        }
    }

    private void updateCraftingOutput()
    {
        // This updates the crafting output
        this.inventoryCrafting.markDirty();
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        return this.subState == 0 ? this.moveInputItem(stack) : this.moveItemsFromGrid();
    }

    @Override
    protected boolean onScheduledTick()
    {
        return this.subState == 1 ? this.moveItemsFromGrid() : false;
    }

    @Override
    protected boolean resetFlushItems()
    {
        if (this.subState == 0)
        {
            return super.resetFlushItems();
        }
        else
        {
            return this.flushItems();
        }
    }

    private void setRecipeFromConfigurationSequence()
    {
        ItemStack stackEmpty = this.sequenceEmpty.getStackInSlot(0);

        for (int slot = 0; slot < 9; slot++)
        {
            ItemStack stack = this.sequenceRecipeConfig.getStackInSlot(slot);

            if (InventoryUtils.areItemStacksEqual(stack, stackEmpty))
            {
                this.sequenceRecipe.getSequence().set(slot, ItemStack.EMPTY);
            }
            else
            {
                this.sequenceRecipe.getSequence().set(slot, stack.copy());
            }
        }

        this.createMatchingSlotsMap(this.sequenceRecipe.getSequence());
    }

    private boolean moveInputItem(ItemStack inputStack)
    {
        List<Integer> matchingSlots = this.getMatchingSlots(inputStack);
        boolean success = false;

        if (matchingSlots != null)
        {
            boolean matchedBefore = InventoryUtils.doesInventoryMatchTemplate(this.inventoryCraftingGrid, this.sequenceRecipe.getSequence());
            success = InventoryUtils.tryMoveStackToSmallestStackInOtherInventory(
                    this.getInputInventory(), this.inventoryCraftingGrid, 0, matchingSlots) != InvResult.MOVED_NOTHING;

            if (matchedBefore == false && success)
            {
                // Full recipe moved to the grid
                if (InventoryUtils.doesInventoryMatchTemplate(this.inventoryCraftingGrid, this.sequenceRecipe.getSequence()))
                {
                    this.updateCraftingOutput();
                }
            }

            if (success == false)
            {
                // Couldn't move the item to the grid, probably because the grid was full, move to the output instead
                success |= this.moveInputItemToOutput();
            }
        }

        // Items in the crafting output, that match the current recipe
        if (this.resultStackTemplate.isEmpty() == false &&
            InventoryUtils.areItemStacksEqual(this.inventoryCraftingOutput.getStackInSlot(0), this.resultStackTemplate))
        {
            // Successfully crafted something
            if (InventoryUtils.tryMoveEntireStackOnly(this.inventoryCraftingOutput, 0, this.getOutputInventory(), 0) != InvResult.MOVED_NOTHING)
            {
                success = true;

                if (InventoryUtils.doesInventoryHaveNonMatchingItems(this.inventoryCraftingGrid, this.sequenceRecipe.getSequence()))
                {
                    this.subState = 1;
                }
            }
        }
        else if (InventoryUtils.doesInventoryHaveNonMatchingItems(this.inventoryCraftingGrid, this.sequenceRecipe.getSequence()))
        {
            this.subState = 1;
            success |= this.moveNonMatchingItemsFromGrid() != GridMoveResult.MOVED_NOTHING;
        }
        else if (this.getInputInventory().getStackInSlot(0).isEmpty() == false)
        {
            success |= this.moveInputItemToOutput();
        }

        return success;
    }

    private boolean moveItemsFromGrid()
    {
        GridMoveResult result = this.moveNonMatchingItemsFromGrid();

        if (InventoryUtils.doesInventoryHaveNonMatchingItems(this.inventoryCraftingGrid, this.sequenceRecipe.getSequence()) == false)
        {
            this.subState = 0;
            return true;
        }

        return result != GridMoveResult.MOVED_NOTHING;
    }

    private GridMoveResult moveNonMatchingItemsFromGrid()
    {
        boolean matches = true;
        boolean moved = false;

        for (int slot = 0; slot < 9; ++slot)
        {
            ItemStack stackOnGrid = this.inventoryCraftingGrid.getStackInSlot(slot);

            if (stackOnGrid.isEmpty() == false &&
                InventoryUtils.areItemStacksEqual(stackOnGrid, this.sequenceRecipe.getStackInSlot(slot)) == false)
            {
                if (InventoryUtils.tryMoveStack(this.inventoryCraftingGrid, slot, this.getOutputInventory(), 0) != InvResult.MOVED_NOTHING)
                {
                    moved = true;
                }

                matches = false;
            }
        }

        return matches ? GridMoveResult.GRID_MATCHES : (moved ? GridMoveResult.MOVED_SOME : GridMoveResult.MOVED_NOTHING);
    }

    /**
     * Move all items from internal buffers to the output, before returning
     * to the programming phase for the next operation cycle.
     * @return
     */
    private boolean flushItems()
    {
        InvResult result = InvResult.MOVED_NOTHING;

        while (this.outputPosition < 9)
        {
            if (this.inventoryCraftingGrid.getStackInSlot(this.outputPosition).isEmpty())
            {
                this.outputPosition++;
            }
            else
            {
                result = InventoryUtils.tryMoveStack(this.inventoryCraftingGrid, this.outputPosition, this.getOutputInventory(), 0);

                if (result == InvResult.MOVED_ALL)
                {
                    this.outputPosition++;
                }

                break;
            }
        }

        // All items moved, return back to the programming phase
        if (this.outputPosition >= 9)
        {
            this.outputPosition = 0;
            this.subState = 0;
            this.setState(State.CONFIGURE);
            return true;
        }

        return result != InvResult.MOVED_NOTHING;
    }

    public IItemHandler getEmptyMarkerInventory()
    {
        return this.sequenceEmpty.getSequenceInventory(false);
    }

    public IItemHandler getRecipeSequenceInventory()
    {
        return this.inventoryRecipePattern;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.subState = tag.getByte("SubState");
        this.setRecipeFromConfigurationSequence();
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("SubState", (byte) this.subState);

        return tag;
    }

    private enum GridMoveResult
    {
        MOVED_NOTHING,
        MOVED_SOME,
        GRID_MATCHES;
    }
}
