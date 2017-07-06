package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitter;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class GuiSplitter extends GuiAutoverse
{
    private final ContainerSplitter containerSP;
    private final TileEntitySplitter te;

    public GuiSplitter(ContainerSplitter container, TileEntitySplitter te)
    {
        super(container, 176, 238, "gui.container.splitter_switchable");
        this.containerSP = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.splitter_switchable";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"),            26,  32, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.end"),              26,  50, 0x404040);

        s = I18n.format("autoverse.gui.label.reset_sequence");
        this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 7,  20, 0x404040);

        s = I18n.format("autoverse.gui.label.splitter.switching_sequence_num", 1);
        this.fontRenderer.drawString(s, 8, 72, 0x404040);

        s = I18n.format("autoverse.gui.label.splitter.switching_sequence_num", 2);
        this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 7,  72, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.splitter.output", 1),  28, 128, 0x404040);
        s = I18n.format("autoverse.gui.label.splitter.output", 2);
        this.fontRenderer.drawString(s, 148 - this.fontRenderer.getStringWidth(s), 128, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.bindTexture(this.guiTextureWidgets);

        this.drawSlotBackgrounds(97, 29, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds( 7, 82, 0, 238, this.container.getSequenceLength(1), this.container.getSequenceLength(1) * 2); // Switch sequence 1
        this.drawSlotBackgrounds(97, 82, 0, 238, this.container.getSequenceLength(2), this.container.getSequenceLength(2) * 2); // Switch sequence 2

        // Draw the colored background for the active output
        Slot slot = this.containerSP.getSlot(this.containerSP.secondaryOutput ? 2 : 1);
        this.drawTexturedModalRect(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 238, 36, 18, 18);
    }
}
