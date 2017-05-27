package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.util.math.MathHelper;
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

        this.renderSlotBackgrounds();
        this.hilightOutputSlot();
    }

    protected void renderSlotBackgrounds()
    {
        final int maxPerRow = 9;
        // Draw the slot backgrounds according to how many slots this tier has
        int rows = Math.max((int) (Math.ceil((double) this.invSize / maxPerRow)), 1);

        for (int row = 0; row < rows; row++)
        {
            int rowLen = MathHelper.clamp(this.invSize - (row * maxPerRow), 1, maxPerRow);

            // Render slots from the player inventory's first row into the Sequencer
            this.drawTexturedModalRect(this.guiLeft + 7, this.guiTop + 21 + row * 18, 7, 73, rowLen * 18, 18);
        }
    }

    protected void hilightOutputSlot()
    {
        int slotNum = this.containrSeq.getExtractSlot();
        Slot slot = slotNum >= 0 && slotNum < this.containrSeq.inventorySlots.size() ? this.containrSeq.getSlot(slotNum) : null;

        if (slot != null)
        {
            this.bindTexture(this.guiTextureWidgets);
            this.drawTexturedModalRect(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 102, 108, 18, 18);
        }
    }
}
