package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.ISlotOffset;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerOffset;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFifo;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;

public class ContainerBufferFifo extends ContainerTile implements ISlotOffset
{
    protected final IItemHandlerModifiable inventoryBase;
    protected final ItemHandlerWrapperFifo inventoryFifo;
    private int insertPos;
    private int extractPos;
    private int insertPosLast = -1;
    private int extractPosLast = -1;

    public ContainerBufferFifo(EntityPlayer player, TileEntityBufferFifo te)
    {
        super(player, te);

        this.inventoryBase = te.getBaseItemHandler();
        this.inventoryFifo = te.getFifoInventory();
        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(48, 177);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int posX = 12;
        int posY = 13;
        int slot = 0;

        for (int row = 0; row < 9; row++)
        {
            for (int col = 0; col < 13 && slot < TileEntityBufferFifo.NUM_SLOTS; col++, slot++)
            {
                this.addSlotToContainer(new SlotItemHandlerOffset(this.inventory, row * 13 + col, posX + col * 18, posY + row * 18, this));
            }
        }

        this.customInventorySlots = new MergeSlotRange(0, this.inventorySlots.size());
    }

    protected boolean transferStackToPrioritySlots(EntityPlayer player, int slotNum, boolean reverse)
    {
        SlotItemHandlerGeneric slot = this.getSlotItemHandler(slotNum);

        if (slot == null || slot.getHasStack() == false || slot.canTakeStack(player) == false)
        {
            return false;
        }

        // This will try to only insert items into the insertPosition slot
        MergeSlotRange range = new MergeSlotRange(this.insertPos, 1); // FIXME how does this go with the wrapping?
        this.transferStackToSlotRange(player, slotNum, range, false);

        return true;
    }

    @Override
    public void detectAndSendChanges()
    {
        int insert = this.inventoryFifo.getInsertSlot();
        int extract = this.inventoryFifo.getExtractSlot();

        for (int i = 0; i < this.listeners.size(); ++i)
        {
            IContainerListener listener = this.listeners.get(i);

            if (insert != this.insertPosLast)
            {
                listener.sendWindowProperty(this, 0, insert);
            }

            if (extract != this.extractPosLast)
            {
                listener.sendWindowProperty(this, 1, extract);
            }
        }

        this.insertPosLast = insert;
        this.extractPosLast = extract;

        super.detectAndSendChanges();
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        super.updateProgressBar(id, data);

        switch (id)
        {
            case 0:
                this.insertPos = data;
                break;

            case 1:
                this.extractPos = data;
                break;

            default:
        }
    }

    public int getInsertPosition()
    {
        return this.insertPos;
    }

    public int getExtractPosition()
    {
        return this.extractPos;
    }

    @Override
    public int getSlotOffset()
    {
        return this.isClient && Configs.fifoBufferOffsetSlots ? this.extractPos : 0;
    }

    /**
     * Offset and wrap the FIFO slot numbers. Requires 0-based indexing for the slots.
     * @param slot
     * @return
     */
    public int getOffsetSlotNumberPositive(int slot)
    {
        final int invSize = this.inventoryBase.getSlots();
        slot += this.getSlotOffset();

        if (slot >= invSize)
        {
            slot -= invSize;
        }

        return MathHelper.clamp(slot, 0, invSize - 1);

    }

    /**
     * Offset and wrap the FIFO slot numbers. Requires 0-based indexing for the slots.
     * @param slotNum
     * @return
     */
    public int getOffsetSlotNumberNegative(int slot)
    {
        final int invSize = this.inventoryBase.getSlots();
        slot -= this.getSlotOffset();

        if (slot < 0)
        {
            slot += invSize;
        }

        return MathHelper.clamp(slot, 0, invSize - 1);
    }

    @Override
    public ItemStack slotClick(int slotNum, int dragType, ClickType clickType, EntityPlayer player)
    {
        if (this.isClient && Configs.fifoBufferOffsetSlots && this.getCustomInventorySlotRange().contains(slotNum))
        {
            slotNum = this.getOffsetSlotNumberNegative(slotNum);
        }

        return super.slotClick(slotNum, dragType, clickType, player);
    }
}
