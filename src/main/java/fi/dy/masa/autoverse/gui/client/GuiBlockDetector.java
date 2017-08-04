package fi.dy.masa.autoverse.gui.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerLockable;
import fi.dy.masa.autoverse.inventory.container.ContainerBlockDetector;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageGuiAction;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockDetector;

public class GuiBlockDetector extends GuiAutoverse
{
    private final ContainerBlockDetector containerD;
    private final TileEntityBlockDetector te;

    public GuiBlockDetector(ContainerBlockDetector container, TileEntityBlockDetector te)
    {
        super(container, 176, 256, "gui.container.block_detector");

        this.containerD = container;
        this.te = te;
        this.infoArea = new InfoArea(7, 36, 11, 11, "autoverse.gui.infoarea.block_detector");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.createButtons();
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

        s = I18n.format("autoverse.gui.label.block_detector.distance_and_angle");
        this.fontRenderer.drawString(s,  8, 54, 0x404040);

        s = I18n.format("autoverse.gui.label.block_detector.delay");
        this.fontRenderer.drawString(s, 98, 54, 0x404040);

        s = I18n.format("autoverse.gui.label.block_detector.detection_items");
        this.fontRenderer.drawString(s,  8, 104, 0x404040);

        s = I18n.format("autoverse.gui.label.block_detector.other_items");
        this.fontRenderer.drawString(s, 28, 151, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.bindTexture(this.guiTextureWidgets);

        final ItemStackHandlerLockable invDetection = this.containerD.getDetectionInventory();
        final ItemStackHandlerLockable invOthersBuffer = this.containerD.getOthersBufferInventory();
        final int invSizeDetection = invDetection.getSlots() > 0 ? invDetection.getSlots() : 18;
        final int invSizeOthersBuffer = invOthersBuffer.getSlots() > 0 ? invOthersBuffer.getSlots() : 1;

        this.drawSlotBackgrounds( 97,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(  7,  64, 0, 238, 4, 4); // Config: Distance
        this.drawSlotBackgrounds(  7,  82, 0, 238, 4, 4); // Config: Angle
        this.drawSlotBackgrounds( 97,  64, 0, 238, 4, 8); // Config: Delay
        this.drawSlotBackgrounds(  7, 113, 0, 238, 9, invSizeDetection); // Detector
        this.drawSlotBackgrounds(  7, 150, 0, 238, 1, invSizeOthersBuffer); // Others buffer

        this.drawSlotBackgrounds(115, 150, 0, 220, 1, 1); // Out Normal

        List<Slot> slotList = this.containerD.getSpecialSlots();

        int firstDetection = this.containerD.getDetectionInventorySlotRange().first;
        int firstOthers = this.containerD.getOthersBufferInventorySlotRange().first;

        // Detection items buffer
        this.drawLockedSlotBackgrounds(invDetection, firstDetection, slotList);

        // "Other blocks" item buffer
        this.drawLockedSlotBackgrounds(invOthersBuffer, firstOthers, slotList);

        this.drawTemplateStacks(invDetection, firstDetection, slotList);
        this.drawTemplateStacks(invOthersBuffer, firstOthers, slotList);

        this.bindTexture(this.guiTextureWidgets);
        RenderHelper.enableGUIStandardItemLighting();

        if (this.te.getUseIndicators())
        {
            // Draw the colored ring around the button
            this.drawTexturedModalRect(this.guiLeft + 159, this.guiTop + 4, 210, 0, 10, 10);
        }
        else
        {
            // Draw the black ring around the button
            this.drawTexturedModalRect(this.guiLeft + 159, this.guiTop + 4, 210, 20, 10, 10);
        }
    }

    @Override
    protected void drawTooltips(int mouseX, int mouseY)
    {
        Slot slot = this.getSlotUnderMouse();

        // Hovering over an empty normal output slot
        if (slot != null && slot.slotNumber == this.containerD.slotNormalOut && slot.getHasStack() == false)
        {
            List<String> list = new ArrayList<String>();
            list.add(I18n.format("autoverse.gui.label.out_normal"));
            this.drawHoveringText(list, mouseX, mouseY, this.fontRenderer);
        }
        else if (slot != null && slot.slotNumber == this.containerD.slotDetectionOut && slot.getHasStack() == false)
        {
            List<String> list = new ArrayList<String>();
            list.add(I18n.format("autoverse.gui.label.block_detector.out_detection"));
            this.drawHoveringText(list, mouseX, mouseY, this.fontRenderer);
        }
        else
        {
            super.drawTooltips(mouseX, mouseY);
        }
    }

    protected void createButtons()
    {
        this.buttonList.clear();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        this.buttonList.add(new GuiButtonHoverText(0, x + 160, y + 5, 8, 8, 0, 16,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.block_detector.use_indicators"));
    }

    @Override
    protected void actionPerformedWithButton(GuiButton button, int mouseButton) throws IOException
    {
        if (button.id == 0)
        {
            int dim = this.te.getWorld().provider.getDimension();

            PacketHandler.INSTANCE.sendToServer(new MessageGuiAction(dim, this.te.getPos(),
                ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC, button.id, 0));
        }
    }
}
