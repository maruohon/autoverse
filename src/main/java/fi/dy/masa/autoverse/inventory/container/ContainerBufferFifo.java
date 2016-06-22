package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperFifo;
import fi.dy.masa.autoverse.inventory.slot.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageSyncSlot;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;

public class ContainerBufferFifo extends ContainerCustomSlotClick implements IBaseInventory
{
    protected final IItemHandlerModifiable inventoryBase;
    protected final ItemHandlerWrapperFifo inventoryFifo;
    public int insertPos;
    public int extractPos;
    public boolean offsetSlots;

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

        for (int row = 0; row <= 8; row++)
        {
            for (int col = 0; col <= 12 && slot < TileEntityBufferFifo.NUM_SLOTS; col++, slot++)
            {
                this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, row * 13 + col, posX + col * 18, posY + row * 18));
            }
        }

        this.customInventorySlots = new MergeSlotRange(0, this.inventorySlots.size());
    }

    @Override
    public IItemHandlerModifiable getBaseInventory()
    {
        return this.inventoryBase;
    }

    protected boolean transferStackToPrioritySlots(EntityPlayer player, int slotNum, boolean reverse)
    {
        SlotItemHandlerGeneric slot = this.getSlotItemHandler(slotNum);
        if (slot == null || slot.getHasStack() == false || slot.canTakeStack(player) == false)
        {
            return false;
        }

        // This will try to only insert items into the insertPosition slot
        int size = slot.getStack().stackSize;
        MergeSlotRange range = new MergeSlotRange(this.insertPos, 1); // FIXME how does this go with the wrapping?

        for (int i = 0; i < size; i++)
        {
            this.transferStackToSlotRange(player, slotNum, range, false);
        }

        return true;
    }

    protected void forceSyncSlots()
    {
        // Sync the custom inventory
        for (int slot = 0; slot < this.inventoryBase.getSlots(); slot++)
        {
            ItemStack newStack = this.inventoryBase.getStackInSlot(slot);
            ItemStack oldStack = ItemStack.copyItemStack(newStack);
            this.inventoryItemStacks.set(slot, oldStack);

            for (int i = 0; i < this.listeners.size(); i++)
            {
                IContainerListener listener = this.listeners.get(i);

                if (listener instanceof EntityPlayerMP)
                {
                    PacketHandler.INSTANCE.sendTo(
                        new MessageSyncSlot(this.windowId, slot, oldStack), (EntityPlayerMP) listener);
                }
            }
        }
    }

    protected void syncInventory()
    {
        // Sync the custom inventory
        for (int slot = 0; slot < this.inventoryBase.getSlots(); slot++)
        {
            ItemStack newStack = this.inventoryBase.getStackInSlot(slot);
            ItemStack oldStack = this.inventoryItemStacks.get(slot);

            if (ItemStack.areItemStacksEqual(oldStack, newStack) == false)
            {
                oldStack = ItemStack.copyItemStack(newStack);
                this.inventoryItemStacks.set(slot, oldStack);

                for (int i = 0; i < this.listeners.size(); i++)
                {
                    IContainerListener listener = this.listeners.get(i);

                    if (listener instanceof EntityPlayerMP)
                    {
                        PacketHandler.INSTANCE.sendTo(
                            new MessageSyncSlot(this.windowId, slot, oldStack), (EntityPlayerMP) listener);
                    }
                }
            }
        }

        // Sync player inventory slots
        for (int slot = this.inventoryBase.getSlots(); slot < this.inventorySlots.size(); slot++)
        {
            ItemStack newStack = this.inventorySlots.get(slot).getStack();
            ItemStack oldStack = this.inventoryItemStacks.get(slot);

            if (ItemStack.areItemStacksEqual(oldStack, newStack) == false)
            {
                oldStack = ItemStack.copyItemStack(newStack);
                this.inventoryItemStacks.set(slot, oldStack);

                for (int i = 0; i < this.listeners.size(); i++)
                {
                    this.listeners.get(i).sendSlotContents(this, slot, oldStack);
                }
            }
        }
    }

    @Override
    public void addListener(IContainerListener listener)
    {
        if (this.listeners.contains(listener))
        {
            throw new IllegalArgumentException("Listener already listening");
        }
        else
        {
            this.listeners.add(listener);

            if (listener instanceof EntityPlayerMP)
            {
                ((EntityPlayerMP)listener).connection.sendPacket(
                    new SPacketSetSlot(-1, -1, ((EntityPlayerMP)listener).inventory.getItemStack()));
            }
        }

        listener.sendProgressBarUpdate(this, 0, this.inventoryFifo.getInsertSlot());
        listener.sendProgressBarUpdate(this, 1, this.inventoryFifo.getExtractSlot());
        listener.sendProgressBarUpdate(this, 2, Configs.fifoBufferUseWrappedInventory ? 1 : 0);

        this.forceSyncSlots();
        this.syncInventory();
    }

    @Override
    public void detectAndSendChanges()
    {
        for (int i = 0; i < this.listeners.size(); ++i)
        {
            IContainerListener listener = this.listeners.get(i);

            if (this.inventoryFifo.getInsertSlot() != this.insertPos)
            {
                listener.sendProgressBarUpdate(this, 0, this.inventoryFifo.getInsertSlot());
            }

            if (this.inventoryFifo.getExtractSlot() != this.extractPos)
            {
                listener.sendProgressBarUpdate(this, 1, this.inventoryFifo.getExtractSlot());
            }

            if (Configs.fifoBufferUseWrappedInventory != this.offsetSlots)
            {
                listener.sendProgressBarUpdate(this, 2, Configs.fifoBufferUseWrappedInventory ? 1 : 0);
            }
        }

        this.insertPos = this.inventoryFifo.getInsertSlot();
        this.extractPos = this.inventoryFifo.getExtractSlot();
        this.offsetSlots = Configs.fifoBufferUseWrappedInventory;

        this.syncInventory();
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        super.updateProgressBar(id, data);

        switch (id)
        {
            case 0: this.insertPos = data; break;
            case 1: this.extractPos = data; break;
            case 2: Configs.fifoBufferUseWrappedInventory = data != 0; break;
            default:
        }
    }
}
