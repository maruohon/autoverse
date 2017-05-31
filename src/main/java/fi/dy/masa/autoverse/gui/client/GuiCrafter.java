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
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 4, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"),   8, 20, 0x404040);

        s = I18n.format("autoverse.gui.label.empty_marker");
        this.fontRenderer.drawString(s, -this.fontRenderer.getStringWidth(s) + 90,  20, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.crafting_grid"),     8,  60, 0x404040);

        s = I18n.format("autoverse.gui.label.crafting_pattern");
        this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 6,  60, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.output_buffer"),     8, 130, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"),                   8, 162, 0x404040);
    }
}
