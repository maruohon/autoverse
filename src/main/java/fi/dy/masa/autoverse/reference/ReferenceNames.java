package fi.dy.masa.autoverse.reference;

public class ReferenceNames
{
    public static final String NAME_BLOCK_BARREL                    = "barrel";
    public static final String NAME_BLOCK_BUFFER                    = "buffer";
    public static final String NAME_BLOCK_FILTER                    = "filter";
    public static final String NAME_BLOCK_FILTER_SEQUENTIAL         = "filter_sequential";

    public static final String NAME_TILE_ENTITY_BARREL              = "barrel";
    public static final String NAME_TILE_ENTITY_BUFFER_FIFO         = "buffer_fifo";
    public static final String NAME_TILE_ENTITY_BUFFER_FIFO_PULSED  = "buffer_fifo_pulsed";
    public static final String NAME_TILE_ENTITY_FILTER              = "filter";
    public static final String NAME_TILE_ENTITY_FILTER_SEQUENTTIAL  = "filter_sequential";


    public static String getPrefixedName(String name)
    {
        return Reference.MOD_ID + "_" + name;
    }

    public static String getDotPrefixedName(String name)
    {
        return Reference.MOD_ID + "." + name;
    }
}
