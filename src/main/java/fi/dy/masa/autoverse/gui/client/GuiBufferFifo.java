package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.inventory.container.ContainerBufferFifo;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;

public class GuiBufferFifo extends GuiAutoverse
{
    private final ContainerBufferFifo containerFifo;
    private final TileEntityBufferFifo te;

    public GuiBufferFifo(ContainerBufferFifo container, TileEntityBufferFifo te)
    {
        super(container, 256, 256, "gui.container.buffer_fifo");
        this.containerFifo = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 4, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.bindTexture(this.guiTextureWidgets);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // Draw a green background for the current extract slot
        int slot = Configs.fifoBufferUseWrappedInventory ? 0 : this.containerFifo.extractPos;

        int exRow = slot / 13;
        int exCol = slot % 13;
        this.drawTexturedModalRect(x + 11 + exCol * 18, y + 12 + exRow * 18, 102, 54, 18, 18);

        // Draw a purple background for the current insert slot
        slot = Configs.fifoBufferUseWrappedInventory ? this.getOffsetSlot(this.containerFifo.insertPos) : this.containerFifo.insertPos;

        int inRow = slot / 13;
        int inCol = slot % 13;
        this.drawTexturedModalRect(x + 11 + inCol * 18, y + 12 + inRow * 18, 102, 36, 18, 18);
    }

    private int getOffsetSlot(int slot)
    {
        slot -= this.containerFifo.extractPos;

        if (slot < 0)
        {
            slot += this.te.getBaseItemHandler().getSlots();
        }

        return slot;
    }
}
