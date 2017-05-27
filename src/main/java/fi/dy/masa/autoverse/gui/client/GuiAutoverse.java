package fi.dy.masa.autoverse.gui.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.client.HotKeys;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.item.base.ItemAutoverse;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageGuiAction;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.reference.ReferenceTextures;

@Mod.EventBusSubscriber(Side.CLIENT)
public class GuiAutoverse extends GuiContainer
{
    protected final ContainerAutoverse container;
    protected final EntityPlayer player;
    protected final ResourceLocation guiTexture;
    protected final ResourceLocation guiTextureWidgets;
    protected int backgroundU;
    protected int backgroundV;
    protected InfoArea infoArea;

    public GuiAutoverse(ContainerAutoverse container, int xSize, int ySize, String textureName)
    {
        super(container);
        this.container = container;
        this.player = container.getPlayer();
        this.xSize = xSize;
        this.ySize = ySize;
        this.guiTexture = ReferenceTextures.getGuiTexture(textureName);
        this.guiTextureWidgets = ReferenceTextures.getGuiTexture("gui.widgets");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float gameTicks)
    {
        super.drawScreen(mouseX, mouseY, gameTicks);

        this.drawSpecialSlots();
        this.drawTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        this.bindTexture(this.guiTexture);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, this.backgroundU, this.backgroundV, this.xSize, this.ySize);

        if (this.infoArea != null)
        {
            this.infoArea.render(this, this.guiTextureWidgets);
        }

        this.bindTexture(this.guiTexture);
    }

    protected void drawSpecialSlots()
    {
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.pushMatrix();
        //GlStateManager.translate((float)this.guiLeft, (float)this.guiTop, 0.0F);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableRescaleNormal();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

        this.zLevel = 100.0F;
        this.itemRender.zLevel = 100.0F;

        for (int slot = 0; slot < this.container.getSpecialSlots().size(); slot++)
        {
            this.drawSpecialSlot(this.container.getSpecialSlots().get(slot));
        }

        this.itemRender.zLevel = 0.0F;
        this.zLevel = 0.0F;

        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
    }

    protected void drawSpecialSlot(Slot slotIn)
    {
        int x = this.guiLeft + slotIn.xPos;
        int y = this.guiTop + slotIn.yPos;
        ItemStack stack = slotIn.getStack();

        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        //this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, itemstack, slotPosX, slotPosY, str);
    }

    protected void drawTooltips(int mouseX, int mouseY)
    {
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // Info text has been set, show it if the mouse is over the designated info area
        if (this.infoArea != null && this.infoArea.isMouseOver(mouseX, mouseY, x, y))
        {
            this.drawHoveringText(this.infoArea.getInfoLines(), mouseX, mouseY, this.fontRenderer);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (int l = 0; l < this.buttonList.size(); ++l)
        {
            GuiButton guibutton = (GuiButton)this.buttonList.get(l);

            if (guibutton.mousePressed(this.mc, mouseX, mouseY))
            {
                // Vanilla GUI only plays the click sound for the left click, we do it for other buttons here
                if (mouseButton != 0)
                {
                    guibutton.playPressSound(this.mc.getSoundHandler());
                }

                this.actionPerformedWithButton(guibutton, mouseButton);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        int dWheel = Mouse.getEventDWheel();

        if (dWheel != 0)
        {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            for (int i = 0; i < this.buttonList.size(); i++)
            {
                GuiButton button = this.buttonList.get(i);

                if (button.mousePressed(this.mc, mouseX, mouseY))
                {
                    this.actionPerformedWithButton(button, 10 + dWheel / 120);
                    break;
                }
            }
        }
        else
        {
            super.handleMouseInput();
        }
    }

    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type)
    {
        // Custom: middle click on a slot, with a modifier key active
        if (type == ClickType.CLONE && (isShiftKeyDown() || isCtrlKeyDown() || isAltKeyDown()))
        {
            if (slotIn != null)
            {
                slotId = slotIn.slotNumber;
            }

            int modifier = 0;

            if (isShiftKeyDown()) { modifier |= HotKeys.MOD_SHIFT; }
            if (isCtrlKeyDown())  { modifier |= HotKeys.MOD_CTRL;  }
            if (isAltKeyDown())   { modifier |= HotKeys.MOD_ALT;   }

            int action = HotKeys.KEYCODE_MIDDLE_CLICK | modifier;

            // Send a packet to the server
            PacketHandler.INSTANCE.sendToServer(new MessageGuiAction(0, BlockPos.ORIGIN,
                    ReferenceGuiIds.GUI_ID_CONTAINER_GENERIC, action, slotId));
        }
        else
        {
            super.handleMouseClick(slotIn, slotId, mouseButton, type);
        }
    }

    /**
     * Called when a mouse action is performed. Wheel actions have a value (dWheel / 120) + 10.
     * @param guiButton
     * @param mouseButton
     * @throws IOException
     */
    protected void actionPerformedWithButton(GuiButton guiButton, int mouseButton) throws IOException { }

    protected void bindTexture(ResourceLocation rl)
    {
        this.mc.getTextureManager().bindTexture(rl);
    }

    public static class InfoArea
    {
        private final int posX;
        private final int posY;
        private final int width;
        private final int height;
        private final String infoText;
        private final Object[] args;
        private int u;
        private int v;

        public InfoArea(int x, int y, int width, int height, String infoTextKey, Object... args)
        {
            this.posX = x;
            this.posY = y;
            this.width = width;
            this.height = height;
            this.infoText = infoTextKey;
            this.args = args;

            // Default texture locations on the widgets sheet
            this.u = 134;

            if (width == 11)
            {
                this.v = 66;
            }
            else if (width == 18)
            {
                this.v = 48;
            }
        }

        public void setUV(int u, int v)
        {
            this.u = u;
            this.v = v;
        }

        public List<String> getInfoLines()
        {
            List<String> lines = new ArrayList<String>();
            ItemAutoverse.addTooltips(this.infoText, lines, false, this.args);
            return lines;
        }

        public boolean isMouseOver(int mouseX, int mouseY, int guiLeft, int guiTop)
        {
            return mouseX >= guiLeft + this.posX && mouseX < guiLeft + this.posX + this.width &&
                   mouseY >= guiTop + this.posY && mouseY < guiTop + this.posY + this.height;
        }

        public void render(GuiAutoverse gui, ResourceLocation texture)
        {
            gui.bindTexture(texture);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            gui.drawTexturedModalRect(gui.guiLeft + this.posX, gui.guiTop + this.posY, this.u, this.v, this.width, this.height);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onMouseInputEventPre(MouseInputEvent.Pre event)
    {
        // Handle the mouse input inside all of the mod's GUIs via the event and then cancel the event,
        // so that some mods like Inventory Sorter don't try to sort the Autoverse inventories.
        // Using priority LOW should still allow even older versions of Item Scroller to work,
        // since it uses normal priority.
        if (event.getGui() instanceof GuiAutoverse)
        {
            try
            {
                event.getGui().handleMouseInput();
                event.setCanceled(true);
            }
            catch (IOException e)
            {
                Autoverse.logger.warn("Exception while executing handleMouseInput() on {}", event.getGui().getClass().getName());
            }
        }
    }

    @SubscribeEvent
    public static void onPotionShiftEvent(GuiScreenEvent.PotionShiftEvent event)
    {
        // Disable the potion shift in all GUIs in this mod
        if (event.getGui() instanceof GuiAutoverse)
        {
            event.setCanceled(true);
        }
    }
}
