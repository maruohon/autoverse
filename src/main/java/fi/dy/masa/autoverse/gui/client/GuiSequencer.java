package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverseTile;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.container.ContainerSequencer;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;

public class GuiSequencer extends GuiAutoverseTile
{
    private final ContainerSequencer containrSeq;
    private final TileEntitySequencer teseq;

    public GuiSequencer(ContainerSequencer container, TileEntitySequencer te)
    {
        super(container, 176, 168, "gui.container.sequencer_basic", te);

        this.containrSeq = container;
        this.teseq = te;
        this.infoArea = new InfoArea(160, 5, 11, 11, "autoverse.gui.infoarea.sequencer_basic");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String s = this.teseq.hasCustomName() ? this.teseq.getName() : I18n.format("autoverse.container.sequencer_basic");
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 6, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 75, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.drawSlotBackgrounds(7, 31, 0, 220, 9, this.teseq.getBaseItemHandler().getSlots());
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

    @Override
    protected void createButtons()
    {
         this.addButton(new GuiButtonHoverText(0, this.guiLeft + 156, this.guiTop + 19, 8, 8, 0, 0,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.inventory_size"));

         this.setButtonMultipliers(9, 4);
    }
}
