package fi.dy.masa.autoverse.event;

import org.lwjgl.input.Keyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import fi.dy.masa.autoverse.client.HotKeys;
import fi.dy.masa.autoverse.gui.client.GuiScreenItemNameField;
import fi.dy.masa.autoverse.item.base.IKeyBound;
import fi.dy.masa.autoverse.item.base.IStringInput;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageKeyPressed;
import fi.dy.masa.autoverse.util.EntityUtils;

public class InputEventHandler
{
    /** Has the active mouse scroll modifier mask, if any */
    private static int scrollingMask = 0;
    /** Has the currently active/pressed mask of supported modifier keys */
    private static int modifierMask = 0;

    /**
     * Reset the modifiers externally. This is to fix the stuck modifier keys
     * if a GUI is opened while the modifiers are active.
     * FIXME Apparently there are key input events for GUI screens in 1.8,
     * so this probably can be removed then.
     */
    public static void resetModifiers()
    {
        scrollingMask = 0;
        modifierMask = 0;
    }

    @SubscribeEvent
    public void onGuiOpenEvent(GuiOpenEvent event)
    {
        // Reset the scrolling modifier when the player opens a GUI.
        // Otherwise the key up event will get eaten and our scrolling mode will get stuck on
        // until the player sneaks again.
        // FIXME Apparently there are key input events for GUI screens in 1.8,
        // so this probably can be removed then.
        InputEventHandler.resetModifiers();
    }

    @SubscribeEvent
    public void onKeyInputEvent(InputEvent.KeyInputEvent event)
    {
        EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();
        int eventKey = Keyboard.getEventKey();
        boolean keyState = Keyboard.getEventKeyState();
        Minecraft mc = FMLClientHandler.instance().getClient();

        // One of our supported modifier keys was pressed or released
        if (HotKeys.isModifierKey(eventKey))
        {
            int mask = HotKeys.getModifierMask(eventKey);

            // Key was pressed
            if (keyState)
            {
                modifierMask |= mask;

                // Only add scrolling mode mask if the currently selected item is one of our IKeyBound items
                if (isHoldingKeyboundItem(player))
                {
                    scrollingMask |= mask;
                }
            }
            // Key was released
            else
            {
                modifierMask &= ~mask;
                scrollingMask &= ~mask;
            }
        }

        // In-game (no GUI open)
        if (mc.inGameHasFocus && keyState)
        {
            if (eventKey == HotKeys.keyToggleMode.getKeyCode() && this.stringInputItemHandling())
            {
                return;
            }

            if (isHoldingKeyboundItem(player))
            {
                if (eventKey == HotKeys.keyToggleMode.getKeyCode())
                {
                    int keyCode = HotKeys.KEYBIND_ID_TOGGLE_MODE | modifierMask;
                    PacketHandler.INSTANCE.sendToServer(new MessageKeyPressed(keyCode));
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event)
    {
        EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();
        Minecraft mc = FMLClientHandler.instance().getClient();
        int dWheel = event.getDwheel();

        if (dWheel != 0)
        {
            dWheel /= 120;

            // If the player pressed down a modifier key while holding an IKeyBound item
            // (note: this means it specifically WON'T work if the player started pressing a modifier
            // key while holding something else, for example when scrolling through the hotbar!!),
            // then we allow for easily scrolling through the changeable stuff using the mouse wheel.
            if (scrollingMask != 0)
            {
                if (isHoldingKeyboundItem(player))
                {
                    int key = HotKeys.KEYCODE_SCROLL | scrollingMask;

                    // Scrolling up, reverse the direction.
                    if (dWheel > 0)
                    {
                        key |= HotKeys.SCROLL_MODIFIER_REVERSE;
                    }

                    if (event.isCancelable())
                    {
                        event.setCanceled(true);
                    }

                    PacketHandler.INSTANCE.sendToServer(new MessageKeyPressed(key));
                }
            }
        }
        else
        {
            if (event.isButtonstate() && isHoldingKeyboundItem(player))
            {
                int eventKey = event.getButton();
                int keyCode = -1;
                int middleKey = mc.gameSettings.keyBindPickBlock.getKeyCode();
                middleKey = middleKey < 0 ? middleKey + 100 : middleKey;

                if (eventKey == HotKeys.keyToggleMode.getKeyCode())
                {
                    keyCode = HotKeys.KEYBIND_ID_TOGGLE_MODE | modifierMask;
                }
                else if (eventKey == middleKey)
                {
                    keyCode = HotKeys.KEYCODE_MIDDLE_CLICK | modifierMask;

                    // FIXME this is a bit of an ugly way to prevent pick-blocking while holding a WotLS or a BPPC
                    if (event.isCancelable() && EntityUtils.getHeldItemOfType(player, IKeyBound.class).isEmpty() == false)
                    {
                        event.setCanceled(true);
                    }
                }

                if (keyCode != -1)
                {
                    PacketHandler.INSTANCE.sendToServer(new MessageKeyPressed(keyCode));
                }
            }
        }
    }

    public static boolean isHoldingKeyboundItem(EntityPlayer player)
    {
        ItemStack stack = EntityUtils.getHeldItemOfType(player, IKeyBound.class);

        return stack.isEmpty() == false && ((stack.getItem() instanceof ItemBlockAutoverse) == false || 
                ((ItemBlockAutoverse) stack.getItem()).hasPlacementProperty(stack));
    }

    private boolean stringInputItemHandling()
    {
        if (GuiScreen.isShiftKeyDown() || GuiScreen.isCtrlKeyDown() || GuiScreen.isAltKeyDown())
        {
            return false;
        }

        ItemStack stack = EntityUtils.getHeldItemOfType(Minecraft.getMinecraft().player, IStringInput.class);

        if (stack.isEmpty() == false)
        {
            Minecraft.getMinecraft().displayGuiScreen(new GuiScreenItemNameField());
            return true;
        }

        return false;
    }
}
