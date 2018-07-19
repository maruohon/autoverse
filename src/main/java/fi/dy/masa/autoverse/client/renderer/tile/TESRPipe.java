package fi.dy.masa.autoverse.client.renderer.tile;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import fi.dy.masa.autoverse.tileentity.TileEntityPipe;

public class TESRPipe extends TileEntitySpecialRenderer<TileEntityPipe>
{
    private RenderItem renderItem;
    private Minecraft mc;
    private Random rand = new Random();

    @Override
    public void render(TileEntityPipe te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
    {
        this.mc = Minecraft.getMinecraft();
        BlockPos pos = te.getPos();

        if (this.mc.player.getDistanceSq(pos) < 900) // 30m
        {
            x += 0.5;
            y += 0.315;
            z += 0.5;

            boolean fancy = this.mc.gameSettings.fancyGraphics;
            this.renderItem = this.mc.getRenderItem();
            this.mc.gameSettings.fancyGraphics = true;

            int ambLight = this.getWorld().getCombinedLight(pos, 0);
            int lu = ambLight % 65536;
            int lv = ambLight / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lu / 1.0F, (float) lv / 1.0F);

            // Render the ItemStacks
            for (int sideIndex = 0; sideIndex < 6; sideIndex++)
            {
                float fullDelay = (float) te.getDelayForSide(sideIndex);

                // Incoming stacks
                if (te.delaysClient[sideIndex] >= -1)
                {
                    if (te.delaysClient[sideIndex] >= 0 && partialTicks < te.partialTicksLast)
                    {
                        te.delaysClient[sideIndex]--;
                    }

                    if (te.isInput[sideIndex] == 0 || te.delaysClient[sideIndex] < fullDelay / 2)
                    {
                        ItemStack stack = te.stacksLast.get(sideIndex);
                        float progress = 0f;

                        if (te.delaysClient[sideIndex] >= 0)
                        {
                            // 1 ... 0
                            progress = (((float) te.delaysClient[sideIndex] + 1f - partialTicks) / fullDelay);
                        }

                        // The item is (normally, see above) rendered moving from the center
                        // of the block on the input side, to the center of this block.
                        EnumFacing inputSide = EnumFacing.byIndex(sideIndex);
                        double posX = x + inputSide.getXOffset() * progress;
                        double posY = y + inputSide.getYOffset() * progress;
                        double posZ = z + inputSide.getZOffset() * progress;

                        this.renderStack(stack, posX, posY, posZ, partialTicks);
                    }
                }

                if (te.delaysOut[sideIndex] >= -1 && partialTicks < te.partialTicksLast)
                {
                    if (--te.delaysOut[sideIndex] < -1)
                    {
                        te.stacksOut.set(sideIndex, ItemStack.EMPTY);
                    }
                }

                // Outgoing stacks
                if (te.delaysOut[sideIndex] >= -1)
                {
                    ItemStack stack = te.stacksOut.get(sideIndex);
                    float progress = 0.5f;
                    fullDelay /= 2;

                    if (te.delaysOut[sideIndex] >= 0)
                    {
                        // 0 ... 0.5
                        progress = 0.5f * (1f - (((float) te.delaysOut[sideIndex] + 1f - partialTicks) / fullDelay));
                    }

                    // The item is rendered moving from the center of this block
                    // to the edge of this block on the output side.
                    EnumFacing outputSide = EnumFacing.byIndex(te.outputDirections[sideIndex]);
                    double posX = x + outputSide.getXOffset() * progress;
                    double posY = y + outputSide.getYOffset() * progress;
                    double posZ = z + outputSide.getZOffset() * progress;

                    this.renderStack(stack, posX, posY, posZ, partialTicks);
                }
            }

            te.partialTicksLast = partialTicks;

            GlStateManager.enableLighting();
            GlStateManager.enableLight(0);
            GlStateManager.enableLight(1);
            GlStateManager.enableColorMaterial();
            GlStateManager.colorMaterial(1032, 5634);
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableNormalize();
            GlStateManager.disableBlend();

            this.mc.gameSettings.fancyGraphics = fancy;
        }
    }

    protected void renderStack(ItemStack stack, double posX, double posY, double posZ, float partialTicks)
    {
        if (stack.isEmpty())
        {
            return;
        }

        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.pushMatrix();

        IBakedModel model = this.renderItem.getItemModelWithOverrides(stack, this.mc.world, (EntityLivingBase) null);
        float rot = (float) ((this.mc.world.getTotalWorldTime() % 100) + partialTicks) / 100f * 360f;
        int modelCount = this.getModelCount(stack);
        boolean isGui3d = model.isGui3d();
        boolean shouldSpreadItems = true;
        int seed = Item.getIdFromItem(stack.getItem()) + stack.getMetadata();
        this.rand.setSeed(seed);

        GlStateManager.translate(posX, posY, posZ);
        GlStateManager.rotate(rot, 0.0F, 1.0F, 0.0F);

        if (isGui3d == false)
        {
            float trX = -0.0F * (float)(modelCount - 1) * 0.5F;
            float trY = -0.0F * (float)(modelCount - 1) * 0.5F;
            float trZ = -0.09375F * (float)(modelCount - 1) * 0.5F;
            GlStateManager.translate(trX, trY, trZ);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableStandardItemLighting();

        // TileEntitySkullRenderer alters both of these options on, but does not restore them.
        GlStateManager.enableCull();
        // This extra enable toggle is to fix a render glitch with items in Item Frames on screen,
        // and then hovering over a stack in an inventory... Rendering... always lovely...
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableRescaleNormal();

        // DIRTY HACK: Fool GlStateManager into thinking GL_RESCALE_NORMAL is enabled, but disable
        // it using popAttrib This prevents RenderItem from enabling it again.
        GlStateManager.pushAttrib();
        GlStateManager.enableRescaleNormal();
        GlStateManager.popAttrib();

        this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        for (int k = 0; k < modelCount; ++k)
        {
            if (isGui3d)
            {
                GlStateManager.pushMatrix();

                if (k > 0)
                {
                    float trX = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float trY = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float trZ = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    GlStateManager.translate(shouldSpreadItems ? trX : 0, shouldSpreadItems ? trY : 0, trZ);
                }

                model = ForgeHooksClient.handleCameraTransforms(model, ItemCameraTransforms.TransformType.GROUND, false);
                this.renderItem.renderItem(stack, model);

                GlStateManager.popMatrix();
            }
            else
            {
                GlStateManager.pushMatrix();

                if (k > 0)
                {
                    float trX = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    float trY = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    GlStateManager.translate(trX, trY, 0.0F);
                }

                model = ForgeHooksClient.handleCameraTransforms(model, ItemCameraTransforms.TransformType.GROUND, false);
                this.renderItem.renderItem(stack, model);

                GlStateManager.popMatrix();
                GlStateManager.translate(0.0F, 0.0F, 0.09375F);
            }
        }

        GlStateManager.disableBlend(); // Clean up after RenderItem
        GlStateManager.enableAlpha();  // Restore world render state after RenderItem
        GlStateManager.popMatrix();
    }

    protected int getModelCount(ItemStack stack)
    {
        int count = 1;

        if (stack.getCount() > 48)
        {
            count = 5;
        }
        else if (stack.getCount() > 32)
        {
            count = 4;
        }
        else if (stack.getCount() > 16)
        {
            count = 3;
        }
        else if (stack.getCount() > 1)
        {
            count = 2;
        }

        return count;
    }
}
