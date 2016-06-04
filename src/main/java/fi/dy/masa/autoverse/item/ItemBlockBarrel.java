package fi.dy.masa.autoverse.item;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.util.NBTUtils;

public class ItemBlockBarrel extends ItemBlockAutoverse
{
    public ItemBlockBarrel(Block block)
    {
        super(block);
    }

    @SideOnly(Side.CLIENT)
    public void addInformationSelective(ItemStack stack, EntityPlayer player, List<String> list, boolean advancedTooltips, boolean verbose)
    {
        ItemStack storedStack = this.getStoredItemStack(stack);

        if (storedStack != null)
        {
            list.add(I18n.format("autoverse.tooltip.item.storeditem") + ": " + PRE_BLUE + storedStack.getDisplayName() + RST_GRAY);
            list.add(I18n.format("autoverse.tooltip.item.storedamount") + ": " + PRE_BLUE + storedStack.stackSize + RST_GRAY);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        String name = super.getItemStackDisplayName(stack);
        ItemStack storedStack = this.getStoredItemStack(stack);

        if (storedStack != null)
        {
            name = String.format("%s - %s%s%s (%d)", name, PRE_GREEN, storedStack.getDisplayName(), RST_WHITE, storedStack.stackSize);
        }

        return name;
    }

    protected ItemStack getStoredItemStack(ItemStack stack)
    {
        NBTTagCompound nbt = NBTUtils.getCompoundTag(stack, "BlockEntityTag", false);

        if (nbt != null)
        {
            ItemStack items[] = new ItemStack[1];
            NBTUtils.readStoredItemsFromTag(nbt, items, "Items");
            return items[0];
        }

        return null;
    }
}
