package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.inventory.container.ContainerCrafter;
import fi.dy.masa.autoverse.tileentity.TileEntityCrafter;

public class GuiCrafter extends GuiAutoverse
{
    private final TileEntityCrafter tec;

    public GuiCrafter(ContainerCrafter container, TileEntityCrafter te)
    {
        super(container, 176, 256, "gui.container.crafter");
        this.tec = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.tec.hasCustomName() ? this.tec.getName() : I18n.format(this.tec.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.reset_sequence"),   98, 20, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"),            28, 38, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.end_marker"),       28, 56, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.empty_marker"),     28, 74, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.crafting_pattern"),  8, 86, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.crafting_grid"),    80, 86, 0x404040);

        s = I18n.format("autoverse.gui.label.out");
        this.fontRenderer.drawString(s, 148 - this.fontRenderer.getStringWidth(s), 158, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"),                   8, 162, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.drawSlotBackgrounds(97, 29, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
    }
}
