package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencerProgrammable;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperSequencerProgrammable extends ItemHandlerWrapperSequenceBase implements ISequenceCallback
{
    public static final int MAX_INV_SIZE = 45;
    private final SequenceMatcherVariable sequenceGeneration;
    private final ItemStackHandlerLockable inventorySequence;
    private int position;

    public ItemHandlerWrapperSequencerProgrammable(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            TileEntitySequencerProgrammable te)
    {
        super(4, inventoryInput, inventoryOutput);

        this.sequenceGeneration = new SequenceMatcherVariable(MAX_INV_SIZE, "SequenceGeneration");
        this.sequenceGeneration.setCallback(this, 0);

        this.getSequenceManager().add(this.sequenceGeneration);

        this.inventorySequence = new ItemStackHandlerLockable(2, MAX_INV_SIZE, 64, false, "ItemsSequence", te);
        this.inventorySequence.setInventorySize(0);
    }

    @Override
    public void onConfigureSequenceSlot(int callbackId, int slot, boolean finished)
    {
        this.inventorySequence.setInventorySize(slot + 1);
        this.inventorySequence.setTemplateStackInSlot(slot, this.sequenceGeneration.getStackInSlot(slot));
        this.inventorySequence.setSlotLocked(slot, true);
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.position = 0;
    }

    @Override
    protected void onResetFlushComplete()
    {
        this.position = 1;
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        if (this.moveInputItemToInventory(this.inventorySequence))
        {
            return true;
        }
        else
        {
            return this.moveInputItemToOutput();
        }
    }

    @Override
    protected boolean resetFlushItems()
    {
        if (this.position == 0)
        {
            return super.resetFlushItems();
        }
        else
        {
            return this.flushItems();
        }
    }

    private boolean flushItems()
    {
        boolean success = InventoryUtils.tryMoveAllItems(this.inventorySequence, this.getOutputInventory()) != InvResult.MOVED_NOTHING;

        if (InventoryUtils.isInventoryEmpty(this.inventorySequence))
        {
            this.inventorySequence.clearTemplateStacks();
            this.inventorySequence.clearLockedStatus();
            this.inventorySequence.setInventorySize(0);
            this.position = 0;
            this.setState(State.CONFIGURE);
        }

        return success;
    }

    @Override
    public int getSlots()
    {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        // The first "virtual slot" is for extraction, the second is for insertion (and thus always empty)
        if (slot == 0 && this.getState() == State.NORMAL)
        {
            return this.inventorySequence.getStackInSlot(this.position);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (slot == 0 && this.getState() == State.NORMAL)
        {
            ItemStack stack = this.inventorySequence.extractItem(this.position, amount, simulate);

            if (simulate == false && stack.isEmpty() == false)
            {
                if (++this.position >= this.inventorySequence.getSlots())
                {
                    this.position = 0;
                }
            }

            return stack;
        }

        return ItemStack.EMPTY;
    }

    public SequenceMatcherVariable getGenerationSequence()
    {
        return this.sequenceGeneration;
    }

    public ItemStackHandlerLockable getSequenceInventory()
    {
        return this.inventorySequence;
    }

    public int getExtractPosition()
    {
        return this.getState() == State.NORMAL ? this.position : 0;
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("Position", (byte) this.position);
        tag.merge(this.inventorySequence.serializeNBT());

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.position = tag.getByte("Position");
        this.inventorySequence.deserializeNBT(tag);
    }
}
