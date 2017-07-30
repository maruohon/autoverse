package fi.dy.masa.autoverse.item.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.client.HotKeys;
import fi.dy.masa.autoverse.client.HotKeys.EnumKey;
import fi.dy.masa.autoverse.item.base.IKeyBound;
import fi.dy.masa.autoverse.item.base.ItemAutoverse;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.ItemType;
import fi.dy.masa.autoverse.util.PlacementProperties;

public class ItemBlockAutoverse extends ItemBlock implements IKeyBound
{
    protected String[] blockNames;
    protected String[] tooltipNames;
    private final HashMap<Integer, PlacementProperty> placementProperties = new HashMap<Integer, PlacementProperty>();

    public static class PlacementProperty
    {
        private boolean isNBTSensitive;
        private List<Pair<String, Integer>> propertyTypes = new ArrayList<Pair<String, Integer>>();
        private List<Pair<Integer, Integer>> propertyValueRange = new ArrayList<Pair<Integer, Integer>>();
        private Map<String, String[]> propertyValueNames = new HashMap<String, String[]>();

        public boolean hasPlacementProperties()
        {
            return this.propertyTypes.isEmpty() == false;
        }

        public boolean isNBTSensitive()
        {
            return this.isNBTSensitive;
        }

        public void setIsNBTSensitive(boolean checkNBT)
        {
            this.isNBTSensitive = checkNBT;
        }

        public void addProperty(String key, int type, int minValue, int maxValue)
        {
            this.propertyTypes.add(Pair.of(key, type));
            this.propertyValueRange.add(Pair.of(minValue, maxValue));
        }

        public void addValueNames(String key, String[] names)
        {
            this.propertyValueNames.put(key, names);
        }

        @Nullable
        public Pair<String, Integer> getProperty(int index)
        {
            return index >= 0 && index < this.propertyTypes.size() ? this.propertyTypes.get(index) : null;
        }

        @Nullable
        public Pair<Integer, Integer> getPropertyValueRange(int index)
        {
            return index >= 0 && index < this.propertyValueRange.size() ? this.propertyValueRange.get(index) : null;
        }

        @Nullable
        public String getPropertyValueName(String key, int index)
        {
            String[] names = this.propertyValueNames.get(key);

            if (names != null && index >= 0 && index < names.length)
            {
                return names[index];
            }

            return null;
        }

        public int getPropertyCount()
        {
            return this.propertyTypes.size();
        }
    }

    public ItemBlockAutoverse(BlockAutoverse block)
    {
        super(block);

        this.setHasSubtypes(true);
        this.setMaxDamage(0);

        this.setBlockNames(block.getUnlocalizedNames());
        this.setTooltipNames(block.getTooltipNames());
    }

    public void setBlockNames(String[] names)
    {
        this.blockNames = names;
    }

    public void setTooltipNames(String[] names)
    {
        this.tooltipNames = names;
    }

    public boolean hasPlacementProperty(ItemStack stack)
    {
        return this.placementProperties.containsKey(OreDictionary.WILDCARD_VALUE) ||
               this.placementProperties.containsKey(stack.getMetadata());
    }

    @Nullable
    public PlacementProperty getPlacementProperty(ItemStack stack)
    {
        PlacementProperty pp = this.placementProperties.get(stack.getMetadata());

        if (pp == null)
        {
            pp = this.placementProperties.get(OreDictionary.WILDCARD_VALUE);
        }

        return pp;
    }

    @Nonnull
    public PlacementProperty getOrCreatePlacementProperty(int stackMeta)
    {
        PlacementProperty pp = this.placementProperties.get(stackMeta);

        if (pp == null)
        {
            pp = new PlacementProperty();
            this.placementProperties.put(stackMeta, pp);
        }

        return pp;
    }

    public void addPlacementProperty(int stackMeta, String key, int type, int minValue, int maxValue)
    {
        PlacementProperty pp = this.getOrCreatePlacementProperty(stackMeta);
        pp.propertyTypes.add(Pair.of(key, type));
        pp.propertyValueRange.add(Pair.of(minValue, maxValue));
    }

    public void addPlacementPropertyValueNames(int stackMeta, String key, String[] names)
    {
        PlacementProperty pp = this.getOrCreatePlacementProperty(stackMeta);
        pp.propertyValueNames.put(key, names);
    }

