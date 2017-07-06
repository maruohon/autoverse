package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitter;

public class GuiRedstoneEmitter extends GuiAutoverse
{
    private final TileEntityRedstoneEmitter te;

    public GuiRedstoneEmitter(ContainerAutoverse container, TileEntityRedstoneEmitter te)
    {
        super(container, 176, 256, "gui.container.redstone_emitter");
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.redstone_emitter";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        s = I18n.format("autoverse.gui.label.reset_sequence");
        this.fontRenderer.drawString(s, 148 - this.fontRenderer.getStringWidth(s), 19, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"),                            8,  19, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.end_marker"),                      46,  38, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter.enabled_marker"), 46,  56, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter.side_config"),     8,  68, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter.sequence_on"),    98,  68, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter.sequence_off"),   98, 116, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.out"),                             28, 144, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        final int mask = this.te.getSideMask();

        int x = this.guiLeft + 7;
        int y = this.guiTop + 77;

        for (int i = 0; i < 6; ++i)
        {
            // Side enabled vs. disabled
            int vOffset = (mask & (1 << i)) != 0 ? 0 : 54;
            int xOffset = (i & 1) * 18;
            int yOffset = (i / 2) * 18;

            this.drawTexturedModalRect(x + xOffset, y + yOffset, 176 + xOffset, vOffset + yOffset, 18, 18);
        }

        this.bindTexture(this.guiTextureWidgets);

        this.drawSlotBackgrounds(97,  29, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(97,  77, 0, 238, this.container.getSequenceLength(1), this.container.getSequenceLength(1) * 2); // Sequence ON
        this.drawSlotBackgrounds(97, 125, 0, 238, this.container.getSequenceLength(2), this.container.getSequenceLength(2) * 2); // Sequence OFF
    }
}
