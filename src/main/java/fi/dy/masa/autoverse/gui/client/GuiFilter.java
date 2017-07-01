package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
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
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 4, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.reset_sequence"),      42,  16, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.matched_sequence"),    54,  35, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.filter_items"),         8,  46, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.normal_out_buffer"),    8, 141, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.filtered_out_buffer"), 98, 141, 0x404040);

        if (this.te instanceof TileEntityFilterSequential)
        {
            this.fontRenderer.drawString(I18n.format("autoverse.gui.label.filtered_seq_buffer"),  8,  93, 0x404040);
        }
    }

    protected int getTier()
    {
        return this.te.getFilterTier();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.renderAllSlotBackgrounds();
    }

    protected void renderAllSlotBackgrounds()
    {
        int resetSlots = this.te.getResetSlotCount();
        int filterSlots = this.te.getFilterSlotCount();

        if (this.te instanceof TileEntityFilterSequential)
        {
            // Filtered items buffer slots
            this.renderSlotBackgrounds(7, 102, 7, 173, 9, filterSlots);
        }

        this.bindTexture(this.guiTextureWidgets);

        // Reset sequence slots
        this.renderSlotBackgrounds(97, 15, 0, 238, 4, resetSlots);

        // Reset sequence matched slots
        this.renderSlotBackgrounds(97, 33, 0, 238, 4, resetSlots);

        // Filter item slots
        this.renderSlotBackgrounds( 7, 55, 0, 238, 9, filterSlots);
    }

    protected void renderSlotBackgrounds(int x, int y, int u, int v, int maxPerRow, int count)
    {
        // Draw the slot backgrounds according to how many slots this tier has
        int rows = Math.max((int) (Math.ceil((double) count / maxPerRow)), 1);

        for (int row = 0; row < rows; row++)
        {
            int rowLen = MathHelper.clamp(count - (row * maxPerRow), 1, maxPerRow);

            // Render slots from the player inventory's first row into the Sequencer
            this.drawTexturedModalRect(this.guiLeft + x, this.guiTop + y + row * 18, u, v, rowLen * 18, 18);
        }
    }
}
