package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverseTile;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.container.ContainerPipe;
import fi.dy.masa.autoverse.tileentity.TileEntityPipe;

public class GuiPipe extends GuiAutoverseTile
{
    private final TileEntityPipe te;

    public GuiPipe(ContainerPipe container, TileEntityPipe te)
    {
        super(container, 176, 192, "gui.container.pipe", te);

        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format("autoverse.container.pipe");
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2,  4, 0x404040);

        int val = this.te.getDelay();
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.delay.num", val),     48, 24, 0x404040);

        val = this.te.getBaseItemHandler().getSlotLimit(0);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.max_stack_num", val), 48, 36, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 100, 0x404040);
    }

    @Override
    protected void createButtons()
    {
        this.addButton(new GuiButtonHoverText(0, this.guiLeft + 35, this.guiTop + 25, 8, 8, 0, 0,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.delay"));

        this.addButton(new GuiButtonHoverText(1, this.guiLeft + 35, this.guiTop + 37, 8, 8, 0, 0,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.max_stack_size"));

        this.setButtonMultipliers(8, 4);
    }
}
