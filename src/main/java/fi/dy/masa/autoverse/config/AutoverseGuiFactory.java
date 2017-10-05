package fi.dy.masa.autoverse.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import fi.dy.masa.autoverse.reference.Reference;

public class AutoverseGuiFactory extends DefaultGuiFactory
{
    public AutoverseGuiFactory()
    {
        super(Reference.MOD_ID, getTitle());
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent)
    {
        return new GuiConfig(parent, getConfigElements(), this.modid, false, false, this.title);
    }

    private static List<IConfigElement> getConfigElements()
    {
        List<IConfigElement> configElements = new ArrayList<IConfigElement>();

        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_CLIENT)));
        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_GENERIC)));
        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_BLOCK_PLACER)));

        return configElements;
    }

    private static String getTitle()
    {
        return GuiConfig.getAbridgedConfigPath(Configs.configurationFile.toString());
    }
}
