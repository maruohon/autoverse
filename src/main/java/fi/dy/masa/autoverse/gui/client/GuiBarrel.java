package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.inventory.container.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityBarrel;

public class GuiBarrel extends GuiContainerLargeStacks
{
    protected final TileEntityBarrel te;
    protected final int max;

    public GuiBarrel(ContainerAutoverse container, TileEntityBarrel te)
    {
        super(container, 176, 128, "gui.container.barrel");

        this.te = te;
        this.max = te.getMaxStackSize();
        this.scaledStackSizeTextInventories.add(container.getCustomInventory());
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRendererObj.drawString(s, this.xSize / 2 - this.fontRendererObj.getStringWidth(s) / 2, 4, 0x404040);
        s = I18n.format("autoverse.gui.label.max") + this.max;
        this.fontRendererObj.drawString(s, 100, 25, 0x404040);
        this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, 35, 0x404040);
    }
}
