package fi.dy.masa.autoverse.gui.client;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.reference.Reference;

public class CreativeTab
{
    public static final CreativeTabs AUTOVERSE_TAB = new CreativeTabs(Reference.MOD_ID)
    {
        @SideOnly(Side.CLIENT)
        @Override
        public Item getTabIconItem()
        {
            return Item.getItemFromBlock(Blocks.DROPPER);
        }

        @SideOnly(Side.CLIENT)
        @Override
        public String getTranslatedTabLabel()
        {
            return Reference.MOD_NAME;
        }
    };
}
