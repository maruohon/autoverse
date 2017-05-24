package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiBarrel;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBarrel;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class TileEntityBarrel extends TileEntityAutoverseInventory
{
    protected boolean isPulsed;
    protected int tier;
    private BlockPos posBottom;

    public TileEntityBarrel()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BARREL);
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, 1, this.getMaxStackSize(), true, "Items", this);
        this.itemHandlerExternal = new ItemHandlerWrapperExternal(this.itemHandlerBase);
    }

    public void setTier(int tier)
    {
        this.tier = MathHelper.clamp(tier, 0, 15);
        this.initInventories();
    }

    public int getTier()
    {
        return this.tier;
    }

    public void setIsPulsed(boolean isPulsed)
    {
        this.isPulsed = isPulsed;
    }

    public boolean isPulsed()
    {
        return this.isPulsed;
    }

    public int getMaxStackSize()
    {
        return (int) Math.pow(2, this.tier);
    }

    @Override
    public void onLoad()
    {
        this.posBottom = this.getPos().down();
    }

    @Override
    protected void onRedstoneChange(boolean state)
    {
        if (this.isPulsed && state)
        {
            this.scheduleBlockUpdate(1, false);
        }
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        if (this.isPulsed)
        {
            this.pushItemsToAdjacentInventory(this.itemHandlerBase, 0, this.posBottom, EnumFacing.UP, true);
        }
    }

    @Override
    protected Vec3d getSpawnedItemPosition()
    {
        return this.getSpawnedItemPosition(EnumFacing.DOWN);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setBoolean("Pulsed", this.isPulsed);
        nbt.setByte("Tier", (byte)this.tier);

        return nbt;
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        this.isPulsed = nbt.getBoolean("Pulsed");
        this.setTier(nbt.getByte("Tier"));

        super.readFromNBTCustom(nbt);
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag = super.getUpdatePacketTag(tag);
        tag.setByte("d", (byte)(this.tier | (this.isPulsed ? 0x10 : 0)));
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        super.handleUpdateTag(tag);

        byte data = tag.getByte("d");
        this.setTier(data & 0xF);
        this.setIsPulsed(data >= 16);
    }

    private class ItemHandlerWrapperExternal implements IItemHandler
    {
        private final IItemHandler parent;

        public ItemHandlerWrapperExternal(IItemHandler parent)
        {
            this.parent = parent;
        }

        @Override
        public int getSlots()
        {
            return this.parent.getSlots();
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return this.parent.getSlotLimit(slot);
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return this.parent.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (TileEntityBarrel.this.getWorld().isBlockPowered(TileEntityBarrel.this.getPos()))
            {
                return stack;
            }

            return this.parent.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (TileEntityBarrel.this.getWorld().isBlockPowered(TileEntityBarrel.this.getPos()))
            {
                return ItemStack.EMPTY;
            }

            return this.parent.extractItem(slot, amount, simulate);
        }
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return new ContainerBarrel(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiBarrel(this.getContainer(player), this);
    }
}
