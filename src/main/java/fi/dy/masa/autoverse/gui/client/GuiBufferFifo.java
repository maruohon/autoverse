package fi.dy.masa.autoverse.gui.client;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.container.ContainerBufferFifo;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageGuiAction;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
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
    public void initGui()
    {
        super.initGui();
        this.createButtons();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 4, 0x404040);

        this.fontRenderer.drawString(String.valueOf(this.te.getFifoLength()), 225, 177, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        this.bindTexture(this.guiTextureWidgets);

        this.drawSlotBackgrounds(11, 12, 0, 184, 13, this.te.getFifoLength());

        // Draw a purple background for the current extract slot
        int slot = Configs.fifoBufferOffsetSlots ? 0 : this.containerFifo.getExtractPosition();

        int exRow = slot / 13;
        int exCol = slot % 13;
        this.drawTexturedModalRect(x + 11 + exCol * 18, y + 12 + exRow * 18, 238, 0, 18, 18);

        slot = this.containerFifo.getInsertPosition();

        if (Configs.fifoBufferOffsetSlots)
        {
            slot = this.containerFifo.getOffsetSlotNumberNegative(slot);
        }

        int inRow = slot / 13;
        int inCol = slot % 13;
        // Draw a green background for the current insert slot
        this.drawTexturedModalRect(x + 11 + inCol * 18, y + 12 + inRow * 18, 238, 18, 18, 18);
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type)
    {
        // Offset/fake the slot number for the duration of the slot click, so that the
        // wrapped slot number gets sent to the server
        if (Configs.fifoBufferOffsetSlots && slot != null &&
            this.containerFifo.getCustomInventorySlotRange().contains(slot.slotNumber))
        {
            int slotNumber = this.containerFifo.getOffsetSlotNumberPositive(slot.slotNumber);
            this.mc.playerController.windowClick(this.inventorySlots.windowId, slotNumber, mouseButton, type, this.mc.player);
        }
        else
        {
            super.handleMouseClick(slot, slotId, mouseButton, type);
        }
    }

    protected void createButtons()
    {
        this.buttonList.clear();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        this.buttonList.add(new GuiButtonHoverText(0, x + 214, y + 177, 8, 8, 0, 0,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.inventory_size"));
    }

    @Override
    protected void actionPerformedWithButton(GuiButton button, int mouseButton) throws IOException
    {
        int dim = this.te.getWorld().provider.getDimension();
        int amount = 0;

        if (mouseButton == 0 || mouseButton == 11)
        {
            amount = 1;
        }
        else if (mouseButton == 1 || mouseButton == 9)
        {
            amount = -1;
        }

        if (button.id == 0)
        {
            if (GuiScreen.isShiftKeyDown()) { amount *= 13; }
            if (GuiScreen.isCtrlKeyDown())  { amount *= 5; }

            PacketHandler.INSTANCE.sendToServer(new MessageGuiAction(dim, this.te.getPos(),
                ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC, button.id, amount));
        }
    }
}
