package fi.dy.masa.autoverse.proxy;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import fi.dy.masa.autoverse.config.Configs;

public class ClientProxy extends CommonProxy
{
    public static KeyBinding keyToggleMode;

    @Override
    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new Configs());
    }
}
