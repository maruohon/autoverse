package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import fi.dy.masa.autoverse.inventory.container.ContainerSequenceDetector;
import fi.dy.masa.autoverse.tileentity.TileEntitySequenceDetector;

public class GuiSequenceDetector extends GuiAutoverse
{
    private final ContainerSequenceDetector containerSD;
    private final TileEntitySequenceDetector te;

    public GuiSequenceDetector(ContainerSequenceDetector container, TileEntitySequenceDetector te)
    {
        super(container, 176, 256, "gui.container.sequence_detector");
        this.containerSD = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.sequence_detector";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"), 26, 24, 0x404040);

        s = I18n.format("autoverse.gui.label.reset_sequence");
        this.fontRenderer.drawString(s, 96 - this.fontRenderer.getStringWidth(s), 16, 0x404040);

        s = I18n.format("autoverse.gui.label.end_marker");
        this.fontRenderer.drawString(s, 26, 42, 0x404040);

        s = I18n.format("autoverse.gui.label.sequence_detector.sequence");
        this.fontRenderer.drawString(s, 8, 150, 0x404040);

        s = I18n.format("autoverse.gui.label.output_buffer");
        this.fontRenderer.drawString(s, this.xSize - 28 - this.fontRenderer.getStringWidth(s), 163, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = this.guiLeft;
        int y = this.guiTop;

        this.bindTexture(this.guiTextureWidgets);

        // Draw the slot backgrounds according to how many slots the detector has at the moment
        final int invSize = this.containerSD.getSequenceLength();
        final int maxRowLength = 9;
        int rows = (int) (Math.ceil((double) invSize / maxRowLength));

        if (invSize > 0)
        {
            rows = Math.max(rows, 1);
        }

        for (int row = 0; row < rows; row++)
        {
            int rowLen = MathHelper.clamp(invSize - (row * maxRowLength), 1, maxRowLength);
            this.drawTexturedModalRect(x + 7, y + 55 + row * 18, 16, 238, rowLen * 18, 18);
        }

        // Draw the hilighted slot backgrounds according to how many slots the detector has matched thus far
        final int matched = this.containerSD.getMatchedLength();

        x = this.guiLeft + 7;
        y = this.guiTop + 55;

        for (int slot = 0; slot < matched; ++slot)
        {
            this.drawTexturedModalRect(x, y, 102, 144, 18, 18);

            x += 18;

            if (slot % 9 == 8)
            {
                y += 18;
                x = this.guiLeft + 7;
            }
        }
    }
}
