package fi.dy.masa.autoverse.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public class NBTUtils
{
    /**
     * Reads the stored items from the provided NBTTagCompound, from a NBTTagList by the name <b>tagName</b>
     * and writes them to the provided array of ItemStacks <b>items</b>.
     * @param tag
     * @param items
     * @param tagName
     */
    public static void readStoredItemsFromTag(NBTTagCompound nbt, ItemStack[] items, String tagName)
    {
        if (nbt == null || nbt.hasKey(tagName, Constants.NBT.TAG_LIST) == false)
        {
            return;
        }

        NBTTagList nbtTagList = nbt.getTagList(tagName, Constants.NBT.TAG_COMPOUND);
        int num = nbtTagList.tagCount();

        for (int i = 0; i < num; ++i)
        {
            NBTTagCompound tag = nbtTagList.getCompoundTagAt(i);
            byte slotNum = tag.getByte("Slot");

            if (slotNum >= 0 && slotNum < items.length)
            {
                items[slotNum] = ItemStack.loadItemStackFromNBT(tag);

                if (items[slotNum] != null && tag.hasKey("ActualCount", Constants.NBT.TAG_INT))
                {
                    items[slotNum].stackSize = tag.getInteger("ActualCount");
                }
            }
            /*else
            {
                EnderUtilities.logger.warn("Failed to read items from NBT, invalid slot: " + slotNum + " (max: " + (items.length - 1) + ")");
            }*/
        }
    }

    /**
     * Writes the ItemStacks in <b>items</b> to a new NBTTagList and returns that list.
     * @param items
     */
    public static NBTTagList createTagListForItems(ItemStack[] items)
    {
        int invSlots = items.length;
        NBTTagList nbtTagList = new NBTTagList();

        // Write all the ItemStacks into a TAG_List
        for (int slotNum = 0; slotNum < invSlots && slotNum <= 127; slotNum++)
        {
            if (items[slotNum] != null)
            {
                NBTTagCompound tag = new NBTTagCompound();
                items[slotNum].writeToNBT(tag);
                tag.setInteger("ActualCount", items[slotNum].stackSize);
                tag.setByte("Slot", (byte)slotNum);
                nbtTagList.appendTag(tag);
            }
        }

        return nbtTagList;
    }

    /**
     * Writes the ItemStacks in <b>items</b> to the NBTTagCompound <b>nbt</b>
     * in a NBTTagList by the name <b>tagName</b>.
     * @param nbt
     * @param items
     * @param tagName the NBTTagList tag name where the items will be written to
     * @param keepExtraSlots set to true to append existing items in slots that are outside of the currently written slot range
     */
    public static NBTTagCompound writeItemsToTag(NBTTagCompound nbt, ItemStack[] items, String tagName, boolean keepExtraSlots)
    {
        if (nbt == null || items == null)
        {
            return nbt;
        }

        int invSlots = items.length;
        NBTTagList nbtTagList = createTagListForItems(items);

        if (keepExtraSlots == true && nbt.hasKey(tagName, Constants.NBT.TAG_LIST) == true)
        {
            // Read the old items and append any existing items that are outside the current written slot range
            NBTTagList nbtTagListExisting = nbt.getTagList(tagName, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < nbtTagListExisting.tagCount(); i++)
            {
                NBTTagCompound tag = nbtTagListExisting.getCompoundTagAt(i);
                byte slotNum = tag.getByte("Slot");
                if (slotNum >= invSlots && slotNum <= 127)
                {
                    nbtTagList.appendTag(tag);
                }
            }
        }

        // Write the items to the compound tag
        if (nbtTagList.tagCount() > 0)
        {
            nbt.setTag(tagName, nbtTagList);
        }
        else
        {
            nbt.removeTag(tagName);
        }

        return nbt;
    }
}
