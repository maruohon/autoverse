package fi.dy.masa.autoverse.gui.client;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.reference.Reference;

public class CreativeTab
{
    public static final CreativeTabs AUTOVERSE_TAB = new CreativeTabs(Reference.MOD_ID)
    {
        @SideOnly(Side.CLIENT)
        @Override
        public ItemStack getTabIconItem()
        {
            return new ItemStack(AutoverseBlocks.FILTER, 1, 1);
        }

        @SideOnly(Side.CLIENT)
        @Override
        public String getTranslatedTabLabel()
        {
            return Reference.MOD_NAME;
        }
    };
}
