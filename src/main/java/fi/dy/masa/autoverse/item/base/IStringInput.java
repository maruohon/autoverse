package fi.dy.masa.autoverse.item.base;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface IStringInput
{
    /**
     * Get the currently active/selected/whatever String from the item
     * @param player
     * @param stack
     * @return
     */
    public String getCurrentString(EntityPlayer player, ItemStack stack);

    /**
     * Handles the given String input, for example stores it as a name of something.
     * @param player
     * @param stack
     * @param text
     */
    public void handleString(EntityPlayer player, ItemStack stack, String text);
}
