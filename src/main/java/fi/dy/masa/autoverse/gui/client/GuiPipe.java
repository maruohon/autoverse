package fi.dy.masa.autoverse.gui.client;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.container.ContainerPipe;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageGuiAction;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.tileentity.TileEntityPipe;

public class GuiPipe extends GuiAutoverse
{
    private final TileEntityPipe te;

    public GuiPipe(ContainerPipe container, TileEntityPipe te)
    {
        super(container, 176, 192, "gui.container.pipe");

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
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2,  4, 0x404040);

        int val = this.te.getBaseItemHandler().getSlotLimit(0);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.max_stack_num", val), 48, 24, 0x404040);

        val = this.te.getDelay();
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.delay.num", val),     48, 36, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 100, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);
    }

    protected void createButtons()
    {
        this.buttonList.clear();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        this.buttonList.add(new GuiButtonHoverText(0, x + 35, y + 25, 8, 8, 0, 0,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.delay"));

        this.buttonList.add(new GuiButtonHoverText(1, x + 35, y + 37, 8, 8, 0, 0,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.max_stack_size"));
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

        if (GuiScreen.isShiftKeyDown()) { amount *= 5; }
        if (GuiScreen.isCtrlKeyDown())  { amount *= 10; }

        PacketHandler.INSTANCE.sendToServer(new MessageGuiAction(dim, this.te.getPos(),
            ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC, button.id, amount));
    }
}
