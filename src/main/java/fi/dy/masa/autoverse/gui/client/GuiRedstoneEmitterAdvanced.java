package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.ContainerRedstoneEmitterAdvanced;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitterAdvanced;

public class GuiRedstoneEmitterAdvanced extends GuiAutoverse
{
    private final ContainerRedstoneEmitterAdvanced containerRE;
    private final TileEntityRedstoneEmitterAdvanced te;

    public GuiRedstoneEmitterAdvanced(ContainerRedstoneEmitterAdvanced container, TileEntityRedstoneEmitterAdvanced te)
    {
        super(container, 256, 256, "gui.container.redstone_emitter_advanced");

        this.containerRE = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.redstone_emitter_advanced";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        s = I18n.format("autoverse.gui.label.reset_sequence");
        this.fontRenderer.drawString(s, 168 - this.fontRenderer.getStringWidth(s), 19, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.end_marker"),                      46,  24, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter_advanced.down"),    8,   53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter_advanced.up"),     89,   53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter_advanced.north"), 170,   53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter_advanced.south"),   8,  100, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter_advanced.west"),   89,  100, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.redstone_emitter_advanced.east"),  170,  100, 0x404040);

        s = I18n.format("autoverse.gui.label.out");
        this.fontRenderer.drawString(s, 184 - this.fontRenderer.getStringWidth(s), 151, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 44, 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.drawSlotBackgrounds(169,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSequences();
    }

    private void drawSequences()
    {
        int x = 7;
        int y = 62;

        for (int side = 0; side < 6; side++)
        {
            int index = side * 2;
            int len = this.container.getSequenceLength(index + 1);
            this.drawSlotBackgrounds(x, y     , 0, 238, len, len); // ON sequence
            this.drawSequenceMatchedMarker(index, x, y);

            index = side * 2 + 1;
            len = this.container.getSequenceLength(index + 1);
            this.drawSlotBackgrounds(x, y + 18, 0, 238, len, len); // OFF sequence
            this.drawSequenceMatchedMarker(index, x, y + 18);

            // Draw a rectangle around this side's group of slots, if the side is currently powered
            if (this.te.isSidePowered(side))
            {
                this.drawTexturedModalRect(this.guiLeft + x - 1, this.guiTop + y - 1, 0, 182, 74, 38);
            }

            if (side == 2)
            {
                x = 7;
                y = 109;
            }
            else
            {
                x += 81;
            }
        }
    }

    private void drawSequenceMatchedMarker(int id, int x, int y)
    {
        // Draw the hilighted slot backgrounds according to how many slots the detector has matched thus far
        final int matched = this.containerRE.getMatchedLength(id);

        for (int slot = 0; slot < matched; ++slot, x += 18)
        {
            this.drawTexturedModalRect(this.guiLeft + x, this.guiTop + y, 238, 36, 18, 18);
        }
    }
}
