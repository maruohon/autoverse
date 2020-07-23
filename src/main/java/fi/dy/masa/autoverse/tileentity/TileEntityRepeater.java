package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityRepeater extends TileEntityRepeaterBase
{
    private int delay = 2;

    public TileEntityRepeater()
    {
        super(ReferenceNames.NAME_BLOCK_REPEATER);
    }

    public int getDelay()
    {
        return this.delay;
    }

    public void setDelay(int delay)
    {
        this.delay = MathHelper.clamp(delay, 1, 20);
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        if (propId == 1)
        {
            this.setDelay(value);
            return true;
        }

        return super.applyProperty(propId, value);
    }

    @Override
    public int[] getProperties()
    {
        int[] values = super.getProperties();

        values[1] = this.delay;

        return values;
    }

    @Override
    public void setPlacementProperties(World world, BlockPos pos, ItemStack stack, NBTTagCompound tag)
    {
        super.setPlacementProperties(world, pos, stack, tag);

        if (tag.hasKey("repeater.delay", Constants.NBT.TAG_BYTE))
        {
            this.setDelay(((int) tag.getByte("repeater.delay")) & 0xFF);
        }
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, EnumFacing side, EntityPlayer player, EnumHand hand)
    {
        if (player.isSneaking() == false)
        {
            this.changeDelay(1);
            return true;
        }

        return super.onRightClickBlock(world, pos, side, player, hand);
    }

    @Override
    public boolean onLeftClickBlock(World world, BlockPos pos, EnumFacing side, EntityPlayer player)
    {
        if (player.isSneaking())
        {
            this.changeDelay(-1);
            return true;
        }

        return super.onLeftClickBlock(world, pos, side, player);
    }

    protected void changeDelay(int amount)
    {
        int delay = this.delay + amount;

        if (delay <= 0)
        {
            delay = 20;
        }
        else if (delay > 20)
        {
            delay = 0;
        }

        this.setDelay(delay);
        this.notifyBlockUpdate(this.getPos());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag = super.getUpdatePacketTag(tag);

        tag.setByte("de", (byte) this.delay);

        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.setDelay(tag.getByte("de"));

        super.handleUpdateTag(tag);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.setDelay(nbt.getByte("Delay"));
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt.setByte("Delay", (byte) this.delay);

        return super.writeToNBTCustom(nbt);
    }
}
