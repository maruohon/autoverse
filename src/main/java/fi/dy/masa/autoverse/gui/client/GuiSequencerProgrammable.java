package fi.dy.masa.autoverse.gui.client;

import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.util.math.MathHelper;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.inventory.container.ContainerSequencerProgrammable;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencerProgrammable;

public class GuiSequencerProgrammable extends GuiAutoverse
{
    private final ContainerSequencerProgrammable containerSP;
    private final TileEntitySequencerProgrammable te;

    public GuiSequencerProgrammable(ContainerSequencerProgrammable container, TileEntitySequencerProgrammable te)
    {
        // Same GUI background as the detector
        super(container, 176, 256, "gui.container.sequence_detector");
        this.containerSP = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.sequencer_programmable";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.input"), 26, 24, 0x404040);

        s = I18n.format("autoverse.gui.label.reset_sequence");
        this.fontRenderer.drawString(s, 96 - this.fontRenderer.getStringWidth(s), 16, 0x404040);

        s = I18n.format("autoverse.gui.label.end_marker");
        this.fontRenderer.drawString(s, 26, 42, 0x404040);

        s = I18n.format("autoverse.gui.label.sequencer_programmable.sequence");
        this.fontRenderer.drawString(s, 8, 150, 0x404040);

        s = I18n.format("autoverse.gui.label.output_buffer");
        this.fontRenderer.drawString(s, this.xSize - 28 - this.fontRenderer.getStringWidth(s), 163, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = this.guiLeft + 7;
        int y = this.guiTop + 55;

        final int invSize = this.containerSP.getSequenceLength();
        final int maxRowLength = 9;
        int rows = (int) (Math.ceil((double) invSize / maxRowLength));

        if (invSize > 0)
        {
            rows = Math.max(rows, 1);
        }

        this.bindTexture(this.guiTextureWidgets);

        // Draw the slot backgrounds according to how many slots the sequencer has at the moment
        for (int row = 0; row < rows; row++)
        {
            int rowLen = MathHelper.clamp(invSize - (row * maxRowLength), 1, maxRowLength);
            this.drawTexturedModalRect(x, y + row * 18, 0, 238, rowLen * 18, 18);
        }

        ItemStackHandlerLockable inv = this.containerSP.getSequenceInventory();
        final int first = this.containerSP.getSequenceInventorySlotRange().first;
        List<Slot> slotList = this.containerSP.getSpecialSlots();

        this.drawLockedSlotBackgrounds(inv, first, slotList);

        if (invSize > 0)
        {
            // Draw the hilighted slot background for the current output slot
            final int outSlot = this.containerSP.getOutputSlot();

            this.drawTexturedModalRect(x + (outSlot % 9) * 18, y + (outSlot / 9) * 18, 238, 0, 18, 18);
        }

        this.drawTemplateStacks(inv, first, slotList);
    }
}
