package fi.dy.masa.autoverse.client.renderer.tile;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import fi.dy.masa.autoverse.tileentity.TileEntityPipe;

public class TESRPipe extends TileEntitySpecialRenderer<TileEntityPipe>
{
    private RenderItem renderItem;
    private Minecraft mc;
    private Random rand = new Random();

    @Override
    public void render(TileEntityPipe te, double x, double y, double z, float partialTicks, int destroyStage, float partial)
    {
        //System.out.printf("TESR 1 - pos: %.1f %.1f %.1f\n", x, y, z);
        this.mc = Minecraft.getMinecraft();
        BlockPos pos = te.getPos();

        if (this.mc.player.getDistanceSq(pos) < 900) // 30m
        {
            x += 0.5;
            y += 0.5;
            z += 0.5;

            NonNullList<ItemStack> stacks = te.getRenderStacks();

            boolean fancy = this.mc.gameSettings.fancyGraphics;
            this.renderItem = this.mc.getRenderItem();
            this.mc.gameSettings.fancyGraphics = true;

            int ambLight = this.getWorld().getCombinedLight(pos, 0);
            int lu = ambLight % 65536;
            int lv = ambLight / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lu / 1.0F, (float) lv / 1.0F);

            // Render the ItemStacks
            for (int i = 0; i < te.delaysClient.length; i++)
            {
                int delay = te.delaysClient[i];

                if (delay >= 0 && partialTicks < te.partialTicksLast)
                {
                    te.delaysClient[i]--;
                    delay--;
                }

                if (delay >= -1)
                {
                    ItemStack stack = stacks.get(i);
                    float progress = -0.5f;

                    if (delay >= 0)
                    {
                        progress = -0.5f + (((float) delay + 1f - partialTicks) / (float) te.delays[i]);
                    }
                    else
                    {
                        //progress = -0.5f;
                    }

                    EnumFacing inputSide = EnumFacing.getFront(i);
                    double posX = x + inputSide.getFrontOffsetX() * progress;
                    double posY = y + inputSide.getFrontOffsetY() * progress;
                    double posZ = z + inputSide.getFrontOffsetZ() * progress;

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
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.pushMatrix();
        IBakedModel model = this.renderItem.getItemModelWithOverrides(stack, this.mc.world, (EntityLivingBase)null);
        int modelCount = this.getModelCount(stack);
        boolean isGui3d = model.isGui3d();
        boolean shouldSpreadItems = true;
        float rot = (float) ((this.mc.world.getTotalWorldTime() % 100) + partialTicks) / 100f * 360f;

        GlStateManager.translate(posX, posY, posZ);
        GlStateManager.rotate(rot, 0.0F, 1.0F, 0.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (isGui3d == false)
        {
            float trX = -0.0F * (float)(modelCount - 1) * 0.5F;
            float trY = -0.0F * (float)(modelCount - 1) * 0.5F;
            float trZ = -0.09375F * (float)(modelCount - 1) * 0.5F;
            GlStateManager.translate(trX, trY, trZ);
        }

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

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();

        /*
        //System.out.printf("TESR 2 - pos: %.1f %.1f %.1f\n", posX, posY, posZ);
        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY, posZ);
        //GlStateManager.translate(-0.25, 0.2, 0); // This offset is currently added to the renderItemIntoGUI arguments
        GlStateManager.scale(0.35f, 0.35f, 0.35f);
        GlStateManager.scale(1 / 16f, -1 / 16f, 1 / 16f);

        // The following rendering stuff within this method has been taken from Storage Drawers by jaquadro, found here:
        // https://github.com/jaquadro/StorageDrawers/blob/e5719be6d64f757a1b58c25e8ca5bc64074c6b9e/src/com/jaquadro/minecraft/storagedrawers/client/renderer/TileEntityDrawersRenderer.java#L180

        // At the time GL_LIGHT* are configured, the coordinates are transformed by the modelview
        // matrix. The transformations used in `RenderHelper.enableGUIStandardItemLighting` are
        // suitable for the orthographic projection used by GUI windows, but they are just a little
        // bit off when rendering a block in 3D and squishing it flat. An additional term is added
        // to account for slightly different shading on the half-size "icons" in 1x2 and 2x2
        // drawers due to the extreme angles caused by flattening the block (as noted below).

        GlStateManager.pushMatrix();
        //GlStateManager.scale(2.6f, 2.6f, 1);
        //GlStateManager.rotate(171.6f, 0.0F, 1.0F, 0.0F);
        //GlStateManager.rotate(84.9f, 1.0F, 0.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();

        // TileEntitySkullRenderer alters both of these options on, but does not restore them.
        GlStateManager.enableCull();
        // This extra enable toggle is to fix a render glitch with items in Item Frames on screen,
        // and then hovering over a stack in an inventory... Rendering... always lovely...
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableRescaleNormal();

        // GL_POLYGON_OFFSET is used to offset flat icons toward the viewer (-Z) in screen space,
        // so they always appear on top of the drawer's front space.
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-1, -1);

        // DIRTY HACK: Fool GlStateManager into thinking GL_RESCALE_NORMAL is enabled, but disable
        // it using popAttrib This prevents RenderItem from enabling it again.
        //
        // Normals are transformed by the inverse of the modelview and projection matrices that
        // excludes the translate terms. When put through the extreme Z scale used to flatten the
        // block, this makes them point away from the drawer face at a very sharp angle. These
        // normals are no longer unit scale (normalized), and normalizing them via
        // GL_RESCALE_NORMAL causes a loss of precision that results in the normals pointing
        // directly away from the face, which is visible as the block faces having identical
        // (dark) shading.

        GlStateManager.pushAttrib();
        GlStateManager.enableRescaleNormal();
        GlStateManager.popAttrib();

        //RenderHelper.enableStandardItemLighting();

        this.renderItem.renderItemIntoGUI(stack, -8, -7);

        GlStateManager.disableBlend(); // Clean up after RenderItem
        GlStateManager.enableAlpha();  // Restore world render state after RenderItem
        //GlStateManager.disablePolygonOffset();

        GlStateManager.popMatrix();
        */
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
