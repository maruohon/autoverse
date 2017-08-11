package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.ContainerFilterSequentialStrict;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequentialStrict;

public class GuiFilterSequentialStrict extends GuiAutoverse
{
    private final ContainerFilterSequentialStrict containerF;
    private final TileEntityFilterSequentialStrict te;

    public GuiFilterSequentialStrict(ContainerFilterSequentialStrict container, TileEntityFilterSequentialStrict te)
    {
        super(container, 176, 256, "gui.container.filter");

        this.containerF = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2,  4, 0x404040);
        s = I18n.format("autoverse.gui.label.rst");
        this.fontRenderer.drawString(s, 96 - this.fontRenderer.getStringWidth(s),  17, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.in"),              8,  35, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.end"),            45,  25, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.filter_items"),    8,  53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.out_normal"),     28, 154, 0x404040);

        s = I18n.format("autoverse.gui.label.out_filtered");
        this.fontRenderer.drawString(s, 148 - this.fontRenderer.getStringWidth(s), 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.drawSlotBackgrounds( 97,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(  7,  62, 0, 238, 9, this.container.getSequenceLength(1)); // Filter sequence
        this.drawSlotBackgrounds(  7, 153, 0, 220, 1, 1); // Out Normal
        this.drawSlotBackgrounds(151, 153, 0, 220, 1, 1); // Out Filtered

        for (int slot = 0, y = 62; slot < this.containerF.getMatchedLength(); slot++)
        {
            this.drawTexturedModalRect(this.guiLeft + 7 + slot * 18, this.guiTop + y, 238, 126, 18, 18);

            if (slot % 9 == 8)
            {
                y += 18;
            }
        }
    }
}
