package fi.dy.masa.autoverse.item;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.client.HotKeys;
import fi.dy.masa.autoverse.client.HotKeys.EnumKey;
import fi.dy.masa.autoverse.event.tasks.PlayerTaskApplySequence;
import fi.dy.masa.autoverse.event.tasks.scheduler.PlayerTaskScheduler;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperPlacerProgrammable;
import fi.dy.masa.autoverse.item.base.IKeyBound;
import fi.dy.masa.autoverse.item.base.IStringInput;
import fi.dy.masa.autoverse.item.base.ItemAutoverse;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockPlacerProgrammable;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.NBTUtils;

public class ItemBlockPlacerConfigurator extends ItemAutoverse implements IKeyBound, IStringInput
{
    public static final int MAX_SEQUENCES = 16;
    private static final String TAG_SELECTION = "Selection";

    public ItemBlockPlacerConfigurator()
    {
        super(ReferenceNames.NAME_ITEM_BLOCK_PLACER_CONFIGURATOR);

        this.setMaxStackSize(1);
        this.setMaxDamage(0);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking())
        {
            TileEntityBlockPlacerProgrammable te = BlockAutoverse.getTileEntitySafely(world, pos, TileEntityBlockPlacerProgrammable.class);

            if (te != null)
            {
                ItemStack stack = player.getHeldItem(hand);

                if (world.isRemote == false)
                {
                    this.applyProperties(stack, pos, facing, player, te);
                }

                return EnumActionResult.SUCCESS;
            }
        }

