package fi.dy.masa.autoverse.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
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
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.client.HotKeys;
import fi.dy.masa.autoverse.client.HotKeys.EnumKey;
import fi.dy.masa.autoverse.event.tasks.PlayerTaskApplySequence;
import fi.dy.masa.autoverse.event.tasks.scheduler.PlayerTaskScheduler;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequenceBase;
import fi.dy.masa.autoverse.item.base.IKeyBound;
import fi.dy.masa.autoverse.item.base.IStringInput;
import fi.dy.masa.autoverse.item.base.ItemAutoverse;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.NBTUtils;

public class ItemWand extends ItemAutoverse implements IKeyBound, IStringInput
{
    public static final int MAX_SEQUENCES = 16;
    private static final String TAG_SELECTION = "Selection";

    public ItemWand()
    {
        super(ReferenceNames.NAME_ITEM_WAND);

        this.setMaxStackSize(1);
        this.setMaxDamage(0);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking())
        {
            TileEntity te = BlockAutoverse.getTileEntitySafely(world, pos, TileEntity.class);

            if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing))
            {
                ItemStack stack = player.getHeldItem(hand);

                if (this.hasStoredSequenceSelected(stack))
                {
                    if (world.isRemote == false)
                    {
                        this.applySequence(stack, pos, facing, player);
                    }

                    return EnumActionResult.SUCCESS;
                }
            }
        }

        return EnumActionResult.PASS;
    }

    private boolean hasStoredSequenceSelected(ItemStack stack)
    {
        NBTTagCompound nbt = this.getTagForSelectedSequence(stack, false);
        return nbt != null && nbt.hasKey("Items", Constants.NBT.TAG_LIST);
    }

    private NBTTagCompound getTagForSelectedSequence(ItemStack stack, boolean create)
    {
        int selection = NBTUtils.getByte(stack, null, TAG_SELECTION);
        return NBTUtils.getCompoundTag(stack, "Sequence_" + selection, create);
    }

    private void setSequenceName(ItemStack stack, String name)
    {
        this.getTagForSelectedSequence(stack, true).setString("Name", name);
    }

    private String getSelectedSequenceName(ItemStack stack)
    {
        NBTTagCompound nbt = this.getTagForSelectedSequence(stack, false);
        return nbt != null && nbt.hasKey("Name", Constants.NBT.TAG_STRING) ? nbt.getString("Name") : "Unnamed";
    }

    private boolean applySequence(ItemStack stack, BlockPos pos, EnumFacing side, EntityPlayer player)
    {
        NBTTagCompound nbt = this.getTagForSelectedSequence(stack, false);

        if (nbt != null)
        {
            NonNullList<ItemStack> sequence = NBTUtils.readStoredItemsFromTag(nbt, "Items");

            // Don't allow multiple tasks per player at once
            if (sequence.size() > 0 && PlayerTaskScheduler.getInstance().hasTask(player, PlayerTaskApplySequence.class) == false)
            {
                PlayerTaskApplySequence task = new PlayerTaskApplySequence(player, pos, side, sequence);
                PlayerTaskScheduler.getInstance().addTask(player, task, 4);
            }
        }

        return false;
    }

    private boolean storeSequence(ItemStack stack, EntityPlayer player)
    {
        World world = player.getEntityWorld();
        RayTraceResult trace = EntityUtils.getRayTraceFromEntity(world, player, false);

        if (trace.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            TileEntity te = world.getTileEntity(trace.getBlockPos());

            if (te != null)
            {
                if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, trace.sideHit))
                {
                    IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, trace.sideHit);
                    NonNullList<ItemStack> items = null;

                    if (inv instanceof ItemHandlerWrapperSequenceBase)
                    {
                        items = ((ItemHandlerWrapperSequenceBase) inv).getFullProgrammingSequence();
                    }
                    else if (te instanceof TileEntityAutoverseInventory)
                    {
                        items = this.createInventorySnapshot(((TileEntityAutoverseInventory) te).getBaseItemHandler());
                    }
                    else if (inv != null)
                    {
                        items = this.createInventorySnapshot(inv);
                    }

                    if (items != null)
                    {
                        NBTUtils.writeItemsToTag(this.getTagForSelectedSequence(stack, true), items, "Items", false);
                        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.MASTER, 0.2f, 1.8f);
                        player.sendStatusMessage(new TextComponentTranslation("autoverse.chat.wand.sequence_stored"), true);
                        return true;
                    }
                }
            }
            // Shift + middle clicked on a non-TileEntity - remove the selected sequence
            else
            {
                int selection = NBTUtils.getByte(stack, null, TAG_SELECTION);
                NBTTagCompound nbt = stack.getTagCompound();

                if (nbt != null)
                {
                    nbt.removeTag("Sequence_" + selection);

                    if (nbt.isEmpty())
                    {
                        stack.setTagCompound(null);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Creates a copy of the inventory and returns it in a new NonNullList.<br>
     * NOTE: Only stacks without NBT data are included on the list!
     * @param inv
     * @return a NonNullList containing a copy of the stacks
     */
    private NonNullList<ItemStack> createInventorySnapshot(IItemHandler inv)
    {
        final int invSize = inv.getSlots();
        NonNullList<ItemStack> items = NonNullList.create();

        for (int slot = 0; slot < invSize; slot++)
        {
            ItemStack stack = inv.getStackInSlot(slot);

            // Only allow items that don't have NBT data, to try to prevent NBT size explosions
            if (stack.isEmpty() == false && stack.getTagCompound() == null)
            {
                items.add(stack.copy());
            }
        }

        return items;
    }

    @Override
    public String getCurrentString(EntityPlayer player, ItemStack stack)
    {
        return this.getSelectedSequenceName(stack);
    }

    @Override
    public void handleString(EntityPlayer player, ItemStack stack, String text)
    {
        if (stack.isEmpty() == false)
        {
            this.setSequenceName(stack, text);
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
            this.storeSequence(stack, player);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        String base = super.getItemStackDisplayName(stack);
        int selection = NBTUtils.getByte(stack, null, TAG_SELECTION);
        return String.format("%s - %d / %d - %s", base, selection + 1, MAX_SEQUENCES, this.getSelectedSequenceName(stack));
    }
}
