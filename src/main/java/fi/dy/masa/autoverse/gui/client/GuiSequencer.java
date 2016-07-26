package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.inventory.container.ContainerSequencer;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;

public class GuiSequencer extends GuiAutoverse
{
    private final TileEntitySequencer teseq;

    public GuiSequencer(ContainerSequencer container, TileEntitySequencer te)
    {
        super(container, 176, 155, "gui.container.sequencer");
        this.teseq = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.teseq.hasCustomName() ? this.teseq.getName() : I18n.format(this.teseq.getName());
        this.fontRendererObj.drawString(s, this.xSize / 2 - this.fontRendererObj.getStringWidth(s) / 2, 6, 0x404040);
        this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, 60, 0x404040);
    }

    protected int getTier()
    {
        return this.teseq.getTier();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        this.coverSlots(x, y);
    }

    protected void coverSlots(int x, int y)
    {
        int numSlots = this.teseq.getBaseItemHandler().getSlots();

        // First row of slots
        if (numSlots < 9)
        {
            this.drawTexturedModalRect(x + 7 + numSlots * 18, y + 21, 7, 3, (9 - numSlots) * 18, 18);
        }
        // For the 16-slot variant the slots are in two rows of 8, so cover the last slot in that case
        else if (numSlots > 9)
        {
            this.drawTexturedModalRect(x + 7 + 8 * 18, y + 21, 7, 3, 18, 18);
        }

        // Second row of slots
        if (numSlots < 16)
        {
            this.drawTexturedModalRect(x + 7, y + 39, 7, 3, 8 * 18, 18);
        }
    }
}
