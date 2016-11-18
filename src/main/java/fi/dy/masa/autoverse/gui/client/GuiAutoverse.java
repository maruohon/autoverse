package fi.dy.masa.autoverse.gui.client;

import java.io.IOException;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.autoverse.inventory.container.ContainerAutoverse;
import fi.dy.masa.autoverse.reference.ReferenceTextures;

public class GuiAutoverse extends GuiContainer
{
    protected final ContainerAutoverse container;
    protected final EntityPlayer player;
    protected final ResourceLocation guiTexture;
    protected final ResourceLocation guiTextureWidgets;
    protected int backgroundU;
    protected int backgroundV;

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

        for (int slot = 0; slot < this.container.specialSlots.size(); slot++)
        {
            this.drawSpecialSlot(this.container.specialSlots.get(slot));
        }

        this.itemRender.zLevel = 0.0F;
        this.zLevel = 0.0F;

        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
    }

    public void drawSpecialSlot(Slot slotIn)
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
        //this.itemRender.renderItemOverlayIntoGUI(this.fontRendererObj, itemstack, slotPosX, slotPosY, str);
    }

    protected void drawTooltips(int mouseX, int mouseY)
    {
        /*
        for (int i = 0; i < this.buttonList.size(); i++)
        {
            GuiButton button = (GuiButton)this.buttonList.get(i);

            // Mouse is over the button
            if ((button instanceof GuiButtonHoverText) && button.mousePressed(this.mc, mouseX, mouseY) == true)
            {
                this.drawHoveringText(((GuiButtonHoverText)button).getHoverStrings(), mouseX, mouseY, this.fontRendererObj);
            }
        }
        */
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (int l = 0; l < this.buttonList.size(); ++l)
        {
            GuiButton guibutton = (GuiButton)this.buttonList.get(l);

            if (guibutton.mousePressed(this.mc, mouseX, mouseY) == true)
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

    protected void actionPerformedWithButton(GuiButton guiButton, int mouseButton) throws IOException { }

    protected void bindTexture(ResourceLocation rl)
    {
        this.mc.getTextureManager().bindTexture(rl);
    }
}
