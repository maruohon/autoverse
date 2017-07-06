package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.client.HotKeys;
import fi.dy.masa.autoverse.client.HotKeys.EnumKey;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.ISlotOffset;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerOffset;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFifo;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class ContainerBufferFifo extends ContainerTile implements ISlotOffset
{
    private final TileEntityBufferFifo tefifo;
    protected final IItemHandlerModifiable inventoryBase;
    protected final ItemHandlerWrapperFifo inventoryFifo;
    private int insertPos = -1;
    private int extractPos = -1;
    private int invSize = -1;

    public ContainerBufferFifo(EntityPlayer player, TileEntityBufferFifo te)
    {
        super(player, te);

        this.tefifo = te;
        this.inventoryBase = te.getBaseItemHandler();
        this.inventoryFifo = te.getFifoInventory();
        this.reAddSlots();
    }

    private void reAddSlots()
    {
        this.reAddSlots(48, 177);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int posX = 12;
        int posY = 13;

        final int slots = this.inventory.getSlots();

        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), slots);

        //SlotPlacerSequence.create(12, 13, this.inventory, this).setMaxSlotsPerRow(13).placeSlots();

        for (int slot = 0, x = posX; slot < slots; slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerOffset(this.inventoryBase, slot, x, posY, this));
            x += 18;

            if (slot % 13 == 12)
            {
                x = posX;
                posY += 18;
            }
        }
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            int insert = this.inventoryFifo.getInsertSlot();
            int extract = this.inventoryFifo.getExtractSlot();
            int invSize = this.tefifo.getFifoLength();

            if (insert != this.insertPos)
            {
                this.syncProperty(0, insert);
                this.insertPos = insert;
            }

            if (extract != this.extractPos)
            {
                this.syncProperty(1, extract);
                this.extractPos = extract;
            }

            if (invSize != this.invSize)
            {
                this.syncProperty(2, invSize);
                this.invSize = invSize;
                this.reAddSlots();
            }
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        switch (id)
        {
            case 0:
                this.insertPos = value;
                break;

            case 1:
                this.extractPos = value;
                break;

            case 2:
                this.tefifo.setFifoLength(value);
                this.reAddSlots();
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

    @Override
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        Slot slot = this.getSlot(element);

        if (slot != null)
        {
            // Ctrl + Middle click: Shift the sequence
            if (EnumKey.MIDDLE_CLICK.matches(action, HotKeys.MOD_CTRL))
            {
                // Ctrl + middle click on a slot with items: Move the rest of the items towards the end of the inventory
                if (slot.getHasStack())
                {
                    InventoryUtils.tryShiftSlots(this.inventoryBase, slot.getSlotIndex(), false);
                }
                // Ctrl + middle click on a slot without items: Move the rest of the items towards the beginning of the inventory
                else
                {
                    InventoryUtils.tryShiftSlots(this.inventoryBase, slot.getSlotIndex(), true);
                }
            }
            // Shift + Middle click: Move the extract position to this slot
            else if (EnumKey.MIDDLE_CLICK.matches(action, HotKeys.MOD_SHIFT))
            {
                this.inventoryFifo.setExtractPosition(slot.getSlotIndex());
                this.tefifo.markDirty();
            }
            // Alt + Middle click: Move the insert position to this slot
            else if (EnumKey.MIDDLE_CLICK.matches(action, HotKeys.MOD_ALT))
            {
                this.inventoryFifo.setInsertPosition(slot.getSlotIndex());
                this.tefifo.markDirty();
            }
        }
    }
}
