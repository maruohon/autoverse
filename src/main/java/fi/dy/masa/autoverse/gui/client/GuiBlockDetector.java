package fi.dy.masa.autoverse.gui.client;

import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.inventory.container.ContainerBlockDetector;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockDetector;

public class GuiBlockDetector extends GuiAutoverse
{
    private final ContainerBlockDetector containerD;
    private final TileEntityBlockDetector te;

    public GuiBlockDetector(ContainerBlockDetector container, TileEntityBlockDetector te)
    {
        // Same GUI background as the filter
        super(container, 176, 256, "gui.container.filter");

        this.containerD = container;
        this.te = te;
        this.infoArea = new InfoArea(7, 36, 11, 11, "autoverse.gui.infoarea.block_detector");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.block_detector";
        String s = this.te.hasCustomName() ? this.te.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        s = I18n.format("autoverse.gui.label.rst");
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

        s = I18n.format("autoverse.gui.label.out_normal");
        this.fontRenderer.drawString(s, 28, 150, 0x404040);

        s = I18n.format("autoverse.gui.label.detector.out_detection");
        this.fontRenderer.drawString(s, this.xSize - 28 - this.fontRenderer.getStringWidth(s), 160, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.bindTexture(this.guiTextureWidgets);

        final ItemStackHandlerLockable inv = this.containerD.getDetectionInventory();
        final int invSize = inv.getSlots() > 0 ? inv.getSlots() : 18;

        this.drawSlotBackgrounds( 25,  33, 0, 238, 1, 1); // 1-bit marker

        this.drawSlotBackgrounds( 97,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(  7,  64, 0, 238, 4, 4); // Config: Distance
        this.drawSlotBackgrounds(  7,  82, 0, 238, 4, 4); // Config: Angle
        this.drawSlotBackgrounds( 97,  64, 0, 238, 4, 8); // Config: Delay
        this.drawSlotBackgrounds(  7, 113, 0, 238, 9, invSize); // Detector

        this.drawSlotBackgrounds(  7, 150, 0, 220, 1, 1); // Out Normal
        this.drawSlotBackgrounds(151, 150, 0, 220, 1, 1); // Out Detector

        final int first = this.containerD.getDetectionInventorySlotRange().first;
        List<Slot> slotList = this.containerD.getSpecialSlots();

        this.drawLockedSlotBackgrounds(inv, first, slotList);
        this.drawTemplateStacks(inv, first, slotList);
    }
}
