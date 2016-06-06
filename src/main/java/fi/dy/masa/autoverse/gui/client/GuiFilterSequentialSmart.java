package fi.dy.masa.autoverse.gui.client;

import fi.dy.masa.autoverse.inventory.container.ContainerFilterSequential;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;

public class GuiFilterSequentialSmart extends GuiFilterSequential
{
    public GuiFilterSequentialSmart(ContainerFilterSequential container, TileEntityFilterSequential te)
    {
        super(container, te, true);
    }

    @Override
    protected void drawFilterPosition(int x, int y)
    {
    }
}
