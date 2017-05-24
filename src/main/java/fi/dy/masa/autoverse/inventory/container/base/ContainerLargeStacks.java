package fi.dy.masa.autoverse.inventory.container.base;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import fi.dy.masa.autoverse.client.HotKeys;
import fi.dy.masa.autoverse.client.HotKeys.EnumKey;

public class ContainerLargeStacks extends ContainerCustomSlotClick
{
    public ContainerLargeStacks(EntityPlayer player, IItemHandler inventory)
    {
        super(player, inventory);
    }

    @Override
    protected int getMaxStackSizeFromSlotAndStack(Slot slot, ItemStack stack)
    {
        // Our inventory
        if (slot instanceof SlotItemHandler && ((SlotItemHandler) slot).getItemHandler() == this.inventory)
        {
            return slot.getItemStackLimit(stack);
        }

        // Player inventory
        return super.getMaxStackSizeFromSlotAndStack(slot, stack);
    }

    @Override
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        // Shift + Middle click: Cycle the stack size in creative mode
        if (EnumKey.MIDDLE_CLICK.matches(action, HotKeys.MOD_SHIFT))
        {
            if (player.capabilities.isCreativeMode && this.inventoryNonWrapped != null)
            {
                this.cycleStackSize(element, this.inventoryNonWrapped);
            }
        }
        // Alt + Middle click: Swap two stacks
        else if (EnumKey.MIDDLE_CLICK.matches(action, HotKeys.MOD_ALT))
        {
            this.swapSlots(element, player);
        }
    }
}
