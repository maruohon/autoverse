package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockPlacer;

public class GuiBlockPlacer extends GuiAutoverse
{
    protected final TileEntityBlockPlacer te;

    public GuiBlockPlacer(ContainerAutoverse container, TileEntityBlockPlacer te)
    {
        super(container, 176, 192, "gui.container.block_placer");

        this.te = te;
        this.infoArea = new InfoArea(159, 6, 11, 11, "autoverse.gui.infoarea.block_placer_nbt");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 98, 0x404040);
    }
}