        return EnumActionResult.PASS;
    }

    private boolean storeProperties(ItemStack stack, EntityPlayer player)
    {
        World world = player.getEntityWorld();
        RayTraceResult trace = EntityUtils.getRayTraceFromEntity(world, player, false);

        if (trace.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            BlockPos pos = trace.getBlockPos();
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            ItemStack stackBlock = block.getPickBlock(state, trace, world, pos, player);
            String blockName = stackBlock.isEmpty() == false ? stackBlock.getDisplayName() : block.getRegistryName().toString();
            TileEntityAutoverse te = BlockAutoverse.getTileEntitySafely(world, pos, TileEntityAutoverse.class);
            boolean success = false;

            if (te != null)
            {
                success = this.getAutoverseTilePropertyValues(stack, te);
            }
            else
            {
                success = this.getBlockPropertyValues(stack, state);
            }

            if (success)
            {
                NBTTagCompound tag = this.getTagForSelectedProperties(stack, true);
                tag.setString("BlockName", blockName);
                world.playSound(null, player.getPosition(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.MASTER, 0.8f, 1.0f);
                player.sendStatusMessage(new TextComponentTranslation("autoverse.chat.configurator.properties_stored"), true);
                return true;
            }
        }

        return false;
    }

    private boolean getAutoverseTilePropertyValues(ItemStack stack, TileEntityAutoverse te)
    {
        int[] propValues = te.getProperties();

        if (propValues.length > 0)
        {
            NBTTagCompound tagSel = this.getTagForSelectedProperties(stack, true);
            tagSel.setIntArray("Props", propValues);
            return true;
        }

        return false;
    }

    private boolean getBlockPropertyValues(ItemStack stack, IBlockState state)
    {
        List<String> propNames = TileEntityBlockPlacerProgrammable.getConfigurableBlockPropertiesSorted(state);
        int length = Math.min(propNames.size(), 4);
        int[] propValues = new int[length];
        Arrays.fill(propValues, -1);

        for (int i = 0; i < propNames.size() && i < 4; i++)
        {
            String propName = propNames.get(i);

            for (Entry <IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet())
            {
                IProperty<?> property = entry.getKey();

                if (propName.equals(property.getName()))
                {
                    if (property instanceof PropertyDirection)
                    {
                        propValues[i] = state.getValue((PropertyDirection) property).getIndex();
                        break;
                    }
                    else if (property instanceof PropertyInteger)
                    {
                        propValues[i] = state.getValue((PropertyInteger) property);
                        break;
                    }
                }
            }
        }

        NBTTagCompound tagSel = this.getTagForSelectedProperties(stack, true);
        tagSel.setIntArray("Props", propValues);
        return true;
    }

    private void applyProperties(ItemStack stack, BlockPos pos, EnumFacing side, EntityPlayer player, TileEntityBlockPlacerProgrammable te)
    {
        if (this.hasStoredPropertiesSelected(stack) == false)
        {
            player.sendStatusMessage(new TextComponentTranslation("autoverse.chat.configurator.error.no_stored_properties_selected"), true);
            return;
        }

        NBTTagCompound tag = this.getTagForSelectedProperties(stack, false);
        int[] values = tag.getIntArray("Props");
        ItemHandlerWrapperPlacerProgrammable placer = te.getPlacerHandler();

        if (placer.canConfigureProperties())
        {
            ItemStack endMarker = placer.getEndMarkerInventory().getStackInSlot(0);
            ItemStack highBitMarker = placer.getHighBitMarkerInventory().getStackInSlot(0);
            ItemStack lowBitMarker = this.getLowBitMarker(player, endMarker, highBitMarker);

            if (endMarker.isEmpty() == false && highBitMarker.isEmpty() == false && lowBitMarker.isEmpty() == false)
            {
                NonNullList<ItemStack> sequence = this.generateSequence(values, endMarker, highBitMarker, lowBitMarker);

                // Don't allow multiple tasks per player at once
                if (sequence.size() > 0 && PlayerTaskScheduler.getInstance().hasTask(player, PlayerTaskApplySequence.class) == false)
                {
                    PlayerTaskApplySequence task = new PlayerTaskApplySequence(player, pos, side, sequence);
                    PlayerTaskScheduler.getInstance().addTask(player, task, 4);
                    player.getEntityWorld().playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.MASTER, 0.6f, 1.0f);
                }
            }
            else
            {
                player.sendStatusMessage(new TextComponentTranslation("autoverse.chat.configurator.error.items_not_found"), true);
            }
        }
        else
        {
            player.sendStatusMessage(new TextComponentTranslation("autoverse.chat.configurator.error.wrong_state"), true);
        }
    }

    private NonNullList<ItemStack> generateSequence(int[] values, ItemStack endMarker, ItemStack highBitMarker, ItemStack lowBitMarker)
    {
        NonNullList<ItemStack> sequence = NonNullList.create();

        for (int i = 0; i < ItemHandlerWrapperPlacerProgrammable.NUM_PROPERTIES; i++)
        {
            final int value = i < values.length ? values[i] : -1;

            if (value == -1)
            {
                sequence.add(endMarker.copy());
            }
            else
            {
                int bitCount = this.getBitCount(value);

                for (int loop = 0; loop < bitCount; loop++)
                {
                    if ((value & (1 << loop)) != 0)
                    {
                        sequence.add(highBitMarker.copy());
                    }
                    else
                    {
                        sequence.add(lowBitMarker.copy());
                    }
                }

                if (bitCount < 8)
                {
                    sequence.add(endMarker.copy());
                }
            }
        }

        return sequence;
    }

    private int getBitCount(int value)
    {
        // Capped at 8, because that's the properties' max sequence length
        return Math.min(32 - Integer.numberOfLeadingZeros(value), 8);
    }

    private ItemStack getLowBitMarker(EntityPlayer player, ItemStack endMarker, ItemStack highBitMarker)
    {
        final int length = player.inventory.getSizeInventory();

        for (int slot = 0; slot < length; slot++)
        {
            ItemStack stack = player.inventory.getStackInSlot(slot);

            if (stack.isEmpty() == false && stack.getMaxStackSize() > 1 &&
                InventoryUtils.areItemStacksEqual(stack, endMarker) == false &&
                InventoryUtils.areItemStacksEqual(stack, highBitMarker) == false)
            {
                return stack.copy();
            }
        }

        return ItemStack.EMPTY;
    }

    private boolean hasStoredPropertiesSelected(ItemStack stack)
    {
        NBTTagCompound nbt = this.getTagForSelectedProperties(stack, false);
        return nbt != null && nbt.hasKey("Props", Constants.NBT.TAG_INT_ARRAY);
    }

    private NBTTagCompound getTagForSelectedProperties(ItemStack stack, boolean create)
    {
        int selection = NBTUtils.getByte(stack, null, TAG_SELECTION);
        return NBTUtils.getCompoundTag(stack, "Selection_" + selection, create);
    }

    private void setPropertiesName(ItemStack stack, String name)
    {
        if (StringUtils.isBlank(name))
        {
            this.getTagForSelectedProperties(stack, true).removeTag("Name");
        }
        else
        {
            this.getTagForSelectedProperties(stack, true).setString("Name", name);
        }
    }

    private String getSelectedPropertiesName(ItemStack stack)
    {
        NBTTagCompound nbt = this.getTagForSelectedProperties(stack, false);

        if (nbt != null)
        {
            if (nbt.hasKey("Name", Constants.NBT.TAG_STRING))
            {
                return nbt.getString("Name");
            }

            if (nbt.hasKey("BlockName", Constants.NBT.TAG_STRING))
            {
                return nbt.getString("BlockName");
            }
        }

        return "???";
    }

    @Override
    public String getCurrentString(EntityPlayer player, ItemStack stack)
    {
        return this.getSelectedPropertiesName(stack);
    }

    @Override
    public void handleString(EntityPlayer player, ItemStack stack, String text)
    {
        if (stack.isEmpty() == false)
        {
            this.setPropertiesName(stack, text);
        }
    }

    @Override
    public void doKeyBindingAction(EntityPlayer player, ItemStack stack, int key)
    {
        // Alt + (Shift) + Scroll/Toggle: Cycle the selection
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_ALT) ||
            EnumKey.TOGGLE.matches(key, HotKeys.MOD_ALT, HotKeys.MOD_SHIFT))
        {
            NBTUtils.cycleByteValue(stack, null, TAG_SELECTION, MAX_SEQUENCES - 1,
                    EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key));
        }
        // Shift + middle click: Store a sequence
        else if (EnumKey.MIDDLE_CLICK.matches(key, HotKeys.MOD_SHIFT))
        {
            this.storeProperties(stack, player);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        String base = super.getItemStackDisplayName(stack);
        int selection = NBTUtils.getByte(stack, null, TAG_SELECTION);
        return String.format("%s - %d / %d - %s", base, selection + 1, MAX_SEQUENCES, this.getSelectedPropertiesName(stack));
    }
}
