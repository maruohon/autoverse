package fi.dy.masa.autoverse.gui.client.base;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.reference.Reference;

public class CreativeTab
{
    public static final CreativeTabs AUTOVERSE_TAB = new CreativeTabs(Reference.MOD_ID)
    {
        @Override
        public ItemStack createIcon()
        {
            return new ItemStack(AutoverseBlocks.FILTER, 1, 1);
        }

        @Override
        public String getTranslationKey()
        {
            return Reference.MOD_NAME;
        }
    };
}
