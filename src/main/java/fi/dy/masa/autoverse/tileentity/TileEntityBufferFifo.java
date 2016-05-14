package fi.dy.masa.autoverse.tileentity;

import net.minecraft.block.Block;
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
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiBufferFifo;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperContainer;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperSelective;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBufferFifo;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityBufferFifo extends TileEntityAutoverseInventory
{
    private int insertSlot;
    private int extractSlot;
    private boolean redstoneState;
    EnumFacing facingOpposite;
    BlockPos posFront;
    BlockPos posBack;

    public TileEntityBufferFifo()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, 117, 1, false, "Items", this);
        this.itemHandlerExternal = new ItemHandlerWrapperFifo(this.getBaseItemHandler());
        this.facingOpposite = EnumFacing.DOWN;
        this.posFront = this.getPos().offset(this.facing);
        this.posBack = this.getPos().offset(this.facingOpposite);
    }

    public int getInsertSlot()
    {
        return this.insertSlot;
    }

    public int getExtractSlot()
    {
        return this.extractSlot;
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer()
    {
        return new ItemHandlerWrapperContainer(this.getBaseItemHandler(), this.getBaseItemHandler());
    }

    @Override
    public void setFacing(EnumFacing facing)
    {
        super.setFacing(facing);

        this.facingOpposite = this.facing.getOpposite();
        this.posFront = this.getPos().offset(this.facing);
        this.posBack = this.getPos().offset(this.facingOpposite);
    }

    private Vec3d getItemPosition()
    {
        double x = this.getPos().getX() + 0.5 + this.facing.getFrontOffsetX() * 0.625;
        double y = this.getPos().getY() + 0.5 + this.facing.getFrontOffsetY() * 0.625;
        double z = this.getPos().getZ() + 0.5 + this.facing.getFrontOffsetZ() * 0.625;

        return new Vec3d(x, y, z);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.insertSlot = nbt.getInteger("InsertPos");
        this.extractSlot = nbt.getInteger("ExtractPos");
        this.redstoneState = nbt.getBoolean("Redstone");

        this.setFacing(this.facing); // Update the opposite and the front and back BlockPos
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setInteger("InsertPos", this.insertSlot);
        nbt.setInteger("ExtractPos", this.extractSlot);
        nbt.setBoolean("Redstone", this.redstoneState);
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        boolean redstone = this.worldObj.isBlockPowered(this.getPos());

        if (redstone != this.redstoneState && redstone == true)
        {
            this.popItem();
        }

        this.redstoneState = redstone;
    }

    private boolean popItem()
    {
        ItemStack stack = this.itemHandlerExternal.getStackInSlot(0);

        if (stack == null)
        {
            return false;
        }

        TileEntity te = this.worldObj.getTileEntity(this.posFront);

        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.facingOpposite))
        {
            IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.facingOpposite);

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
        }

        // No adjacent inventory, drop the item in world
        stack = this.itemHandlerExternal.extractItem(0, 1, false);
        EntityUtils.dropItemStacksInWorld(this.worldObj, this.getItemPosition(), stack, -1, true, false);

        if (Configs.disableSounds == false)
        {
            this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.3f, 1f);
        }

        return true;
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
