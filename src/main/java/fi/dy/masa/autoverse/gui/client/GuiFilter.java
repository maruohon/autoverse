package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.inventory.container.ContainerFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;

public class GuiFilter extends GuiAutoverse
{
    private final TileEntityFilter te;

    public GuiFilter(ContainerFilter container, TileEntityFilter te)
    {
        super(container, 176, 256, "gui.container.filter");
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRendererObj.drawString(s, this.xSize / 2 - this.fontRendererObj.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRendererObj.drawString(I18n.format("autoverse.gui.label.resetsequence"),     8,  19, 0x404040);
        this.fontRendererObj.drawString(I18n.format("autoverse.gui.label.filteritems"),       8,  52, 0x404040);
        this.fontRendererObj.drawString(I18n.format("autoverse.gui.label.filteroutbuffer"),   8, 103, 0x404040);
        this.fontRendererObj.drawString(I18n.format("autoverse.gui.label.nonmatchoutbuffer"), 8, 136, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        int tier = this.te.getFilterTier();

        // Cover the unavailable slots for the lower tier blocks (the GUI texture is for the highest tier variant)
        if (tier == 0)
        {
            // Reset sequence slots
            this.drawTexturedModalRect(x + 43, y + 28, 7, 3, 2 * 18, 18);

            // Reset sequence buffer slots
            this.drawTexturedModalRect(x + 133, y + 28, 7, 3, 2 * 18, 18);

            // First row of filter slots
            this.drawTexturedModalRect(x + 25, y + 61, 7, 3, 8 * 18, 18);

            // Second row of filter slots
            this.drawTexturedModalRect(x +  7, y + 79, 7, 3, 9 * 18, 18);
        }
        else if (tier == 1)
        {
            // Reset sequence slots
            this.drawTexturedModalRect(x + 61, y + 28, 7, 3, 18, 18);

            // Reset sequence buffer slots
            this.drawTexturedModalRect(x + 151, y + 28, 7, 3, 18, 18);

            // Second row of filter slots
            this.drawTexturedModalRect(x +  7, y + 79, 7, 3, 9 * 18, 18);
        }

        /*this.bindTexture(this.guiTextureWidgets);

        this.drawTexturedModalRect(x + 11 + exCol * 18, y + 12 + exRow * 18, 102, 54, 18, 18);*/
    }
}
