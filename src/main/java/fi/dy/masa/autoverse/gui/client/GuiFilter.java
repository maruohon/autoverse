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
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"),              8,  35, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.resetsequence"),     42,  16, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.matchedsequence"),   54,  35, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.filteritems"),        8,  46, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.filteroutbuffer"),    8,  93, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.nonmatchoutbuffer"),  8, 141, 0x404040);
        //this.fontRenderer.drawString(I18n.format("container.inventory"),                    8, 164, 0x404040);
    }

    protected int getTier()
    {
        return this.te.getFilterTier();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        this.coverSlots(x, y);
    }

    protected void coverSlots(int x, int y)
    {
        int resetSlots = this.te.getNumResetSlots();
        int filterSlots = this.te.getNumFilterSlots();

        if (resetSlots < 4)
        {
            // Reset sequence slots
            this.drawTexturedModalRect(x + 133 + (resetSlots - 2) * 18, y + 15, 3, 33, (4 - resetSlots) * 18, 18);

            // Reset sequence matcher slots
            this.drawTexturedModalRect(x + 133 + (resetSlots - 2) * 18, y + 33, 3, 33, (4 - resetSlots) * 18, 18);
        }

        // First row of filter slots
        for (int i = filterSlots; i < 9; i++)
        {
            this.drawTexturedModalRect(x + 7 + i * 18, y + 55, 3, 33, 18, 18);
        }

        if (filterSlots < 18)
        {
            // Second row of filter slots
            this.drawTexturedModalRect(x +   7, y + 73, 3, 33, 5 * 18, 18);
            this.drawTexturedModalRect(x +  97, y + 73, 3, 33, 4 * 18, 18);

            // Second row of filtered items buffer slots
            this.drawTexturedModalRect(x +   7, y + 120, 3, 33, 5 * 18, 18);
            this.drawTexturedModalRect(x +  97, y + 120, 3, 33, 4 * 18, 18);
        }

        // Filtered items buffer slots (first row)
        for (int i = filterSlots; i < 9; i++)
        {
            this.drawTexturedModalRect(x + 7 + i * 18, y + 102, 3, 33, 18, 18);
        }
    }
}
