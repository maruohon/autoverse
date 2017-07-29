package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.block.BlockMuxer;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.ContainerMuxerSimple;
import fi.dy.masa.autoverse.tileentity.TileEntityMuxer;

public class GuiMuxerSimple extends GuiAutoverse
{
    private final ContainerMuxerSimple containerM;
    private final TileEntityMuxer te;

    public GuiMuxerSimple(ContainerMuxerSimple container, TileEntityMuxer te)
    {
        // Same background texture as the redstone splitter
        super(container, 176, 146, "gui.container.splitter_redstone");
        this.containerM = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s;

        if (this.te.hasCustomName())
        {
            s = this.te.getName();
        }
        else
        {
            s = this.te.getMuxerType() == BlockMuxer.MuxerType.PRIORITY ?
                    I18n.format("autoverse.container.muxer_priority") :
                    I18n.format("autoverse.container.muxer_redstone");
        }

        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input.num", 1),     8, 19, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input.num", 2),    62, 20, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.out"),            116, 20, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 52, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.bindTexture(this.guiTextureWidgets);

        // Draw the colored background for the active output
        Slot slot = this.containerM.getSlot(this.containerM.secondaryInput ? 1 : 0);
        this.drawTexturedModalRect(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 238, 36, 18, 18);
    }
}
