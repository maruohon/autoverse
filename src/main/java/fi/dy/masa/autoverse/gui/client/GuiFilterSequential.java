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

    @Override
    protected int getTier()
    {
        return super.getTier() + 1;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = ((this.width - this.xSize) / 2) + (this.containerFS.filterPosition % 9) * 18 + 7;
        int y = ((this.height - this.ySize) / 2) + (this.containerFS.filterPosition / 9) * 18 + 65;

        this.bindTexture(this.guiTextureWidgets);

        // Draw the colored background for the currently checked filter item
        this.drawTexturedModalRect(x, y, 102, 36, 18, 18);
    }
}
