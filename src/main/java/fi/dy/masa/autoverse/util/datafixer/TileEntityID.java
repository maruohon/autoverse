package fi.dy.masa.autoverse.util.datafixer;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.IFixableData;

public class TileEntityID implements IFixableData
{
    private static final Map<String, String> OLD_TO_NEW_ID_MAP = new HashMap<String, String>();

    public int getFixVersion()
    {
        return 704;
    }

    public NBTTagCompound fixTagCompound(NBTTagCompound tag)
    {
        String newId = OLD_TO_NEW_ID_MAP.get(tag.getString("id"));

        if (newId != null)
        {
            tag.setString("id", newId);
        }

        return tag;
    }

    public static Map<String, String> getMap()
    {
        return OLD_TO_NEW_ID_MAP;
    }

    static
    {
        OLD_TO_NEW_ID_MAP.put("autoverse_barrel",                   "autoverse:barrel");
        OLD_TO_NEW_ID_MAP.put("autoverse_buffer_fifo",              "autoverse:buffer_fifo");
        OLD_TO_NEW_ID_MAP.put("autoverse_buffer_fifo_pulsed",       "autoverse:buffer_fifo_pulsed");
        OLD_TO_NEW_ID_MAP.put("autoverse_filter",                   "autoverse:filter");
        OLD_TO_NEW_ID_MAP.put("autoverse_filter_sequential",        "autoverse:filter_sequential");
        OLD_TO_NEW_ID_MAP.put("autoverse_filter_sequential_smart",  "autoverse:filter_sequential_smart");
        OLD_TO_NEW_ID_MAP.put("autoverse_sequencer",                "autoverse:sequencer");
        OLD_TO_NEW_ID_MAP.put("autoverse_splitter",                 "autoverse:splitter");
    }
}
