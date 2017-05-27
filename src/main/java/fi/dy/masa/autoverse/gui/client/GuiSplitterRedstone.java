package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitterRedstone;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class GuiSplitterRedstone extends GuiAutoverse
{
    private final ContainerSplitterRedstone containerSP;
    private final TileEntitySplitter te;

    public GuiSplitterRedstone(ContainerSplitterRedstone container, TileEntitySplitter te)
    {
        super(container, 176, 146, "gui.container.splitter_redstone");
        this.containerSP = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format("autoverse.container.splitter_redstone");
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"), 8,  19, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.splitter.output_1"),  62, 20, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.splitter.output_2"), 116, 20, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 52, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        // Draw the colored background for the active output
        this.bindTexture(this.guiTextureWidgets);
        Slot slot = this.containerSP.getSlot(this.containerSP.secondaryOutput ? 2 : 1);
        this.drawTexturedModalRect(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 102, 18, 18, 18);
    }
}
