package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitter;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class GuiSplitter extends GuiAutoverse
{
    private final ContainerSplitter containerSP;
    private final TileEntitySplitter te;
    private final boolean selectable;

    public GuiSplitter(ContainerSplitter container, TileEntitySplitter te)
    {
        super(container, 176, 206, "gui.container.splitter_" + (te.isSelectable() ? "selectable" : "togglable"));
        this.containerSP = container;
        this.te = te;
        this.selectable = te.isSelectable();

        if (this.selectable)
        {
            this.ySize = 238;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.splitter_" + (this.selectable ? "selectable" : "togglable");
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"),             8,  19, 0x404040);

        s = I18n.format("autoverse.gui.label.reset_sequence");
        this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 6,  20, 0x404040);

        if (this.selectable)
        {
            s = I18n.format("autoverse.gui.label.splitter.switching_sequence_num", 1);
            this.fontRenderer.drawString(s, 8, 72, 0x404040);

            s = I18n.format("autoverse.gui.label.splitter.switching_sequence_num", 2);
            this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 6,  72, 0x404040);

            this.fontRenderer.drawString(I18n.format("autoverse.gui.label.splitter.output", 1),  28, 132, 0x404040);
            this.fontRenderer.drawString(I18n.format("autoverse.gui.label.splitter.output", 2), 116, 132, 0x404040);
            this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 145, 0x404040);
        }
        else
        {
            s = I18n.format("autoverse.gui.label.splitter.switching_sequence");
            this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 6,  72, 0x404040);

            this.fontRenderer.drawString(I18n.format("autoverse.gui.label.splitter.outputs"),  8,  72, 0x404040);
            this.fontRenderer.drawString(I18n.format("container.inventory"),                   8, 114, 0x404040);
        }
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
