package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.inventory.container.ContainerSequencer;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;

public class GuiSequencer extends GuiAutoverse
{
    private final ContainerSequencer containrSeq;
    private final TileEntitySequencer teseq;
    private final int invSize;

    public GuiSequencer(ContainerSequencer container, TileEntitySequencer te)
    {
        super(container, 176, 156, "gui.container.sequencer");
        this.containrSeq = container;
        this.teseq = te;
        this.invSize = te.getBaseItemHandler().getSlots();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.teseq.hasCustomName() ? this.teseq.getName() : I18n.format(this.teseq.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 6, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 60, 0x404040);
    }

    protected int getTier()
    {
        return this.teseq.getTier();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.drawSlotBackgrounds(7, 21, 0, 202, 9, this.invSize);
        this.hilightOutputSlot();
    }

    protected void hilightOutputSlot()
    {
        int slotNum = this.containrSeq.getExtractSlot();
        Slot slot = slotNum >= 0 && slotNum < this.containrSeq.inventorySlots.size() ? this.containrSeq.getSlot(slotNum) : null;

        if (slot != null)
        {
            this.bindTexture(this.guiTextureWidgets);
            this.drawTexturedModalRect(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 238, 0, 18, 18);
        }
    }
}
