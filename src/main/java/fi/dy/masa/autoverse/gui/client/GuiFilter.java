package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.inventory.container.ContainerFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;

public class GuiFilter extends GuiAutoverse
{
    private final TileEntityFilter te;

    public GuiFilter(ContainerFilter container, TileEntityFilter te)
    {
        super(container, 176, 256, "gui.container.filter");
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
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.out_normal"),     28, 151, 0x404040);

        s = I18n.format("autoverse.gui.label.out_filtered");
        this.fontRenderer.drawString(s, 148 - this.fontRenderer.getStringWidth(s), 160, 0x404040);

        if (this.te instanceof TileEntityFilterSequential)
        {
            this.fontRenderer.drawString(I18n.format("autoverse.gui.label.filtered_seq_buffer"), 8, 100, 0x404040);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.drawSlotBackgrounds( 97,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(  7,  62, 0, 238, 9, this.container.getSequenceLength(1)); // Filter sequence
        this.drawSlotBackgrounds(  7, 150, 0, 202, 1, 1); // Out Normal
        this.drawSlotBackgrounds(151, 150, 0, 202, 1, 1); // Out Filtered

        if (this.te instanceof TileEntityFilterSequential)
        {
            // Filtered items buffer slots
            this.drawSlotBackgrounds(7, 109, 0, 238, 9, this.container.getSequenceLength(1));
        }
    }
}
