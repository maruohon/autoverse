package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockPlacerProgrammable;

public class GuiBlockPlacerProgrammable extends GuiAutoverse
{
    private final TileEntityBlockPlacerProgrammable te;

    public GuiBlockPlacerProgrammable(ContainerAutoverse container, TileEntityBlockPlacerProgrammable te)
    {
        super(container, 248, 256, "gui.container.block_placer_programmable");

        this.te = te;
        this.infoArea = new InfoArea(7, 36, 11, 11, "autoverse.gui.infoarea.block_placer_programmable");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.placer_programmable";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        s = I18n.format("autoverse.gui.label.rst");
        this.fontRenderer.drawString(s, 166 - this.fontRenderer.getStringWidth(s), 16, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.block_placer_programmable.end_marker"),   45,  24, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.1_bit_marker"),                           45,  42, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.block_placer_programmable.trigger"),       8,  53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.block_placer_programmable.offset"),       89,  53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.block_placer_programmable.property", 1), 170,  53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.block_placer_programmable.property", 2),   8, 100, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.block_placer_programmable.property", 3),  89, 100, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.block_placer_programmable.property", 4), 170, 100, 0x404040);

        s = I18n.format("autoverse.gui.label.out");
        this.fontRenderer.drawString(s, 184 - this.fontRenderer.getStringWidth(s), 152, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 44, 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.drawSlotBackgrounds(169,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(  7,  62, 0, 238, this.container.getSequenceLength(1), this.container.getSequenceLength(1) * 2); // Trigger
    }
}
