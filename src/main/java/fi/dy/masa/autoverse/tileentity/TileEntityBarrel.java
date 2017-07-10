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
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiBarrel;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBarrel;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperCreative;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class TileEntityBarrel extends TileEntityAutoverseInventory
{
    private boolean isPulsed;
    private int tier;
    private boolean isCreative;
    private BlockPos posBottom = BlockPos.ORIGIN;

    public TileEntityBarrel()
    {
        this(false);
    }

    public TileEntityBarrel(boolean isPulsed)
    {
        super(ReferenceNames.NAME_BLOCK_BARREL, true);

        this.isPulsed = isPulsed;
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, 1, 64, true, "Items", this);
        this.createInventoryWrapper();
    }

    private void createInventoryWrapper()
    {
        // The Pulsed barrel is not redstone-lockable
        if (this.isPulsed)
        {
            this.itemHandlerExternal = new ItemHandlerWrapperCreative(this.itemHandlerBase, this);
        }
        else
        {
            this.itemHandlerExternal = new ItemHandlerWrapperBarrel(this.itemHandlerBase, this);
        }
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        if (propId == 0)
        {
            this.setTier(value);
            return true;
        }

        return false;
    }

    @Override
    public void setPlacementProperties(World world, BlockPos pos, ItemStack stack, NBTTagCompound tag)
    {
        if (tag.hasKey("barrel.tier", Constants.NBT.TAG_BYTE))
        {
            this.setTier(tag.getByte("barrel.tier"));
        }
    }

    private void setTier(int tier)
    {
        this.tier = MathHelper.clamp(tier, 0, 15);
        this.itemHandlerBase.setStackLimit(this.getMaxStackSize());
    }

    public int getTier()
    {
        return this.tier;
    }

    private void setIsPulsed(boolean isPulsed)
    {
        this.isPulsed = isPulsed;
        this.createInventoryWrapper();
    }

    public boolean isPulsed()
    {
        return this.isPulsed;
    }

    @Override
    public boolean isCreative()
    {
        return this.isCreative;
    }

    public void setIsCreative(boolean isCreative)
    {
        this.isCreative = isCreative;
    }

    public int getMaxStackSize()
    {
        return (int) Math.pow(2, this.tier);
    }

    @Override
    public void onLoad()
    {
        this.posBottom = this.getPos().down();
        this.createInventoryWrapper();
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
            this.pushItemsToAdjacentInventory(this.itemHandlerExternal, 0, this.posBottom, EnumFacing.UP, true);
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
        nbt.setBoolean("Creative", this.isCreative);
        nbt.setByte("Tier", (byte)this.tier);

        return nbt;
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        this.isCreative = nbt.getBoolean("Creative");
        this.setTier(nbt.getByte("Tier"));

        super.readFromNBTCustom(nbt);

        // (Re-)create/use the correct inventory wrapper based on whether the barrel is Pulsed type
        this.setIsPulsed(nbt.getBoolean("Pulsed"));
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag = super.getUpdatePacketTag(tag);

        byte mask = (byte) this.tier;
        mask |= (this.isPulsed ? 0x80 : 0);
        mask |= (this.isCreative() ? 0x40 : 0);
        tag.setByte("d", mask);

        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        super.handleUpdateTag(tag);

        int data = tag.getByte("d");
        this.setIsPulsed((data & 0x80) != 0);
        this.setIsCreative((data & 0x40) != 0);

        data = data & 0xF;

        if (this.tier != data)
        {
            this.setTier(data);
            this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
        }
    }

    @Override
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        // Change Barrel tier
        if (action == 0)
        {
            this.setTier(this.tier + element);
            this.markDirty();
            this.notifyBlockUpdate(this.getPos());
        }
        // Toggle creative mode
        else if (action == 1)
        {
            if (player.capabilities.isCreativeMode)
            {
                this.isCreative = ! this.isCreative;
                this.markDirty();
            }
        }
    }

    private class ItemHandlerWrapperBarrel extends ItemHandlerWrapperCreative
    {
        private final TileEntityBarrel te;

        public ItemHandlerWrapperBarrel(IItemHandler baseHandler, TileEntityBarrel te)
        {
            super(baseHandler, te);

            this.te = te;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (this.te.getWorld().isBlockPowered(this.te.getPos()))
            {
                return stack;
            }

            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (this.te.getWorld().isBlockPowered(this.te.getPos()))
            {
                return ItemStack.EMPTY;
            }

            return super.extractItem(slot, amount, simulate);
        }
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return new ContainerBarrel(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiBarrel(this.getContainer(player), this);
    }
}
