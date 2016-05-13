package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.inventory.container.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;

public class GuiBufferFifo extends GuiAutoverse
{
    private final TileEntityBufferFifo te;

    public GuiBufferFifo(ContainerAutoverse container, TileEntityBufferFifo te)
    {
        super(container, 256, 256, "gui.container." + te.getTEName());
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRendererObj.drawString(s, this.xSize / 2 - this.fontRendererObj.getStringWidth(s) / 2, 4, 0x404025);
    }

    /*@Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);
    }*/
}
