package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitter;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class GuiSplitter extends GuiAutoverse
{
    private final TileEntitySplitter te;

    public GuiSplitter(ContainerSplitter container, TileEntitySplitter te)
    {
        super(container, 176, 206, "gui.container.splitter");
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"),              8,  10, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.resetsequence"),     42,  20, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.matchedsequence"),   54,  39, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.sequenceitems"),      8,  50, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.splitter.out1"),      8,  83, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.splitter.out2"),     98,  83, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"),                    8, 115, 0x404040);
    }
}
