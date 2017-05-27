package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitter;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class GuiSplitter extends GuiAutoverse
{
    private final ContainerSplitter containerSP;
    private final TileEntitySplitter te;

    public GuiSplitter(ContainerSplitter container, TileEntitySplitter te)
    {
        super(container, 176, 206, "gui.container.splitter_togglable");
        this.containerSP = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.splitter_togglable";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"),             8,  19, 0x404040);

        s = I18n.format("autoverse.gui.label.resetsequence");
        this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 6,  20, 0x404040);

        s = I18n.format("autoverse.gui.label.splitter.switchingsequence");
        this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 6,  72, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.splitter.outputs"),  8,  72, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"),                   8, 114, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        // Draw the colored background for the active output
        this.bindTexture(this.guiTextureWidgets);
        Slot slot = this.container.getSlot(this.containerSP.secondaryOutput ? 2 : 1);
        this.drawTexturedModalRect(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 102, 18, 18, 18);
    }
}
