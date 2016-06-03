package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.items.SlotItemHandler;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageSyncSlot;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverseInventory;

public class ContainerLargeStacks extends ContainerCustomSlotClick
{
    public ContainerLargeStacks(EntityPlayer player, TileEntityAutoverseInventory te)
    {
        super(player, te);
    }

    @Override
    protected int getMaxStackSizeFromSlotAndStack(Slot slot, ItemStack stack)
    {
        // Our inventory
        if (slot instanceof SlotItemHandler && ((SlotItemHandler)slot).getItemHandler() == this.inventory)
        {
            return slot.getItemStackLimit(stack);
        }

        // Player inventory
        return super.getMaxStackSizeFromSlotAndStack(slot, stack);
    }

    @Override
    public void addListener(IContainerListener iCrafting)
    {
        if (this.listeners.contains(iCrafting))
        {
            throw new IllegalArgumentException("Listener already listening");
        }
        else
        {
            this.listeners.add(iCrafting);

            if (iCrafting instanceof EntityPlayerMP)
            {
                this.syncAllSlots((EntityPlayerMP)iCrafting);
                ((EntityPlayerMP)iCrafting).connection.sendPacket(new SPacketSetSlot(-1, -1, ((EntityPlayerMP)iCrafting).inventory.getItemStack()));
            }

            this.detectAndSendChanges();
        }
    }

    protected void syncAllSlots(EntityPlayerMP player)
    {
        for (int slot = 0; slot < this.inventorySlots.size(); slot++)
        {
            ItemStack stack = this.inventorySlots.get(slot).getStack();
            PacketHandler.INSTANCE.sendTo(new MessageSyncSlot(this.windowId, slot, stack), player);
        }
    }

    @Override
    public void detectAndSendChanges()
    {
        for (int slot = 0; slot < this.inventorySlots.size(); slot++)
        {
            ItemStack currentStack = this.inventorySlots.get(slot).getStack();
            ItemStack prevStack = this.inventoryItemStacks.get(slot);

            if (ItemStack.areItemStacksEqual(prevStack, currentStack) == false)
            {
                prevStack = ItemStack.copyItemStack(currentStack);
                this.inventoryItemStacks.set(slot, prevStack);

                for (int i = 0; i < this.listeners.size(); ++i)
                {
                    IContainerListener listener = this.listeners.get(i);
                    if (listener instanceof EntityPlayerMP)
                    {
                        EntityPlayerMP player = (EntityPlayerMP)listener;
                        PacketHandler.INSTANCE.sendTo(new MessageSyncSlot(this.windowId, slot, prevStack), player);
                    }
                }
            }
        }
    }
}
