package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.ContainerTrashBin;
import fi.dy.masa.autoverse.tileentity.TileEntityTrashBin;

public class GuiTrashBin extends GuiAutoverse
{
    private final TileEntityTrashBin te;
    private final ContainerTrashBin container;
    private int yOffset;

    public GuiTrashBin(ContainerTrashBin container, TileEntityTrashBin te, boolean isBuffer)
    {
        super(container, 176, 168, "gui.container.trash_" + (isBuffer ? "buffer" : "bin"));

        this.container = container;
        this.te = te;

        if (isBuffer)
        {
            this.ySize = 220;
            this.yOffset = 52;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        long count = this.container.getTrashedCount();
        String feedback = this.getUserFeedback(count);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.trash.count", count),        8, 44 + this.yOffset, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.trash.feedback", feedback),  8, 55 + this.yOffset, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 75 + this.yOffset, 0x404040);
    }

    private String getUserFeedback(long trashedCount)
    {
        if (trashedCount >= 10000) return "YOU ARE A MONSTER!! ;_;";
        if (trashedCount >=  1000) return "HOW COULD YOU?! :'-C";
        if (trashedCount >=   100) return "IT HURTS! :'-(";
        if (trashedCount >=    64) return ":-<";
        if (trashedCount >=    32) return ":-(";
        if (trashedCount >=    16) return ":-/";
        if (trashedCount >      0) return "¯\\_(ツ)_/¯";
        return "<3";
    }
}
