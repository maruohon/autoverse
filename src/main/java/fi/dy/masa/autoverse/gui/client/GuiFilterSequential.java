package fi.dy.masa.autoverse.gui.client;

import fi.dy.masa.autoverse.inventory.container.ContainerFilterSequential;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;

public class GuiFilterSequential extends GuiFilter
{
    private final boolean drawFilterPosition;
    protected final ContainerFilterSequential containerFS;

    public GuiFilterSequential(ContainerFilterSequential container, TileEntityFilterSequential te, boolean isSmart)
    {
        super(container, te);
        this.drawFilterPosition = isSmart == false;
        this.containerFS = container;
    }

    @Override
    protected void renderAllSlotBackgrounds()
    {
        super.renderAllSlotBackgrounds();

        if (this.drawFilterPosition)
        {
            this.drawFilterPosition(this.guiLeft, this.guiTop);
        }
    }

    protected void drawFilterPosition(int x, int y)
    {
        this.bindTexture(this.guiTextureWidgets);

        int pos = this.containerFS.getFilterPosition();

        if (pos >= 0)
        {
            x += (pos % 9) * 18 + 7;
            y += (pos / 9) * 18 + 55;
        }

        // Draw the colored background for the currently matched filter item
        this.drawTexturedModalRect(x, y, 102, 36, 18, 18);
    }
}
