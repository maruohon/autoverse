package fi.dy.masa.autoverse.gui.client.base;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageGuiAction;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class GuiAutoverseTile extends GuiAutoverse
{
    protected final TileEntityAutoverse te;
    protected int buttonMultiplierShift = 2;
    protected int buttonMultiplierCtrl = 4;

    public GuiAutoverseTile(ContainerAutoverse container, int xSize, int ySize, String textureName, TileEntityAutoverse te)
    {
        super(container, xSize, ySize, textureName);

        this.te = te;
    }

    protected GuiAutoverseTile setButtonMultipliers(int shift, int ctrl)
    {
        this.buttonMultiplierShift = shift;
        this.buttonMultiplierCtrl = ctrl;
        return this;
    }

    @Override
    protected void actionPerformedWithButton(GuiButton button, int mouseButton) throws IOException
    {
        if (button.id >= 0 && button.id < this.buttonList.size())
        {
            int dim = this.te.getWorld().provider.getDimension();
            int amount = 0;

            // For the scrolling stuff, see GuiAutoverse#handleMouseInput()
            // Left click or scroll down
            if (mouseButton == 0 || mouseButton == 11)
            {
                amount = 1;
            }
            // Right click or scroll up
            else if (mouseButton == 1 || mouseButton == 9)
            {
                amount = -1;
            }

            if (GuiScreen.isShiftKeyDown()) { amount *= this.buttonMultiplierShift; }
            if (GuiScreen.isCtrlKeyDown())  { amount *= this.buttonMultiplierCtrl; }

            PacketHandler.INSTANCE.sendToServer(new MessageGuiAction(dim, this.te.getPos(),
                ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC, button.id, amount));
        }
    }
}
