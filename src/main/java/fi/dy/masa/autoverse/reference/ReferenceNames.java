package fi.dy.masa.autoverse.reference;

public class ReferenceNames
{
    public static final String NAME_BLOCK_BARREL                    = "barrel";
    public static final String NAME_BLOCK_BLOCK_READER              = "block_reader";
    public static final String NAME_BLOCK_BREAKER                   = "breaker";
    public static final String NAME_BLOCK_BUFFER                    = "buffer";
    public static final String NAME_BLOCK_CIRCUIT                   = "circuit";
    public static final String NAME_BLOCK_CRAFTER                   = "crafter";
    public static final String NAME_BLOCK_DETECTOR                  = "detector";
    public static final String NAME_BLOCK_FILTER                    = "filter";
    public static final String NAME_BLOCK_FILTER_SEQUENTIAL         = "filter_sequential";
    public static final String NAME_BLOCK_FILTER_SEQUENTIAL_STRICT  = "filter_sequential_strict";
    public static final String NAME_BLOCK_INVENTORY_READER          = "inventory_reader";
    public static final String NAME_BLOCK_MACHINE_FRAME             = "machine_frame";
    public static final String NAME_BLOCK_MUXER                     = "muxer";
    public static final String NAME_BLOCK_PIPE                      = "pipe";
    public static final String NAME_BLOCK_PLACER                    = "placer";
    public static final String NAME_BLOCK_REDSTONE_EMITTER          = "redstone_emitter";
    public static final String NAME_BLOCK_SENSOR                    = "sensor";
    public static final String NAME_BLOCK_SEQUENCE_DETECTOR         = "sequence_detector";
    public static final String NAME_BLOCK_SEQUENCER                 = "sequencer";
    public static final String NAME_BLOCK_SEQUENCER_PROGRAMMABLE    = "sequencer_programmable";
    public static final String NAME_BLOCK_SPLITTER                  = "splitter";
    public static final String NAME_BLOCK_TRASH                     = "trash";

    public static final String NAME_TILE_ENTITY_BLOCK_READER_NBT    = "block_reader_nbt";
    public static final String NAME_TILE_ENTITY_BUFFER_FIFO         = "buffer_fifo";
    public static final String NAME_TILE_ENTITY_BUFFER_FIFO_AUTO    = "buffer_fifo_auto";
    public static final String NAME_TILE_ENTITY_BUFFER_FIFO_PULSED  = "buffer_fifo_pulsed";
    public static final String NAME_TILE_ENTITY_LATCH               = "latch";
    public static final String NAME_TILE_ENTITY_PIPE_DIRECTIONAL    = "pipe_dir";
    public static final String NAME_TILE_ENTITY_PIPE_EXTRACTION     = "pipe_ex";
    public static final String NAME_TILE_ENTITY_PIPE_ROUNDROBIN     = "pipe_rr";
    public static final String NAME_TILE_ENTITY_PLACER_PROGRAMMABLE = "placer_programmable";
    public static final String NAME_TILE_ENTITY_REDSTONE_EMITTER_ADVANCED = "redstone_emitter_advanced";
    public static final String NAME_TILE_ENTITY_SENSOR_HEIGHT       = "sensor_height";
    public static final String NAME_TILE_ENTITY_TRASH_BIN           = "trash_bin";
    public static final String NAME_TILE_ENTITY_TRASH_BUFFER        = "trash_buffer";

    public static final String NAME_ITEM_BLOCK_PLACER_CONFIGURATOR  = "configurator";
    public static final String NAME_ITEM_WAND                       = "wand";

    public static String getPrefixedName(String name)
    {
        return Reference.MOD_ID + "_" + name;
    }

    public static String getDotPrefixedName(String name)
    {
        return Reference.MOD_ID + "." + name;
    }
}
