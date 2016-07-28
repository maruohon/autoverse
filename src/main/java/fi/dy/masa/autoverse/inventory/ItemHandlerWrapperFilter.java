package fi.dy.masa.autoverse.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverse;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class ItemHandlerWrapperFilter implements IItemHandler, INBTSerializable<NBTTagCompound>
{
    protected final TileEntityAutoverse te;
    protected final IItemHandler resetItems;
    protected final IItemHandler filterItems;
    protected final IItemHandler filteredOut;
    protected final IItemHandler othersOut;
    protected final ItemStackHandlerBasic resetSequenceBuffer;
    protected int seqBufWrite;
    protected EnumMode mode;

    public ItemHandlerWrapperFilter(
            IItemHandler resetItems,
            IItemHandler filterItems,
            IItemHandler filteredOut,
            IItemHandler othersOut,
            TileEntityAutoverse te)
    {
        this.resetItems = resetItems;
        this.filterItems = filterItems;
        this.filteredOut = filteredOut;
        this.othersOut = othersOut;
        this.te = te;
        this.resetSequenceBuffer = new ItemStackHandlerBasic(this.resetItems.getSlots(), 1, false, "MatchedItems");
        this.seqBufWrite = 0;
        this.mode = EnumMode.ACCEPT_RESET_ITEMS;
    }

    public EnumMode getMode()
    {
        return this.mode;
    }

    public void setMode(EnumMode mode)
    {
        this.mode = mode;
    }

    public IItemHandler getSequenceBuffer()
    {
        return this.resetSequenceBuffer;
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("SeqWr", (byte)this.seqBufWrite);
        tag.setByte("Mode", (byte)this.mode.getId());
        tag.merge(this.resetSequenceBuffer.serializeNBT());

        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag)
    {
        this.seqBufWrite  = tag.getByte("SeqWr");
        this.mode = EnumMode.fromId(tag.getByte("Mode"));
        this.resetSequenceBuffer.deserializeNBT(tag);
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return null;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return null;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        //String side = this.te.getWorld().isRemote ? "client" : "server";
        //System.out.printf("%s - %s - INSERT - wrPos: %d stack: %s mode: %s\n",
        //        side, simulate, this.seqBufWrite, stack, this.mode);

        switch (this.mode)
        {
            case ACCEPT_RESET_ITEMS:
                stack = this.configureSequence(stack, simulate, this.resetItems, EnumMode.ACCEPT_FILTER_ITEMS);
                break;

            case ACCEPT_FILTER_ITEMS:
                stack = this.configureSequence(stack, simulate, this.filterItems, EnumMode.SORT_ITEMS);
                break;

            case SORT_ITEMS:
                stack = this.sortItem(stack, simulate);
                if (simulate == false)
                {
                    this.te.scheduleBlockTick(1, false);
                }
                break;

            case RESET:
                if (InventoryUtils.isInventoryEmpty(this.filteredOut) && InventoryUtils.isInventoryEmpty(this.othersOut))
                {
                    this.mode = EnumMode.ACCEPT_RESET_ITEMS;
                }
                break;

            default:
        }

        //System.out.printf("======== insert end ========\n");
        return stack;
    }

    protected ItemStack configureSequence(ItemStack stack, boolean simulate, IItemHandler inv, EnumMode nextMode)
    {
        if (this.seqBufWrite >= inv.getSlots())
        {
            Autoverse.logger.warn(String.format("%s#configureSequence: seqBufWrite out of range: %d - max: %d",
                    this.getClass().getSimpleName(), this.seqBufWrite, inv.getSlots() - 1));
            this.seqBufWrite = 0;
        }

        // Check that the current target slot isn't already filled
        if (inv.getStackInSlot(this.seqBufWrite) != null)
        {
            this.seqBufWrite = InventoryUtils.getFirstEmptySlot(inv);

            if (this.seqBufWrite == -1)
            {
                this.mode = nextMode;
                this.seqBufWrite = 0;
                return stack;
            }
        }

        stack = inv.insertItem(this.seqBufWrite, stack, simulate);

        if (simulate == false && ++this.seqBufWrite >= inv.getSlots())
        {
            this.mode = nextMode;
            this.seqBufWrite = 0;
        }

        return stack;
    }

    protected void checkForSequenceMatch(ItemStack stack)
    {
        //System.out.printf("checking item at pos: %d\n", this.seqBufWrite);
        IItemHandlerModifiable invSequenceBuffer = this.resetSequenceBuffer;
        IItemHandler invReferenceSequence = this.resetItems;

        if (this.seqBufWrite < invReferenceSequence.getSlots() && // FIXME remove this check after the bugs are gone...
            InventoryUtils.areItemStacksEqual(stack, invReferenceSequence.getStackInSlot(this.seqBufWrite)) == true)
        {
            //System.out.printf("%6d - rst item match - pos: %d, mode: %s, stack: %s\n",
            //        System.currentTimeMillis() % 100000, this.seqBufWrite, this.mode, stack);

            if ((this.seqBufWrite + 1) >= invReferenceSequence.getSlots())
            {
                this.reset();
            }
            else
            {
                //System.out.printf("adding to reset sequence at pos: %d\n", this.seqBufWrite);
                invSequenceBuffer.setStackInSlot(this.seqBufWrite, stack.copy());
                this.seqBufWrite++;
            }
        }
        // Encountered an item that breaks the currently monitored sequence
        else if (this.seqBufWrite > 0)
        {
            //System.out.printf("reset sequence broken at wr pos: %d\n", this.seqBufWrite);

            // Add the latest item to the buffer, if it's a new start
            if (InventoryUtils.areItemStacksEqual(stack, invReferenceSequence.getStackInSlot(0)) == true)
            {
                //System.out.printf("reset sequence broken - found new start item, adding to pos: %d\n", this.seqBufWrite);
                invSequenceBuffer.setStackInSlot(this.seqBufWrite, stack.copy());
                this.seqBufWrite++;
            }

            // Shift out the "matched sequence" buffer until the next possible start sequence in it, if any
            do
            {
                //System.out.printf("reset sequence broken, shifting buffer...\n");
                this.shiftSequenceBuffer(invSequenceBuffer, invReferenceSequence);
            } while (this.sequenceBufferMatches(invSequenceBuffer, invReferenceSequence, this.seqBufWrite) == false);

            //System.out.printf("reset sequence broken - new wr pos: %d\n", this.seqBufWrite);
        }
    }

    protected boolean sequenceBufferMatches(IItemHandler invSequenceBuffer, IItemHandler invReference, int length)
    {
        int num = Math.min(length, invReference.getSlots());
        //System.out.printf("sequenceBufferMatches(): len: %d\n", num);

        for (int slot = 0; slot < num; slot++)
        {
            if (InventoryUtils.areItemStacksEqual(invSequenceBuffer.getStackInSlot(slot), invReference.getStackInSlot(slot)) == false)
            {
                return false;
            }
        }

        return true;
    }

    protected int getNextSequenceStartIndex(IItemHandler invSequenceBuffer, IItemHandler invReference, int start, int length)
    {
        int num = Math.min(length, invReference.getSlots());

        for (int slot = start ; slot < num; slot++)
        {
            if (InventoryUtils.areItemStacksEqual(invSequenceBuffer.getStackInSlot(slot), invReference.getStackInSlot(0)) == true)
            {
                return slot;
            }
        }

        return -1;
    }

    protected void shiftSequenceBuffer(IItemHandlerModifiable invSequenceBuffer, IItemHandler invReference)
    {
        // Find a valid start position
        int readPos = this.getNextSequenceStartIndex(invSequenceBuffer, invReference, 1, this.seqBufWrite);
        int numSlots = invSequenceBuffer.getSlots();
        int writePos = 0;
        //System.out.printf("shiftSequenceBuffer() start - readPos: %d writePos: %d numSlots: %d seqBufWr: %d\n", readPos, writePos, numSlots, this.seqBufWrite);

        if (readPos >= 0)
        {
            // Shift the items starting from that valid start position, into the start of the buffer
            while (true)
            {
                //System.out.printf("shiftSequenceBuffer() - looping... readPos: %d writePos: %d\n", readPos, writePos);
                if (readPos >= this.seqBufWrite)
                {
                    //System.out.printf("shiftSequenceBuffer() - looping... breaking out at readPos: %d writePos: %d\n", readPos, writePos);
                    this.seqBufWrite = writePos;
                    break;
                }

                invSequenceBuffer.setStackInSlot(writePos, invSequenceBuffer.getStackInSlot(readPos));
                readPos++;
                writePos++;
            }
        }
        else
        {
            this.seqBufWrite = 0;
        }

        //System.out.printf("shiftSequenceBuffer() middle - readPos: %d writePos: %d numSlots: %d seqBufWr: %d\n", readPos, writePos, numSlots, this.seqBufWrite);

        // Clear the end of the buffer where the old matched items were
        for ( ; writePos < numSlots; writePos++)
        {
            invSequenceBuffer.setStackInSlot(writePos, null);
        }
    }

    protected ItemStack sortItem(ItemStack stack, boolean simulate)
    {
        if (simulate == false)
        {
            this.checkForSequenceMatch(stack);
        }

        if (InventoryUtils.getSlotOfFirstMatchingItemStack(this.filterItems, stack) != -1)
        {
            return InventoryUtils.tryInsertItemStackToInventoryStackFirst(this.filteredOut, stack, simulate);
        }

        return InventoryUtils.tryInsertItemStackToInventoryStackFirst(this.othersOut, stack, simulate);
    }

    protected IItemHandler getResetPhaseFilterItemsOutInventory()
    {
        return this.filteredOut;
    }

    protected void reset()
    {
        //System.out.printf("=== RESET ===\n");
        IItemHandlerModifiable invSequenceBuffer = this.resetSequenceBuffer;
        IItemHandler invReferenceSequence = this.resetItems;
        // Dump the reset sequence inventory and the filter item inventory into the output inventory
        InventoryUtils.tryMoveAllItems(invReferenceSequence, this.othersOut);
        InventoryUtils.tryMoveAllItems(this.filterItems, this.getResetPhaseFilterItemsOutInventory());
        this.mode = EnumMode.RESET;
        this.seqBufWrite = 0;

        for (int i = 0; i < invSequenceBuffer.getSlots(); i++)
        {
            invSequenceBuffer.setStackInSlot(i, null);
        }
    }

    public enum EnumMode
    {
        ACCEPT_RESET_ITEMS (0),
        ACCEPT_FILTER_ITEMS (1),
        SORT_ITEMS (2),
        OUTPUT_ITEMS (3),
        RESET (10);

        private final int id;

        EnumMode(int id)
        {
            this.id = id;
        }

        public int getId()
        {
            return this.id;
        }

        public static EnumMode fromId(int id)
        {
            for (EnumMode mode : values())
            {
                if (mode.getId() == id)
                {
                    return mode;
                }
            }

            return ACCEPT_RESET_ITEMS;
        }
    }
}
