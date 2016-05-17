package fi.dy.masa.autoverse.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
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
    protected int slotPosition;
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
        this.slotPosition = 0;
        this.mode = EnumMode.ACCEPT_RESET_ITEMS;

        this.initMode();
    }

    public EnumMode getMode()
    {
        return this.mode;
    }

    public void setMode(EnumMode mode)
    {
        this.mode = mode;
    }

    protected void initMode()
    {
        switch (this.mode)
        {
            case ACCEPT_RESET_ITEMS:
                this.seqBufWrite = InventoryUtils.getFirstEmptySlot(this.resetItems);
                if (this.seqBufWrite < 0)
                {
                    this.mode = EnumMode.ACCEPT_FILTER_ITEMS;
                    this.seqBufWrite = 0;
                }
                break;
            case ACCEPT_FILTER_ITEMS:
                this.seqBufWrite = InventoryUtils.getFirstEmptySlot(this.filterItems);
                if (this.seqBufWrite < 0)
                {
                    this.mode = EnumMode.SORT_ITEMS;
                    this.seqBufWrite = 0;
                }
                break;
            default:
        }
    }

    public IItemHandler getSequenceBuffer()
    {
        return this.resetSequenceBuffer;
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        //tag.setByte("SlotPos", (byte)this.slotPosition);
        tag.setByte("SeqWr", (byte)this.seqBufWrite);
        tag.setByte("Mode", (byte)this.mode.getId());
        tag.merge(this.resetSequenceBuffer.serializeNBT());

        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag)
    {
        //this.slotPosition = tag.getByte("SlotPos");
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
        //System.out.printf("%6d : insert - simulate: %s stack: %s mode: %s slotPos: %d wrPos: %d\n",
        //        System.currentTimeMillis() % 100000, simulate, stack, this.mode, this.slotPosition, this.seqBufWrite);
        String side = this.te.getWorld().isRemote ? "client" : "server";
        System.out.printf("%s - %s - INSERT - wrPos: %d stack: %s mode: %s\n",
                side, simulate, this.seqBufWrite, stack, this.mode);

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
                break;

            case RESET:
                if (InventoryUtils.isInventoryEmpty(this.filteredOut) && InventoryUtils.isInventoryEmpty(this.othersOut))
                {
                    this.mode = EnumMode.ACCEPT_RESET_ITEMS;
                }
                break;

            default:
        }

        System.out.printf("======== insert end ========\n");
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

    protected ItemStack sortItem(ItemStack stack, boolean simulate)
    {
        if (simulate == false)
        {
            this.checkForSequenceMatch(stack, this.resetSequenceBuffer, this.resetItems);
        }

        if (InventoryUtils.getSlotOfFirstMatchingItemStack(this.filterItems, stack) != -1)
        {
            return InventoryUtils.tryInsertItemStackToInventory(this.filteredOut, stack, simulate);
        }

        return InventoryUtils.tryInsertItemStackToInventory(this.othersOut, stack, simulate);
    }

    protected void checkForSequenceMatch(ItemStack stack, IItemHandlerModifiable invSequenceBuffer, IItemHandler invReferenceSequence)
    {
        //System.out.printf("checking item at pos: %d\n", this.seqBufWrite);

        if (this.seqBufWrite < invReferenceSequence.getSlots() && // FIXME remove this check after the bugs are gone...
            InventoryUtils.areItemStacksEqual(stack, invReferenceSequence.getStackInSlot(this.seqBufWrite)) == true)
        {
            //System.out.printf("%6d - rst item match - pos: %d, mode: %s, stack: %s\n",
            //        System.currentTimeMillis() % 100000, this.seqBufWrite, this.mode, stack);

            if ((this.seqBufWrite + 1) >= invReferenceSequence.getSlots())
            {
                System.out.printf("=== RESET ===\n");
                // Dump the reset sequence inventory and the filter item inventory into the output inventory
                InventoryUtils.tryMoveAllItems(invReferenceSequence, this.othersOut);
                InventoryUtils.tryMoveAllItems(this.filterItems, this.filteredOut);
                this.mode = EnumMode.RESET;
                this.seqBufWrite = 0;
                //this.slotPosition = 0;

                for (int i = 0; i < invSequenceBuffer.getSlots(); i++)
                {
                    invSequenceBuffer.setStackInSlot(i, null);
                }
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

            do
            {
                //System.out.printf("reset sequence broken, shifting buffer...\n");
                this.shiftSequenceBuffer(invSequenceBuffer, invReferenceSequence);
            } while (this.sequenceBufferMatches(invSequenceBuffer, invReferenceSequence, this.seqBufWrite) == false);

            //System.out.printf("reset sequence broken - new wr pos: %d\n", this.seqBufWrite);
        }

        this.te.scheduleBlockTick(1, false);
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

        for ( ; writePos < numSlots; writePos++)
        {
            invSequenceBuffer.setStackInSlot(writePos, null);
        }
    }

    public enum EnumMode
    {
        ACCEPT_RESET_ITEMS (0),
        ACCEPT_FILTER_ITEMS (1),
        SORT_ITEMS (2),
        RESET (3);

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
            return values()[MathHelper.clamp_int(id, 0, values().length - 1)];
        }
    }
}
