package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityPlacerProgrammable;

public class GuiPlacerProgrammable extends GuiAutoverse
{
    private final TileEntityPlacerProgrammable te;

    public GuiPlacerProgrammable(ContainerAutoverse container, TileEntityPlacerProgrammable te)
    {
        super(container, 176, 256, "gui.container.placer_programmable");
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.placer_programmable";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        s = I18n.format("autoverse.gui.label.placer_programmable.reset_sequence");
        this.fontRenderer.drawString(s, 96 - this.fontRenderer.getStringWidth(s), 16, 0x404040);

        s = I18n.format("autoverse.gui.label.placer_programmable.end_marker");
        this.fontRenderer.drawString(s, 45, 24, 0x404040);

        s = I18n.format("autoverse.gui.label.placer_programmable.high_bit_marker");
        this.fontRenderer.drawString(s, 45, 42, 0x404040);

        s = I18n.format("autoverse.gui.label.placer_programmable.trigger");
        this.fontRenderer.drawString(s,  8, 54, 0x404040);

        s = I18n.format("autoverse.gui.label.placer_programmable.property", 1);
        this.fontRenderer.drawString(s, 98, 54, 0x404040);

        s = I18n.format("autoverse.gui.label.placer_programmable.property", 2);
        this.fontRenderer.drawString(s,  8, 104, 0x404040);

        s = I18n.format("autoverse.gui.label.placer_programmable.property", 3);
        this.fontRenderer.drawString(s, 98, 104, 0x404040);

        s = I18n.format("autoverse.gui.label.output_buffer");
        this.fontRenderer.drawString(s, this.xSize - 28 - this.fontRenderer.getStringWidth(s), 152, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        /*
        int x = this.guiLeft + 7;
        int y = this.guiTop + 55;

        final int invSize = this.containerPP.getSequenceLength();
        final int maxRowLength = 9;
        int rows = (int) (Math.ceil((double) invSize / maxRowLength));

        if (invSize > 0)
        {
            rows = Math.max(rows, 1);
        }

        // Draw the slot backgrounds according to how many slots the sequencer has at the moment
        for (int row = 0; row < rows; row++)
        {
            int rowLen = MathHelper.clamp(invSize - (row * maxRowLength), 1, maxRowLength);
            this.drawTexturedModalRect(x, y + row * 18, 16, 238, rowLen * 18, 18);
        }

        this.bindTexture(this.guiTextureWidgets);

        ItemStackHandlerLockable inv = this.containerPP.getSequenceInventory();
        final int first = this.containerPP.getSequenceInventorySlotRange().first;
        List<Slot> slotList = this.containerPP.getSpecialSlots();

        this.drawLockedSlotBackgrounds(inv, first, slotList);

        if (invSize > 0)
        {
            // Draw the hilighted slot background for the current output slot
            final int outSlot = this.containerPP.getOutputSlot();

            this.drawTexturedModalRect(x + (outSlot % 9) * 18, y + (outSlot / 9) * 18, 102, 162, 18, 18);
        }

        this.drawTemplateStacks(inv, first, slotList);
        */
    }

    private void drawPropertySlots(int propId, int x, int y)
    {
        
    }
}
