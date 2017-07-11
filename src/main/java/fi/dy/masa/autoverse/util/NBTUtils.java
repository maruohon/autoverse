package fi.dy.masa.autoverse.util;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.reference.Reference;

public class NBTUtils
{
    /**
     * Sets the root compound tag in the given ItemStack. An empty compound will be stripped completely.
     */
    @Nonnull
    public static ItemStack setRootCompoundTag(@Nonnull ItemStack stack, @Nullable NBTTagCompound nbt)
    {
        if (nbt != null && nbt.hasNoTags())
        {
            nbt = null;
        }

        stack.setTagCompound(nbt);
        return stack;
    }

    /**
     * Get the root compound tag from the ItemStack.
     * If one doesn't exist, then it will be created and added if <b>create</b> is true, otherwise null is returned.
     */
    @Nullable
    public static NBTTagCompound getRootCompoundTag(@Nonnull ItemStack stack, boolean create)
    {
        NBTTagCompound nbt = stack.getTagCompound();

        if (create == false)
        {
            return nbt;
        }

        // create = true
        if (nbt == null)
        {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        return nbt;
    }

    /**
     * Get a compound tag by the given name <b>tagName</b> from the other compound tag <b>nbt</b>.
     * If one doesn't exist, then it will be created and added if <b>create</b> is true, otherwise null is returned.
     */
    @Nullable
    public static NBTTagCompound getCompoundTag(@Nullable NBTTagCompound nbt, @Nonnull String tagName, boolean create)
    {
        if (nbt == null)
        {
            return null;
        }

        if (create == false)
        {
            return nbt.hasKey(tagName, Constants.NBT.TAG_COMPOUND) ? nbt.getCompoundTag(tagName) : null;
        }

        // create = true

        if (nbt.hasKey(tagName, Constants.NBT.TAG_COMPOUND) == false)
        {
            nbt.setTag(tagName, new NBTTagCompound());
        }

        return nbt.getCompoundTag(tagName);
    }

    /**
     * Returns a compound tag by the given name <b>tagName</b>. If <b>tagName</b> is null,
     * then the root compound tag is returned instead. If <b>create</b> is <b>false</b>
     * and the tag doesn't exist, null is returned and the tag is not created.
     * If <b>create</b> is <b>true</b>, then the tag(s) are created and added if necessary.
     */
    @Nullable
    public static NBTTagCompound getCompoundTag(@Nonnull ItemStack stack, @Nullable String tagName, boolean create)
    {
        NBTTagCompound nbt = getRootCompoundTag(stack, create);

        if (tagName != null)
        {
            nbt = getCompoundTag(nbt, tagName, create);
        }

        return nbt;
    }

    /**
     * Reads a byte array from NBT into the provided int array.
     * The number of elements read is the minimum of the provided array's length and
     * the read byte array's length.
     * @param arr
     * @param nbt
     * @param tagName
     */
    public static void readByteArrayIntoIntArray(int[] arr, NBTTagCompound nbt, String tagName)
    {
        byte[] arrBytes = nbt.getByteArray(tagName);
        final int len = Math.min(arr.length, arrBytes.length);

        for (int i = 0; i < len; i++)
        {
            arr[i] = arrBytes[i];
        }
    }

    /**
     * Writes the provided int array into NBT as a byte array, by casting each element into a byte.
     * @param arr
     * @param nbt
     * @param tagName
     */
    public static void writeIntArrayAsByteArray(int[] arr, NBTTagCompound nbt, String tagName)
    {
        byte[] bytes = new byte[arr.length];

        for (int i = 0; i < arr.length; i++)
        {
            bytes[i] = (byte) arr[i];
        }

        nbt.setByteArray(tagName, bytes);
    }

    /**
     * Reads a byte array from NBT into the provided byte array.
     * The number of elements read is the minimum of the provided array's length and
     * the read byte array's length.
     * @param arr
     * @param nbt
     * @param tagName
     */
    public static void readByteArray(byte[] arr, NBTTagCompound nbt, String tagName)
    {
        byte[] arrNbt = nbt.getByteArray(tagName);
        final int len = Math.min(arr.length, arrNbt.length);

        for (int i = 0; i < len; i++)
        {
            arr[i] = arrNbt[i];
        }
    }

    /**
     * Reads an int array from NBT into the provided int array.
     * The number of elements read is the minimum of the provided array's length and
     * the read byte array's length.
     * @param arr
     * @param nbt
     * @param tagName
     */
    public static void readIntArray(int[] arr, NBTTagCompound nbt, String tagName)
    {
        int[] arrNbt = nbt.getIntArray(tagName);
        final int len = Math.min(arr.length, arrNbt.length);

        for (int i = 0; i < len; i++)
        {
            arr[i] = arrNbt[i];
        }
    }

    /**
     * Reads an ItemStack from the given compound tag, including the Ender Utilities-specific custom stackSize.
     * @param tag
     * @return
     */
    @Nonnull
    public static ItemStack loadItemStackFromTag(@Nonnull NBTTagCompound tag)
    {
        ItemStack stack = new ItemStack(tag);

        if (tag.hasKey("ActualCount", Constants.NBT.TAG_INT))
        {
            stack.setCount(tag.getInteger("ActualCount"));
        }

        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    @Nonnull
    public static NBTTagCompound storeItemStackInTag(@Nonnull ItemStack stack, @Nonnull NBTTagCompound tag)
    {
        if (stack.isEmpty() == false)
        {
            stack.writeToNBT(tag);

            if (stack.getCount() > 127)
            {
                // Prevent overflow and negative stack sizes
                tag.setByte("Count", (byte) (stack.getCount() & 0x7F));
                tag.setInteger("ActualCount", stack.getCount());
            }
        }

        return tag;
    }

    /**
     * Reads the stored items from the provided NBTTagCompound, from a NBTTagList by the name <b>tagName</b>
     * and writes them to the provided list of ItemStacks <b>items</b>.<br>
     * <b>NOTE:</b> The list should be initialized to be large enough for all the stacks to be read!
     * @param tag
     * @param items
     * @param tagName
     */
    public static void readStoredItemsFromTag(@Nonnull NBTTagCompound nbt, NonNullList<ItemStack> items, @Nonnull String tagName)
    {
        if (nbt.hasKey(tagName, Constants.NBT.TAG_LIST) == false)
        {
            return;
        }

        NBTTagList nbtTagList = nbt.getTagList(tagName, Constants.NBT.TAG_COMPOUND);
        int num = nbtTagList.tagCount();
        int listSize = items.size();

        for (int i = 0; i < num; ++i)
        {
            NBTTagCompound tag = nbtTagList.getCompoundTagAt(i);
            int slotNum = tag.getShort("Slot");

            if (slotNum >= 0 && slotNum < listSize)
            {
                items.set(slotNum, loadItemStackFromTag(tag));
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
    @Nonnull
    public static NBTTagList createTagListForItems(NonNullList<ItemStack> items)
    {
        NBTTagList nbtTagList = new NBTTagList();
        final int invSlots = items.size();

        // Write all the ItemStacks into a TAG_List
        for (int slotNum = 0; slotNum < invSlots; slotNum++)
        {
            ItemStack stack = items.get(slotNum);

            if (stack.isEmpty() == false)
            {
                NBTTagCompound tag = storeItemStackInTag(stack, new NBTTagCompound());

                if (invSlots <= 127)
                {
                    tag.setByte("Slot", (byte) slotNum);
                }
                else
                {
                    tag.setShort("Slot", (short) slotNum);
                }

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
    @Nonnull
    public static NBTTagCompound writeItemsToTag(@Nonnull NBTTagCompound nbt, NonNullList<ItemStack> items,
            @Nonnull String tagName, boolean keepExtraSlots)
    {
        int invSlots = items.size();
        NBTTagList nbtTagList = createTagListForItems(items);

        if (keepExtraSlots && nbt.hasKey(tagName, Constants.NBT.TAG_LIST))
        {
            // Read the old items and append any existing items that are outside the current written slot range
            NBTTagList nbtTagListExisting = nbt.getTagList(tagName, Constants.NBT.TAG_COMPOUND);
            final int count = nbtTagListExisting.tagCount();

            for (int i = 0; i < count; i++)
            {
                NBTTagCompound tag = nbtTagListExisting.getCompoundTagAt(i);
                int slotNum = tag.getShort("Slot");

                if (slotNum >= invSlots)
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

    /**
     * Stores a cached snapshot of the current inventory in a compound tag <b>InvCache</b>.
     * It is meant for tooltip use in the ItemBlocks.
     * @param nbt
     * @return
     */
    public static NBTTagCompound storeCachedInventory(NBTTagCompound nbt, IItemHandler inv, int maxEntries)
    {
        NBTTagList list = new NBTTagList();
        int stacks = 0;
        long items = 0;
        final int size = inv.getSlots();

        for (int slot = 0; slot < size; slot++)
        {
            ItemStack stack = inv.getStackInSlot(slot);

            if (stack.isEmpty() == false)
            {
                if (stacks < maxEntries)
                {
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setString("dn", stack.getDisplayName());
                    tag.setInteger("c", stack.getCount());
                    list.appendTag(tag);
                }

                stacks++;
                items += stack.getCount();
            }
        }

        if (stacks > 0)
        {
            NBTTagCompound wrapper = new NBTTagCompound();
            wrapper.setTag("il", list);
            wrapper.setInteger("ts", stacks);
            wrapper.setLong("ti", items);
            nbt.setTag("InvCache", wrapper);
        }
        else
        {
            nbt.removeTag("InvCache");
        }

        return nbt;
    }

    /**
     * Adds ready formatted description of the stored items in a cached tag to the list provided.
     * NOTE: CLIENT-ONLY!
     * @param stack
     * @param lines
     * @param maxItemLines
     */
    public static void getCachedInventoryStrings(ItemStack stack, List<String> lines, int maxItemLines)
    {
        NBTTagCompound wrapper = getCompoundTag(stack, "InvCache", false);

        if (wrapper == null)
        {
            return;
        }

        String preWhite = TextFormatting.WHITE.toString();
        String rst = TextFormatting.RESET.toString() + TextFormatting.GRAY.toString();
        NBTTagList list = wrapper.getTagList("il", Constants.NBT.TAG_COMPOUND);
        final int totalStacks = wrapper.getInteger("ts");
        final int numLines = Math.min(list.tagCount(), maxItemLines);
        String countStr = AutoverseStringUtils.formatNumberWithKSeparators(wrapper.getLong("ti"));

        lines.add(I18n.format(Reference.MOD_ID + ".tooltip.item.stackcount", totalStacks, countStr));

        for (int i = 0; i < numLines; i++)
        {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            countStr = AutoverseStringUtils.formatNumberWithKSeparators(tag.getInteger("c"));
            lines.add(String.format("  %s%s%s %s", preWhite, countStr, rst, tag.getString("dn")));
        }

        if (totalStacks > maxItemLines)
        {
            lines.add(I18n.format(Reference.MOD_ID + ".tooltip.item.andmorestacksnotlisted", preWhite, totalStacks - maxItemLines, rst));
        }
    }

    /**
     * Returns the base display name appended with either the display name
     * and stack size of the stored item if there is only one, or the number of
     * stored stacks, if there are more than one stack.
     * @param stack
     * @param nameBase
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String getItemStackDisplayName(ItemStack stack, String nameBase)
    {
        NBTTagCompound wrapper = getCompoundTag(stack, "InvCache", false);

        if (wrapper == null)
        {
            return nameBase;
        }

        String preGree = TextFormatting.GREEN.toString();
        String rstWhite = TextFormatting.RESET.toString() + TextFormatting.WHITE.toString();
        NBTTagList list = wrapper.getTagList("il", Constants.NBT.TAG_COMPOUND);
        int totalStacks = wrapper.getInteger("ts");

        if (totalStacks == 1)
        {
            NBTTagCompound tag = list.getCompoundTagAt(0);
            String countStr = AutoverseStringUtils.formatNumber(tag.getInteger("c"), 9999, 4);
            nameBase = String.format("%s - %s%s%s (%s)", nameBase, preGree, tag.getString("dn"), rstWhite, countStr);
        }
        else if (totalStacks > 0)
        {
            nameBase = String.format("%s (%d %s)", nameBase, totalStacks,
                    net.minecraft.util.text.translation.I18n.translateToLocal(Reference.MOD_ID + ".tooltip.item.stacks"));
        }

        return nameBase;
    }
}
