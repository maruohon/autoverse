package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.inventory.slot.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageSyncSlot;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;

public class ContainerBufferFifo extends ContainerCustomSlotClick implements IBaseInventory
{
    protected final IItemHandlerModifiable inventoryBase;
    private final TileEntityBufferFifo tefifo;
    private int insertPos;
    private int extractPos;
    public boolean offsetSlots;

    public ContainerBufferFifo(EntityPlayer player, TileEntityBufferFifo te)
    {
        super(player, te);

        this.tefifo = te;
        this.inventoryBase = te.getBaseItemHandler();
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
                ICrafting crafter = this.listeners.get(i);

                if (crafter instanceof EntityPlayerMP)
                {
                    EntityPlayerMP player = (EntityPlayerMP)crafter;
                    PacketHandler.INSTANCE.sendTo(new MessageSyncSlot(this.windowId, slot, oldStack), player);
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
                    ICrafting crafter = this.listeners.get(i);

                    if (crafter instanceof EntityPlayerMP)
                    {
                        EntityPlayerMP player = (EntityPlayerMP)crafter;
                        PacketHandler.INSTANCE.sendTo(new MessageSyncSlot(this.windowId, slot, oldStack), player);
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
    public void onCraftGuiOpened(ICrafting listener)
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
                ((EntityPlayerMP)listener).playerNetServerHandler.sendPacket(new SPacketSetSlot(-1, -1, ((EntityPlayerMP)listener).inventory.getItemStack()));
            }
        }

        listener.sendProgressBarUpdate(this, 0, this.tefifo.getInsertSlot());
        listener.sendProgressBarUpdate(this, 1, this.tefifo.getExtractSlot());
        listener.sendProgressBarUpdate(this, 2, Configs.fifoBufferUseWrappedInventory ? 1 : 0);

        this.forceSyncSlots();
        this.syncInventory();
    }

    @Override
    public void detectAndSendChanges()
    {
        for (int i = 0; i < this.listeners.size(); ++i)
        {
            ICrafting listener = this.listeners.get(i);

            if (this.tefifo.getInsertSlot() != this.insertPos)
            {
                listener.sendProgressBarUpdate(this, 0, this.tefifo.getInsertSlot());
            }

            if (this.tefifo.getExtractSlot() != this.extractPos)
            {
                listener.sendProgressBarUpdate(this, 1, this.tefifo.getExtractSlot());
            }

            if (Configs.fifoBufferUseWrappedInventory != this.offsetSlots)
            {
                listener.sendProgressBarUpdate(this, 2, Configs.fifoBufferUseWrappedInventory ? 1 : 0);
            }
        }

        this.insertPos = this.tefifo.getInsertSlot();
        this.extractPos = this.tefifo.getExtractSlot();
        this.offsetSlots = Configs.fifoBufferUseWrappedInventory;

        this.syncInventory();
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        super.updateProgressBar(id, data);

        switch (id)
        {
            case 0: this.tefifo.setInsertSlot(data); break;
            case 1: this.tefifo.setExtractSlot(data); break;
            case 2: Configs.fifoBufferUseWrappedInventory = data != 0; break;
            default:
        }
    }
}
