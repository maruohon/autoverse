package fi.dy.masa.autoverse.item.block;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.util.NBTUtils;

public class ItemBlockStorage extends ItemBlockAutoverse
{
    public ItemBlockStorage(BlockAutoverse block)
    {
        super(block);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformationSelective(ItemStack stack, EntityPlayer player, List<String> list, boolean advancedTooltips, boolean verbose)
    {
        NBTUtils.getCachedInventoryStrings(stack, list, 9);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        return NBTUtils.getItemStackDisplayName(stack, super.getItemStackDisplayName(stack));
    }

    @Override
    public NBTTagCompound getNBTShareTag(ItemStack stack)
    {
        NBTTagCompound nbt = stack.getTagCompound();

        if (nbt != null && nbt.hasKey("InvCache", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound shareNBT = new NBTTagCompound();
            shareNBT.setTag("InvCache", nbt.getCompoundTag("InvCache").copy());
            return shareNBT;
        }

        return nbt;
    }
}
