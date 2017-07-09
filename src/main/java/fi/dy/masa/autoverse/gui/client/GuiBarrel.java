package fi.dy.masa.autoverse.gui.client;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageGuiAction;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.tileentity.TileEntityBarrel;

public class GuiBarrel extends GuiAutoverse
{
    protected final TileEntityBarrel te;

    public GuiBarrel(ContainerAutoverse container, TileEntityBarrel te)
    {
        super(container, 176, 168, "gui.container.barrel");

        this.te = te;
        this.infoArea = new InfoArea(158, 21, 11, 11, "autoverse.gui.infoarea.barrel");
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
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.tier_num", this.te.getTier() + 1),            21, 53, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.max_stack_num", this.te.getMaxStackSize()),   21, 64, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 75, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        if (this.te.isCreative())
        {
            this.bindTexture(this.guiTextureWidgets);
            this.drawTexturedModalRect(this.guiLeft + 159, this.guiTop + 7, 210, 0, 10, 10);
        }
    }

    protected void createButtons()
    {
        this.buttonList.clear();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        this.buttonList.add(new GuiButtonHoverText(0, x + 8, y + 54, 8, 8, 0, 0,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.barrel.change_tier"));

        this.buttonList.add(new GuiButtonHoverText(1, x + 160, y + 8, 8, 8, 0, 8,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.toggle_creative"));
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

        if (button.id == 0 || button.id == 1)
        {
            if (GuiScreen.isShiftKeyDown()) { amount *= 2; }
            if (GuiScreen.isCtrlKeyDown())  { amount *= 4; }

            PacketHandler.INSTANCE.sendToServer(new MessageGuiAction(dim, this.te.getPos(),
                ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC, button.id, amount));
        }
    }
}
