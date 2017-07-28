package fi.dy.masa.autoverse.event.tasks;

import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class PlayerTaskApplySequence implements IPlayerTask
{
    private final int dimension;
    private int position;
    private int failCount;
    private int count;
    private final BlockPos pos;
    private final EnumFacing side;
    private final NonNullList<ItemStack> sequence;

    public PlayerTaskApplySequence(EntityPlayer player, BlockPos pos, EnumFacing side, NonNullList<ItemStack> sequence)
    {
        this.dimension = player.getEntityWorld().provider.getDimension();
        this.pos = pos;
        this.side = side;
        this.sequence = sequence;
    }

    @Override
    public void init()
    {
    }

    @Override
    public boolean canExecute(World world, EntityPlayer player)
    {
        return this.position < this.sequence.size() &&
               world.provider.getDimension() == this.dimension &&
               world.isBlockLoaded(this.pos);
    }

    @Override
    public boolean execute(World world, EntityPlayer player)
    {
        if (this.position >= this.sequence.size())
        {
            return true;
        }

        IItemHandler inv = this.getInventory(world);

        if (inv == null)
        {
            return true;
        }

        ItemStack stack = this.getItem(player);

        if (stack.isEmpty())
        {
            return true;
        }

        stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);

        if (stack.isEmpty())
        {
            this.count++;
            this.failCount = 0;

            // Finished
            if (++this.position >= this.sequence.size())
            {
                return true;
            }
        }
        // Failed to insert the item
        else
        {
            // Survival mode, return the item
            if (player.capabilities.isCreativeMode == false)
            {
                inv = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);

                // Failed to insert the item back to the player's inventory
                if (stack.isEmpty() == false)
                {
                    player.dropItem(stack, false);
                }
            }

            // 10 second fail timeout
            if (++this.failCount >= 50)
            {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private IItemHandler getInventory(World world)
    {
        TileEntity te = world.getTileEntity(this.pos);

        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.side))
        {
            return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.side);
        }

        return null;
    }

    private ItemStack getItem(EntityPlayer player)
    {
        if (player.capabilities.isCreativeMode)
        {
            ItemStack stack = this.sequence.get(this.position).copy();
            stack.setCount(1);
            return stack;
        }
        else
        {
            IItemHandler inv = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            return InventoryUtils.collectItemsFromInventory(inv, this.sequence.get(this.position), 1, true);
        }
    }

    @Override
    public void stop(EntityPlayer player)
    {
        // Success, applied all items
        if (this.position >= this.sequence.size())
        {
            player.sendStatusMessage(new TextComponentTranslation("autoverse.chat.wand.sequence_applied_success", Integer.valueOf(this.count)), true);
        }
        // Stopped before finishing the sequence
        else
        {
            player.sendStatusMessage(new TextComponentTranslation("autoverse.chat.wand.sequence_applied_failed",
                    Integer.valueOf(this.count), Integer.valueOf(this.sequence.size())), true);
        }
        //Autoverse.logger.info("PlayerTaskApplySequence exiting, applied {} items", this.count);
    }
}
