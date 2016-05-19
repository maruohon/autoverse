package fi.dy.masa.autoverse.gui.client;

import fi.dy.masa.autoverse.inventory.container.ContainerFilterSequential;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;

public class GuiFilterSequentialSmart extends GuiFilterSequential
{
    public GuiFilterSequentialSmart(ContainerFilterSequential container, TileEntityFilterSequential te)
    {
        super(container, te);
    }

    @Override
    protected void drawFilterPosition(int x, int y)
    {
    }

    @Override
    protected void coverSlots(int x, int y)
    {
        super.coverSlots(x, y);

        // Cover the unavailable slots for the lower tier blocks (the GUI texture is for the highest tier variant)
        if (this.getTier() == 0)
        {
            // First row of filtered items buffer slots
            this.drawTexturedModalRect(x + 79, y + 113, 3, 33, 5 * 18, 18);
        }
    }
}
