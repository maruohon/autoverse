package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitter;

public class GuiRedstoneEmitter extends GuiAutoverse
{
    private final TileEntityRedstoneEmitter te;

    public GuiRedstoneEmitter(ContainerAutoverse container, TileEntityRedstoneEmitter te)
    {
        super(container, 176, 238, "gui.container.redstone_emitter");
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.redstone_emitter";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"), 8, 19, 0x404040);

        s = I18n.format("autoverse.gui.label.reset_sequence");
        this.fontRenderer.drawString(s, 61, 18, 0x404040);

        s = I18n.format("autoverse.gui.label.redstone_emitter.side_config");
        this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 6, 18, 0x404040);

        s = I18n.format("autoverse.gui.label.redstone_emitter.enabled_marker");
        this.fontRenderer.drawString(s, 29, 74, 0x404040);

        s = I18n.format("autoverse.gui.label.redstone_emitter.sequence_on");
        this.fontRenderer.drawString(s, 8, 90, 0x404040);

        s = I18n.format("autoverse.gui.label.redstone_emitter.sequence_off");
        this.fontRenderer.drawString(s, 80, 90, 0x404040);

        s = I18n.format("autoverse.gui.label.output_buffer");
        this.fontRenderer.drawString(s, this.xSize - this.fontRenderer.getStringWidth(s) - 6, 138, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 145, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        final int mask = this.te.getSideMask();

        int x = this.guiLeft + 133;
        int y = this.guiTop + 29;

        for (int i = 0; i < 6; ++i)
        {
            // Side enabled vs. disabled
            int vOffset = (mask & (1 << i)) != 0 ? 0 : 54;
            int xOffset = (i & 1) * 18;
            int yOffset = (i / 2) * 18;

            this.drawTexturedModalRect(x + xOffset, y + yOffset, 176 + xOffset, vOffset + yOffset, 18, 18);
        }
    }
}
