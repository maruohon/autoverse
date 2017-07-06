package fi.dy.masa.autoverse.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse.PlacementProperty;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.ItemType;
import fi.dy.masa.autoverse.util.PlacementProperties;
import fi.dy.masa.autoverse.util.PositionUtils;

public class RenderEventHandler
{
    private static RenderEventHandler instance;
    public Minecraft mc;

    protected BlockPos pointedPosLast = BlockPos.ORIGIN;
    protected EnumFacing pointedBlockFacingLast = EnumFacing.DOWN;
    protected float partialTicks;

    public RenderEventHandler()
    {
        this.mc = Minecraft.getMinecraft();
        instance = this;
    }

    public static RenderEventHandler getInstance()
    {
        return instance;
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event)
    {
        if (event.getType() != ElementType.ALL)
        {
            return;
        }

        if (this.mc.currentScreen == null && this.mc.player != null)
        {
            this.renderPlacementPropertiesHud(this.mc.player);
        }
    }

    @SubscribeEvent
    public void onBlockHilight(DrawBlockHighlightEvent event)
    {
        RayTraceResult trace = event.getTarget();

        if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            World world = this.mc.world;
            BlockPos pos = trace.getBlockPos();
            IBlockState state = world.getBlockState(pos);
            state = state.getActualState(world, pos);
            Block block = state.getBlock();

            if (block == AutoverseBlocks.INVENTORY_READER)
            {
                this.updatePointedBlockHilight(world, trace.getBlockPos(), state, (BlockAutoverse) block, event.getPartialTicks());
            }
        }
    }

    /**
     * Renders text on the screen, with the given offset from the screen edge from the specified corner.<br>
     * <b>NOTE: Only BOTTOM_LEFT is currently implemented!!</b>
     * @param lines
     * @param offsetX
     * @param offsetY
     * @param align
     * @param useTextBackground
     * @param useFontShadow
     * @param mc
     */
    public static void renderText(List<String> lines, int offsetX, int offsetY, HudAlignment align,
            boolean useTextBackground, boolean useFontShadow, Minecraft mc)
    {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int scaledY = scaledResolution.getScaledHeight();
        int lineHeight = mc.fontRenderer.FONT_HEIGHT + 2;
        int posX = offsetX;
        int posY = offsetY;

        switch (align)
        {
            // TODO Add all the others, if needed some time...
            case TOP_LEFT:
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                break;

            case BOTTOM_LEFT:
                posY = scaledY - (lineHeight * lines.size()) - offsetY;
        }

        int textBgColor = 0x80000000;
        FontRenderer fontRenderer = mc.fontRenderer;

        for (String line : lines)
        {
            if (useTextBackground)
            {
                Gui.drawRect(posX - 2, posY - 2, posX + fontRenderer.getStringWidth(line) + 2, posY + fontRenderer.FONT_HEIGHT, textBgColor);
            }

            if (useFontShadow)
            {
                mc.ingameGUI.drawString(fontRenderer, line, posX, posY, 0xFFFFFFFF);
            }
            else
            {
                fontRenderer.drawString(line, posX, posY, 0xFFFFFFFF);
            }

            posY += fontRenderer.FONT_HEIGHT + 2;
        }
    }

    private void renderPlacementPropertiesHud(EntityPlayer player)
    {
        ItemStack stack = player.getHeldItemMainhand();

        if (stack.isEmpty() || (stack.getItem() instanceof ItemBlockAutoverse) == false)
        {
            stack = player.getHeldItemOffhand();
        }

        if (stack.isEmpty() == false && stack.getItem() instanceof ItemBlockAutoverse)
        {
            ItemBlockAutoverse item = (ItemBlockAutoverse) stack.getItem();

            if (item.hasPlacementProperty(stack))
            {
                renderText(this.getPlacementPropertiesText(item, stack, player), 4, 0, HudAlignment.BOTTOM_LEFT, true, true, this.mc);
            }
        }
    }

    private List<String> getPlacementPropertiesText(ItemBlockAutoverse item, ItemStack stack, EntityPlayer player)
    {
        String preGreen = TextFormatting.GREEN.toString();
        String rst = TextFormatting.RESET.toString() + TextFormatting.WHITE.toString();

        List<String> lines = new ArrayList<String>();
        PlacementProperties props = PlacementProperties.getInstance();
        UUID uuid = player.getUniqueID();
        PlacementProperty pp = item.getPlacementProperty(stack);
        boolean nbtSensitive = pp.isNBTSensitive();
        ItemType type = new ItemType(stack, nbtSensitive);
        int index = props.getPropertyIndex(uuid, type);
        int count = pp.getPropertyCount();

        for (int i = 0; i < count; i++)
        {
            Pair<String, Integer> pair = pp.getProperty(i);

            if (pair != null)
            {
                String key = pair.getLeft();
                String pre = (i == index) ? "> " : "  ";
                String name = I18n.format(Reference.MOD_ID + ".placement_properties." + key);
                int value = props.getPropertyValue(uuid, type, key, pair.getRight());
                String valueName = pp.getPropertyValueName(key, value);

                if (valueName == null)
                {
                    valueName = String.valueOf(value);
                }
                else
                {
                    String translationKey = Reference.MOD_ID + ".placement_properties.valuenames." + key + "." + valueName;
                    String translated = I18n.format(translationKey);

                    // If there is a translation, use that, otherwise use the value name key directly
                    if (translated.equals(translationKey) == false)
                    {
                        valueName = translated;
                    }
                }

                lines.add(String.format("%s%s: %s%s%s", pre, name, preGreen, valueName, rst));
            }
        }

        return lines;
    }

    public <T> AxisAlignedBB getPointedHilightBox(BlockAutoverse block)
    {
        Map<T, AxisAlignedBB> boxMap = block.getHilightBoxMap();
        T key = EntityUtils.getPointedBox(this.mc.getRenderViewEntity(), 6d, boxMap, this.partialTicks);

        if (key != null)
        {
            return boxMap.get(key);
        }

        return PositionUtils.ZERO_BB;
    }

    protected <T> void updatePointedBlockHilight(World world, BlockPos pos, IBlockState state, BlockAutoverse block, float partialTicks)
    {
        EnumFacing facing = state.getValue(BlockAutoverse.FACING);

        if (pos.equals(this.pointedPosLast) == false || facing != this.pointedBlockFacingLast)
        {
            block.updateBlockHilightBoxes(world, pos, facing);
            this.pointedPosLast = pos;
            this.pointedBlockFacingLast = facing;
        }

        this.partialTicks = partialTicks;
    }

    public enum HudAlignment
    {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
