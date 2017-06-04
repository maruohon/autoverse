package fi.dy.masa.autoverse.gui.client;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.container.base.SlotRange;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageGuiAction;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockReaderNBT;

public class GuiBlockReaderNBT extends GuiAutoverse
{
    private final TileEntityBlockReaderNBT ter;

    public GuiBlockReaderNBT(ContainerAutoverse container, TileEntityBlockReaderNBT te)
    {
        super(container, 176, 213, "gui.container.block_reader_nbt");

        this.ter = te;
        //this.infoArea = new InfoArea(160, 5, 11, 11, "autoverse.gui.infoarea.block_reader_nbt");
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
        String str = I18n.format("autoverse.container.block_reader_nbt");
        this.fontRenderer.drawString(str, this.xSize / 2 - this.fontRenderer.getStringWidth(str) / 2, 5, 0x404040);

        str = I18n.format("autoverse.gui.label.block_reader_nbt.length_num", this.ter.getMaxLength());
        this.fontRenderer.drawString(str, 41, 30, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 119, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // Draw the slot backgrounds for existing/enabled slots
        SlotRange range = this.container.getCustomInventorySlotRange();

        for (int i = range.first; i < range.lastExc; i++)
        {
            Slot slot = this.container.getSlot(i);

            if (slot != null)
            {
                this.drawTexturedModalRect(x + slot.xPos - 1, y + slot.yPos - 1, 7, 130, 18, 18);
            }
        }
    }

    protected void createButtons()
    {
        this.buttonList.clear();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        this.buttonList.add(new GuiButtonHoverText(0, x +  8, y + 25, 14, 14, 60, 42,
                this.guiTextureWidgets, 14, 0, "autoverse.gui.label.block_reader.take_blocks"));

        this.buttonList.add(new GuiButtonHoverText(1, x + 29, y + 31, 8, 8, 0, 120,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.block_reader.block_count"));
    }

    @Override
    protected void actionPerformedWithButton(GuiButton button, int mouseButton) throws IOException
    {
        int dim = this.ter.getWorld().provider.getDimension();
        int amount = 0;

        if (mouseButton == 0 || mouseButton == 11)
        {
            amount = 1;
        }
        else if (mouseButton == 1 || mouseButton == 9)
        {
            amount = -1;
        }

        if (button.id >= 0 && button.id <= 1)
        {
            if (GuiScreen.isShiftKeyDown()) { amount *= 8; }
            if (GuiScreen.isCtrlKeyDown())  { amount *= 4; }

            PacketHandler.INSTANCE.sendToServer(new MessageGuiAction(dim, this.ter.getPos(),
                ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC, button.id, amount));
        }
    }
}
