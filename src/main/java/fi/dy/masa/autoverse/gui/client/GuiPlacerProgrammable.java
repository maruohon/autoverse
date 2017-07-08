package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockPlacerProgrammable;

public class GuiPlacerProgrammable extends GuiAutoverse
{
    private final TileEntityBlockPlacerProgrammable te;

    public GuiPlacerProgrammable(ContainerAutoverse container, TileEntityBlockPlacerProgrammable te)
    {
        // Same GUI background as the filter
        super(container, 176, 256, "gui.container.filter");
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.placer_programmable";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        s = I18n.format("autoverse.gui.label.rst");
        this.fontRenderer.drawString(s, 96 - this.fontRenderer.getStringWidth(s), 16, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.placer_programmable.end_marker"),  45,  24, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.1_bit_marker"),                    45,  42, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.placer_programmable.trigger"),      8,  53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.placer_programmable.property", 1), 98,  53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.placer_programmable.property", 2),  8, 100, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.placer_programmable.property", 3), 98, 100, 0x404040);

        s = I18n.format("autoverse.gui.label.out");
        this.fontRenderer.drawString(s, this.xSize - 28 - this.fontRenderer.getStringWidth(s), 152, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.drawSlotBackgrounds( 25,  33, 0, 238, 1, 1); // 1-bit marker

        this.drawSlotBackgrounds( 97,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(  7,  62, 0, 238, this.container.getSequenceLength(1), this.container.getSequenceLength(1) * 2); // Trigger

        this.drawSlotBackgrounds( 97,  62, 0, 238, 4, 8); // Property 1
        this.drawSlotBackgrounds(  7, 109, 0, 238, 4, 8); // Property 2
        this.drawSlotBackgrounds( 97, 109, 0, 238, 4, 8); // Property 3

        this.drawSlotBackgrounds(151, 150, 0, 220, 1, 1); // Output
    }
}