    @Override
    public void doKeyBindingAction(EntityPlayer player, ItemStack stack, int key)
    {
        ItemBlockAutoverse item = (ItemBlockAutoverse) stack.getItem();

        if (item.hasPlacementProperty(stack) && player instanceof EntityPlayerMP)
        {
            PlacementProperty pp = this.getPlacementProperty(stack);
            ItemType type = new ItemType(stack, pp.isNBTSensitive());
            int index = PlacementProperties.getInstance().getPropertyIndex(player.getUniqueID(), type);

            if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE) || EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT))
            {
                index += EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT) ? -1 : 1;
                if (index < 0) { index = Math.max(0, pp.getPropertyCount() - 1); }
                else if (index >= pp.getPropertyCount()) { index = 0; }

                PlacementProperties.getInstance().setPropertyIndex(player.getUniqueID(), type, index);
                PlacementProperties.getInstance().syncCurrentlyHeldItemDataForPlayer((EntityPlayerMP) player, stack);
            }
            else
            {
                Pair<String, Integer> pair = pp.getProperty(index);
                Pair<Integer, Integer> range = pp.getPropertyValueRange(index);
                int minValue = range != null ? range.getLeft() : 0;
                int maxValue = range != null ? range.getRight() : 1;

                if (pair != null && EnumKey.getBaseKey(key) == EnumKey.SCROLL.getKeyCode())
                {
                    int change = EnumKey.keypressActionIsReversed(key) ? -1 : 1;

                    if (EnumKey.keypressContainsControl(key)) { change *= 10;  }
                    if (EnumKey.keypressContainsAlt(key))     { change *= 100; }

                    int value = PlacementProperties.getInstance().getPropertyValue(player.getUniqueID(), type, pair.getLeft(), pair.getRight());
                    value += change;

                    if (value < minValue) { value = maxValue; }
                    if (value > maxValue) { value = minValue; }

                    PlacementProperties.getInstance().setPropertyValue(player.getUniqueID(), type, pair.getLeft(), pair.getRight(), value);
                    PlacementProperties.getInstance().syncCurrentlyHeldItemDataForPlayer((EntityPlayerMP) player, stack);
                }
            }
        }
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        ItemStack stack = player.getHeldItem(hand);

        if (this.hasPlacementProperty(stack))
        {
            ItemType type = new ItemType(stack, this.getPlacementProperty(stack).isNBTSensitive());
            stack = stack.copy();

            EnumActionResult result = super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);

            if (result == EnumActionResult.SUCCESS)
            {
                NBTTagCompound tag = PlacementProperties.getInstance().getPropertyTag(player.getUniqueID(), type);

                if (tag != null)
                {
                    Block block = worldIn.getBlockState(pos).getBlock();

                    if (block.isReplaceable(worldIn, pos) == false)
                    {
                        pos = pos.offset(facing);
                        block = worldIn.getBlockState(pos).getBlock();
                    }

                    if (block instanceof BlockAutoverse)
                    {
                        ((BlockAutoverse) block).setPlacementProperties(worldIn, pos, stack, tag);
                    }
                }
            }

            return result;
        }
        else
        {
            return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
        }
    }

    @Override
    public int getMetadata(int meta)
    {
        return meta;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        if (this.blockNames != null && stack.getMetadata() < this.blockNames.length)
        {
            return "tile." + ReferenceNames.getDotPrefixedName(this.blockNames[stack.getMetadata()]);
        }

        return super.getUnlocalizedName(stack);
    }

    public String getTooltipName(ItemStack stack)
    {
        if (this.tooltipNames != null)
        {
            if (stack.getMetadata() < this.tooltipNames.length)
            {
                return "tile." + ReferenceNames.getDotPrefixedName(this.tooltipNames[stack.getMetadata()]);
            }
            // Some blocks may have a common tooltip for all different states/meta values,
            // by only including one entry in the array
            else if (this.tooltipNames.length == 1)
            {
                return "tile." + ReferenceNames.getDotPrefixedName(this.tooltipNames[0]);
            }
        }

        return this.getUnlocalizedName(stack);
    }

    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> list, boolean advancedTooltips, boolean verbose)
    {
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag advanced)
    {
        ArrayList<String> tmpList = new ArrayList<String>();
        boolean verbose = Autoverse.proxy.isShiftKeyDown();

        // "Fresh" items without NBT data: display the tips before the usual tooltip data
        if (stack.getTagCompound() == null)
        {
            this.addTooltips(stack, tmpList, verbose);

            if (verbose == false && tmpList.size() > 2)
            {
                list.add(I18n.format(Reference.MOD_ID + ".tooltip.item.holdshiftfordescription"));
            }
            else
            {
                list.addAll(tmpList);
            }
        }

        tmpList.clear();

        boolean isAdvanced = advanced == ITooltipFlag.TooltipFlags.ADVANCED;
        EntityPlayer player = Autoverse.proxy.getClientPlayer();

        this.addTooltipLines(stack, player, tmpList, isAdvanced, true);

        // If we want the compact version of the tooltip, and the compact list has more than 2 lines, only show the first line
        // plus the "Hold Shift for more" tooltip.
        if (verbose == false && tmpList.size() > 2)
        {
            tmpList.clear();
            this.addTooltipLines(stack, player, tmpList, isAdvanced, false);

            if (tmpList.size() > 0)
            {
                list.add(tmpList.get(0));
            }

            list.add(I18n.format(Reference.MOD_ID + ".tooltip.item.holdshift"));
        }
        else
        {
            list.addAll(tmpList);
        }
    }

    public void addTooltips(ItemStack stack, List<String> list, boolean verbose)
    {
        ItemAutoverse.addTranslatedTooltip(this.getTooltipName(stack) + ".tooltips", list, verbose);

        List<String> tmp = new ArrayList<String>();

        // Translation found
        if (ItemAutoverse.addTranslatedTooltip(this.getTooltipName(stack) + ".config_props", tmp, verbose))
        {
            ItemAutoverse.addTranslatedTooltip("autoverse.tooltip.config_props_title", list, verbose);
            list.addAll(tmp);
        }

        if (this.hasPlacementProperty(stack))
        {
            ItemAutoverse.addTranslatedTooltip(Reference.MOD_ID + ".tooltip.placementproperties.tooltips", list, verbose);
        }
    }

    @Override
    public boolean isFull3D()
    {
        return true;
    }

    /*
    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState)
    {
        if (world.setBlockState(pos, newState, 3) == false)
        {
            return false;
        }

        IBlockState state = world.getBlockState(pos);

        if (state.getBlock() == this.block)
        {
            setTileEntityNBT(world, player, pos, stack);

            if (this.block instanceof BlockAutoverseTileEntity)
            {
                ((BlockAutoverseTileEntity) this.block).onBlockPlacedBy(world, pos, side, state, player, stack);
            }
            else
            {
                this.block.onBlockPlacedBy(world, pos, state, player, stack);
            }
        }

        return true;
    }
    */
}
