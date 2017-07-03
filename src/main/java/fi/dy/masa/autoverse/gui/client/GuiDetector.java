package fi.dy.masa.autoverse.gui.client;

import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.inventory.container.ContainerDetector;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockDetector;

public class GuiDetector extends GuiAutoverse
{
    private final ContainerDetector containerD;
    private final TileEntityBlockDetector te;

    public GuiDetector(ContainerDetector container, TileEntityBlockDetector te)
    {
        super(container, 176, 256, "gui.container.detector");
        this.containerD = container;
        this.te = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.detector";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        s = I18n.format("autoverse.gui.label.placer_programmable.reset_sequence");
        this.fontRenderer.drawString(s, 96 - this.fontRenderer.getStringWidth(s), 16, 0x404040);

        s = I18n.format("autoverse.gui.label.end");
        this.fontRenderer.drawString(s, 45, 24, 0x404040);

        s = I18n.format("autoverse.gui.label.1_bit_marker");
        this.fontRenderer.drawString(s, 45, 42, 0x404040);

        s = I18n.format("autoverse.gui.label.detector.distance_and_angle");
        this.fontRenderer.drawString(s,  8, 54, 0x404040);

        s = I18n.format("autoverse.gui.label.detector.delay");
        this.fontRenderer.drawString(s, 98, 54, 0x404040);

        s = I18n.format("autoverse.gui.label.detector.detection_items");
        this.fontRenderer.drawString(s,  8, 104, 0x404040);

        s = I18n.format("autoverse.gui.label.output_buffer");
        this.fontRenderer.drawString(s, 28, 152, 0x404040);

        s = I18n.format("autoverse.gui.label.detector.out_detection");
        this.fontRenderer.drawString(s, this.xSize - 28 - this.fontRenderer.getStringWidth(s), 162, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        final int invSize = this.containerD.getDetectionInvSize();

        this.bindTexture(this.guiTextureWidgets);

        this.drawSlotBackgrounds(7, 113, 0, 238, 9, invSize);

        ItemStackHandlerLockable inv = this.containerD.getDetectionInventory();
        final int first = this.containerD.getDetectionInventorySlotRange().first;
        List<Slot> slotList = this.containerD.getSpecialSlots();

        this.drawLockedSlotBackgrounds(inv, first, slotList);
        this.drawTemplateStacks(inv, first, slotList);
    }
}
