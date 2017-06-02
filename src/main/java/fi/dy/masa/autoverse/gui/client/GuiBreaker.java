package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityBreaker;

public class GuiBreaker extends GuiAutoverse
{
    protected final TileEntityBreaker te;

    public GuiBreaker(ContainerAutoverse container, TileEntityBreaker te)
    {
        super(container, 176, 166, "dummy");

        this.te = te;
        // Use the vanilla Dropper/Dispenser GUI texture
        this.guiTexture = new ResourceLocation("textures/gui/container/dispenser.png");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(this.te.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 71, 0x404040);
    }
}
