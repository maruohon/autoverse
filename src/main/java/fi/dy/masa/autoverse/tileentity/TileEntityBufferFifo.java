package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiBufferFifo;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperSelective;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperSelectiveModifiable;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBufferFifo;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityBufferFifo extends TileEntityAutoverseInventory
{
    public static final int NUM_SLOTS = 26; // FIXME debug: change back to 117

    protected int insertSlot;
    protected int extractSlot;
    protected EnumFacing facingOpposite;
    protected BlockPos posFront;
    protected BlockPos posBack;

    public TileEntityBufferFifo()
    {
        this(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);
    }

    public TileEntityBufferFifo(String name)
    {
        super(name);

        this.facingOpposite = EnumFacing.DOWN;
        this.posFront = this.getPos().offset(this.facing);
        this.posBack = this.getPos().offset(this.facingOpposite);
        this.initInventories();
    }

    protected void initInventories()
    {
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, NUM_SLOTS, 1, false, "Items", this);
        this.itemHandlerExternal = new ItemHandlerWrapperFifo(this.getBaseItemHandler());
    }

    public int getInsertSlot()
    {
        return this.insertSlot;
    }

    public int getExtractSlot()
    {
        return this.extractSlot;
    }

    public void setInsertSlot(int slot)
    {
        this.insertSlot = slot;
    }

    public void setExtractSlot(int slot)
    {
        this.extractSlot = slot;
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer()
    {
        if (Configs.fifoBufferUseWrappedInventory)
        {
            return new ItemHandlerWrapperOffset(this.getBaseItemHandler());
        }

        return this.getBaseItemHandler();
    }

    @Override
    public void setFacing(EnumFacing facing)
    {
        super.setFacing(facing);

        this.facingOpposite = this.facing.getOpposite();
        this.posFront = this.getPos().offset(this.facing);
        this.posBack = this.getPos().offset(this.facingOpposite);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.insertSlot = nbt.getByte("InsertPos");

        if (nbt.hasKey("ExtractPos", Constants.NBT.TAG_BYTE) == true)
        {
            this.extractSlot = nbt.getByte("ExtractPos");
        }

        this.setFacing(this.facing); // Update the opposite and the front and back BlockPos
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("InsertPos", (byte)this.insertSlot);
        nbt.setByte("ExtractPos", (byte)this.extractSlot);
    }

    protected Vec3d getItemPosition()
    {
        double x = this.getPos().getX() + 0.5 + this.facing.getFrontOffsetX() * 0.625;
        double y = this.getPos().getY() + 0.5 + this.facing.getFrontOffsetY() * 0.5;
        double z = this.getPos().getZ() + 0.5 + this.facing.getFrontOffsetZ() * 0.625;

        if (this.facing == EnumFacing.DOWN)
        {
            y -= 0.25;
        }

        return new Vec3d(x, y, z);
    }

    @Override
    protected void onRedstoneChange(boolean state)
    {
        if (state == true)
        {
            this.getWorld().scheduleUpdate(this.getPos(), this.getBlockType(), 1);
        }
    }

    /**
     * Tries to do whatever action is appropriate on a redstone pulse.
     * @return whether the action succeeded
     */
    protected boolean handleItemsOnRedstonePulse()
    {
        ItemStack stack = this.itemHandlerExternal.getStackInSlot(0);

        if (stack != null)
        {
            TileEntity te = this.worldObj.getTileEntity(this.posFront);
            IItemHandler inv = te != null ? te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.facingOpposite) : null;

            if (inv != null)
            {
                // First simulate adding the item, if that succeeds, then actually extract it and insert it into the adjacent inventory
                // TODO Add a version of the method that doesn't try to stack first
                stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, true);

                if (stack == null)
                {
                    stack = this.itemHandlerExternal.extractItem(0, 1, false);
                    InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);

                    if (Configs.disableSounds == false)
                    {
                        this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.3f, 1f);
                    }

                    return true;
                }

                return false;
            }
            else
            {
                // No adjacent inventory, drop the item in world
                stack = this.itemHandlerExternal.extractItem(0, 1, false);
                EntityUtils.dropItemStacksInWorld(this.worldObj, this.getItemPosition(), stack, -1, true, false);

                if (Configs.disableSounds == false)
                {
                    this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.3f, 1f);
                }

                return true;
            }
        }

        return true;
    }

    @Override
    public void onBlockTick(IBlockState state, Random rand)
    {
        this.handleItemsOnRedstonePulse();
    }

    public int getOffsetSlot(int slot)
    {
        int numSlots = this.getBaseItemHandler().getSlots();
        slot += this.extractSlot;

        if (slot >= numSlots)
        {
            slot -= numSlots;
        }

        return slot;
    }

    private class ItemHandlerWrapperFifo extends ItemHandlerWrapperSelective
    {
        public ItemHandlerWrapperFifo(IItemHandler baseHandler)
        {
            super(baseHandler);
        }

        @Override
        public int getSlots()
        {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return super.getStackInSlot(TileEntityBufferFifo.this.extractSlot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (stack == null)
            {
                return null;
            }

            int sizeOrig = stack.stackSize;

            ItemStack stackRet = super.insertItem(TileEntityBufferFifo.this.insertSlot, stack, simulate);

            if (simulate == false && (stackRet == null || stackRet.stackSize != sizeOrig) && ++TileEntityBufferFifo.this.insertSlot >= super.getSlots())
            {
                TileEntityBufferFifo.this.insertSlot = 0;
            }

            return stackRet;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            ItemStack stackRet = super.extractItem(TileEntityBufferFifo.this.extractSlot, amount, simulate);

            if (simulate == false && stackRet != null && ++TileEntityBufferFifo.this.extractSlot >= super.getSlots())
            {
                TileEntityBufferFifo.this.extractSlot = 0;
            }

            return stackRet;
        }
    }

    private class ItemHandlerWrapperOffset extends ItemHandlerWrapperSelectiveModifiable
    {
        public ItemHandlerWrapperOffset(IItemHandlerModifiable baseHandler)
        {
            super(baseHandler);
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return super.getStackInSlot(TileEntityBufferFifo.this.getOffsetSlot(slot));
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack)
        {
            //System.out.printf("setting slot %d (offset: %d) to: %s\n", slot, TileEntityBufferFifo.this.getOffsetSlot(slot), stack);
            super.setStackInSlot(TileEntityBufferFifo.this.getOffsetSlot(slot), stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            //System.out.printf("inserting to slot %d (offset: %d) to: %s\n", slot, TileEntityBufferFifo.this.getOffsetSlot(slot), stack);
            return super.insertItem(TileEntityBufferFifo.this.getOffsetSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return super.extractItem(TileEntityBufferFifo.this.getOffsetSlot(slot), amount, simulate);
        }
    }

    @Override
    public ContainerBufferFifo getContainer(EntityPlayer player)
    {
        return new ContainerBufferFifo(player, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiBufferFifo(this.getContainer(player), this);
    }
}
