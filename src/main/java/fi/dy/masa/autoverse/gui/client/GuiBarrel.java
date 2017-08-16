package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverseTile;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityBarrel;

public class GuiBarrel extends GuiAutoverseTile
{
    protected final TileEntityBarrel te;

    public GuiBarrel(ContainerAutoverse container, TileEntityBarrel te)
    {
        super(container, 176, 168, "gui.container.barrel", te);

        this.te = te;
        this.infoArea = new InfoArea(158, 21, 11, 11, "autoverse.gui.infoarea.barrel");
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

    @Override
    protected void createButtons()
    {
        this.addButton(new GuiButtonHoverText(0, this.guiLeft +   8, this.guiTop + 54, 8, 8, 0, 0,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.barrel.change_tier"));

        this.addButton(new GuiButtonHoverText(1, this.guiLeft + 160, this.guiTop +  8, 8, 8, 0, 8,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.toggle_creative"));
    }
}
