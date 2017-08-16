package fi.dy.masa.autoverse.gui.client.button;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.autoverse.item.base.ItemAutoverse;

public class GuiButtonHoverText extends GuiButtonIcon
{
    protected ArrayList<String> hoverStrings;

    public GuiButtonHoverText(int id, int x, int y, int w, int h, int u, int v,
            ResourceLocation texture, int hoverOffsetU, int hoverOffsetV, String ... hoverStrings)
    {
        super(id, x, y, w, h, u, v, texture, hoverOffsetU, hoverOffsetV);
        this.hoverStrings = new ArrayList<String>();

        for (String text : hoverStrings)
        {
            ItemAutoverse.addTranslatedTooltip(text, this.hoverStrings, false);
        }
    }

    public List<String> getHoverStrings()
    {
        return this.hoverStrings;
    }
}
