package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import fi.dy.masa.autoverse.inventory.wrapper.InventoryCraftingWrapper;
import fi.dy.masa.autoverse.inventory.wrapper.machines.SequenceMatcher.SequenceInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperCrafter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcher sequenceEmpty;
    private final SequenceMatcher sequenceRecipeConfig;
    private final SequenceMatcher sequenceRecipe;
    private int outputPosition;
    private int subState;
    private ItemStack resultStackTemplate = ItemStack.EMPTY;

    private final InventoryCraftingWrapper inventoryCrafting;
    private final InventoryCraftingWrapper inventoryCraftingSequenceCraftingWrapper;
    private final IItemHandler inventoryCraftingGrid;
    private final IItemHandler inventoryCraftingOutput;
    private final SequenceInventory inventoryRecipePattern;

    public ItemHandlerWrapperCrafter(
            IItemHandler inventoryInput,
            InventoryCraftingWrapper inventoryCrafting,
            IItemHandlerModifiable inventoryCraftingOutput,
            IItemHandler inventoryOutput)
    {
        super(4, inventoryInput, inventoryOutput);

        this.inventoryCrafting = inventoryCrafting;
        this.inventoryCraftingGrid = new InvWrapper(this.inventoryCrafting);
        this.inventoryCraftingOutput = inventoryCraftingOutput;

        this.sequenceEmpty  = new SequenceMatcher(1, "SequenceEmpty");

        // This is the configured recipe sequence, including the items that represent empty slots
        this.sequenceRecipeConfig = new SequenceMatcher(9, "SequenceRecipe");

        // This is the final recipe sequence, and is not saved or added to the manager, but used internally
        this.sequenceRecipe = new SequenceMatcher(9, "SequenceRecipeMasked");

        this.inventoryRecipePattern = this.sequenceRecipeConfig.getSequenceInventory(false);

        this.inventoryCraftingSequenceCraftingWrapper = new InventoryCraftingWrapper(3, 3,
                this.sequenceRecipe.getSequenceInventory(false), inventoryCraftingOutput, null);

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

        // Switch to the empty-marker-removed sequence for normal mode (for GUI purposes)
        this.inventoryRecipePattern.setSequence(this.sequenceRecipe.getSequence());

        this.resultStackTemplate = CraftingManager.getInstance()
                .findMatchingRecipe(this.inventoryCraftingSequenceCraftingWrapper, this.inventoryCrafting.getWorld());
    }

    public void onLoad(World world)
    {
        this.inventoryCraftingSequenceCraftingWrapper.setWorld(world);
        this.resultStackTemplate = CraftingManager.getInstance().findMatchingRecipe(this.inventoryCraftingSequenceCraftingWrapper, world);
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
        List<Integer> slots = this.getMatchingSlots(inputStack);
        boolean success = false;

        if (slots != null)
        {
            for (int slot : slots)
            {
                if (inputStack.isEmpty())
                {
                    success = true;
                    break;
                }

                // Only move one item at a time to each grid slot
                if (this.inventoryCraftingGrid.getStackInSlot(slot).isEmpty() &&
                    InventoryUtils.areItemStacksEqual(this.sequenceRecipe.getStackInSlot(slot), inputStack))
                {
                    InventoryUtils.tryMoveStack(this.getInputInventory(), 0, this.inventoryCraftingGrid, slot, 1);
                }

                inputStack = this.getInputInventory().getStackInSlot(0);
            }

            if (success == false)
            {
                // Couldn't move the item to the grid, probably because the grid was full, move to the output instead
                success |= this.moveInputItemToOutput();
            }
        }

        if (this.resultStackTemplate.isEmpty() == false &&
            InventoryUtils.areItemStacksEqual(this.inventoryCraftingOutput.getStackInSlot(0), this.resultStackTemplate))
        {
            success |= InventoryUtils.tryMoveEntireStackOnly(this.inventoryCraftingOutput, 0, this.getOutputInventory(), 0) != InvResult.MOVED_NOTHING;
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

        if (this.gridMatchesRecipe())
        {
            this.subState = 0;
            return true;
        }

        return result == GridMoveResult.MOVED_SOME;
    }

    private boolean gridMatchesRecipe()
    {
        for (int slot = 0; slot < 9; ++slot)
        {
            if (InventoryUtils.areItemStacksEqual(
                    this.inventoryCraftingGrid.getStackInSlot(slot),
                    this.sequenceRecipe.getStackInSlot(slot)) == false)
            {
                return false;
            }
        }

        return true;
    }

    private GridMoveResult moveNonMatchingItemsFromGrid()
    {
        for (int slot = 0; slot < 9; ++slot)
        {
            ItemStack stackOnGrid = this.inventoryCraftingGrid.getStackInSlot(slot);

            if (stackOnGrid.isEmpty() == false &&
                InventoryUtils.areItemStacksEqual(stackOnGrid, this.sequenceRecipe.getStackInSlot(slot)) == false)
            {
                return InventoryUtils.tryMoveStack(this.inventoryCraftingGrid, slot,
                        this.getOutputInventory(), 0) != InvResult.MOVED_NOTHING ? GridMoveResult.MOVED_SOME : GridMoveResult.MOVED_NOTHING;
            }
        }

        return GridMoveResult.GRID_MATCHES;
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
