package fi.dy.masa.autoverse.reference;

import net.minecraft.util.ResourceLocation;

public class ReferenceTextures
{
    public static final String RESOURCE_PREFIX = Reference.MOD_ID.toLowerCase() + ":";
    public static final String GUI_LOCATION = "textures/gui/";
    public static final String ITEM_LOCATION = "items/";


    public static String getEntityTextureName(String name)
    {
        return Reference.MOD_ID + ":textures/entity/entity." + name + ".png";
    }

    public static ResourceLocation getGuiTexture(String name)
    {
        return getResourceLocation(Reference.MOD_ID.toLowerCase(), GUI_LOCATION + name + ".png");
    }

    public static ResourceLocation getItemTexture(String name)
    {
        return getResourceLocation(Reference.MOD_ID.toLowerCase(), ITEM_LOCATION + name);
    }

    public static ResourceLocation getResourceLocation(String modId, String path)
    {
        return new ResourceLocation(modId, path);
    }
}
