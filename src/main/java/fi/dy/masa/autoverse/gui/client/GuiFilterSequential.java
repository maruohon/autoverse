package fi.dy.masa.autoverse.gui.client;

import fi.dy.masa.autoverse.inventory.container.ContainerFilterSequential;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;

public class GuiFilterSequential extends GuiFilter
{
    protected final ContainerFilterSequential containerFS;

    public GuiFilterSequential(ContainerFilterSequential container, TileEntityFilterSequential te)
    {
        super(container, te);
        this.containerFS = container;
    }

    protected void drawFilterPosition(int x, int y)
    {
        this.bindTexture(this.guiTextureWidgets);

        x += (this.containerFS.filterPosition % 9) * 18 + 7;
        y += (this.containerFS.filterPosition / 9) * 18 + 65;

        // Draw the colored background for the currently checked filter item
        this.drawTexturedModalRect(x, y, 102, 36, 18, 18);
    }

    @Override
    protected void coverSlots(int x, int y)
    {
        int tier = this.getTier();

        // Reset sequence slots
        this.drawTexturedModalRect(x + 133 + tier * 18, y + 15, 3, 33, (2 - tier) * 18, 18);

        // Reset sequence matcher slots
        this.drawTexturedModalRect(x + 133 + tier * 18, y + 35, 3, 33, (2 - tier) * 18, 18);

        // Cover the unavailable slots for the lower tier blocks (the GUI texture is for the highest tier variant)
        if (tier == 0)
        {
            // First row of filter slots
            this.drawTexturedModalRect(x +  79, y + 65, 3, 33, 5 * 18, 18);

            // Second row of filter slots
            this.drawTexturedModalRect(x +   7, y + 83, 3, 33, 5 * 18, 18);
            this.drawTexturedModalRect(x +  97, y + 83, 3, 33, 4 * 18, 18);
        }
        else if (tier == 1)
        {
            // Second row of filter slots
            this.drawTexturedModalRect(x +   7, y + 83, 3, 33, 5 * 18, 18);
            this.drawTexturedModalRect(x +  97, y + 83, 3, 33, 4 * 18, 18);
        }

        this.drawFilterPosition(x, y);
    }
}
