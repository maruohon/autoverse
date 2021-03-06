package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverseTile;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitterLength;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class GuiSplitterLength extends GuiAutoverseTile
{
    private final ContainerSplitterLength containerS;
    private final TileEntitySplitter te;

    public GuiSplitterLength(ContainerSplitterLength container, TileEntitySplitter te)
    {
        // The same gui background as the Block Detector
        super(container, 176, 256, "gui.container.block_detector", te);

        this.containerS = container;
        this.te = te;
        this.infoArea = new InfoArea(7, 36, 11, 11, "autoverse.gui.infoarea.splitter_length");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.splitter_length";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        s = I18n.format("autoverse.gui.label.rst");
        this.fontRenderer.drawString(s, 96 - this.fontRenderer.getStringWidth(s), 16, 0x404040);

        s = I18n.format("autoverse.gui.label.end");
        this.fontRenderer.drawString(s, 45, 24, 0x404040);

        s = I18n.format("autoverse.gui.label.1_bit_marker");
        this.fontRenderer.drawString(s, 45, 42, 0x404040);

        s = I18n.format("autoverse.gui.label.muxer_programmable.count_num", 1);
        this.fontRenderer.drawString(s,  8, 54, 0x404040);

        s = I18n.format("autoverse.gui.label.muxer_programmable.count_num", 2);
        this.fontRenderer.drawString(s,  8, 104, 0x404040);

        s = I18n.format("autoverse.gui.label.splitter.output", 1);
        this.fontRenderer.drawString(s, 28, 152, 0x404040);

        s = I18n.format("autoverse.gui.label.splitter.output", 2);
        this.fontRenderer.drawString(s, 148 - this.fontRenderer.getStringWidth(s), 152, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.bindTexture(this.guiTextureWidgets);

        this.drawSlotBackgrounds( 97,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(  7,  64, 0, 238, 8, 16); // Config: Side 1 length
        this.drawSlotBackgrounds(  7, 113, 0, 238, 8, 16); // Config: Side 2 length

        this.drawSlotBackgrounds(  7, 150, 0, 220, 1, 1); // Output 1

        // Draw the colored background for the active output
        int x = this.containerS.secondary ? 151 : 7;
        this.drawTexturedModalRect(this.guiLeft + x, this.guiTop + 150, 238, 36, 18, 18);

        // Draw the button outline
        this.drawTexturedModalRect(this.guiLeft + 158, this.guiTop + 64, 210, 20, 10, 10);
    }

    @Override
    protected void createButtons()
    {
        this.addButton(new GuiButtonHoverText(0, this.guiLeft + 159, this.guiTop + 65, 8, 8, 0, 24,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.splitter_length.toggle"));
    }
}
