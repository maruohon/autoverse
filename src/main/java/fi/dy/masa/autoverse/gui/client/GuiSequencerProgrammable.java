package fi.dy.masa.autoverse.gui.client;

import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.inventory.container.ContainerSequencerProgrammable;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencerProgrammable;

public class GuiSequencerProgrammable extends GuiAutoverse
{
    private final ContainerSequencerProgrammable containerSP;
    private final TileEntitySequencerProgrammable te;

    public GuiSequencerProgrammable(ContainerSequencerProgrammable container, TileEntitySequencerProgrammable te)
    {
        // Same GUI background as the filter
        super(container, 176, 256, "gui.container.filter");
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

        s = I18n.format("autoverse.gui.label.rst");
        this.fontRenderer.drawString(s, 96 - this.fontRenderer.getStringWidth(s), 17, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.end"),    45, 25, 0x404040);
        this.fontRenderer.drawString("^ " + I18n.format("autoverse.gui.label.sequencer_programmable.sequence"), 8, 150, 0x404040);

        s = I18n.format("autoverse.gui.label.out");
        this.fontRenderer.drawString(s, this.xSize - 28 - this.fontRenderer.getStringWidth(s), 151, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = this.guiLeft + 7;
        int y = this.guiTop + 55;
        final ItemStackHandlerLockable inv = this.containerSP.getSequenceInventory();
        final int invSize = inv.getSlots();

        this.bindTexture(this.guiTextureWidgets);

        this.drawSlotBackgrounds( 97,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(  7,  55, 0, 202, 9, invSize);
        this.drawSlotBackgrounds(151, 150, 0, 202, 1, 1); // Out

        final int first = this.containerSP.getSequenceInventorySlotRange().first;
        final List<Slot> slotList = this.containerSP.getSpecialSlots();

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
