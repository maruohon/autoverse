package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import fi.dy.masa.autoverse.inventory.container.ContainerFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;

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
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.resetsequence"),     42,  16, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.matchedsequence"),   54,  35, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.filteritems"),        8,  46, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.filteroutbuffer"),    8,  93, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.nonmatchoutbuffer"),  8, 141, 0x404040);
        //this.fontRenderer.drawString(I18n.format("container.inventory"),                    8, 164, 0x404040);
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
        int resetSlots = this.te.getNumResetSlots();
        int filterSlots = this.te.getNumFilterSlots();

        // Reset item slots
        this.renderSlotBackgrounds(97, 15, 7, 173, 4, resetSlots);
        // Reset sequence matcher slots
        this.renderSlotBackgrounds(97, 33, 176, 0, 4, resetSlots);
        // Filter template items
        this.renderSlotBackgrounds(7, 55, 7, 173, 9, filterSlots);
        // Filtered items buffer
        this.renderSlotBackgrounds(7, 102, 7, 173, 9, filterSlots);
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
