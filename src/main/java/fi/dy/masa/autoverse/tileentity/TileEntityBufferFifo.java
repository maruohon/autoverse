package fi.dy.masa.autoverse.tileentity;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
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
    private int insertPos;
    private int extractPos;
    private boolean redstoneState;
    EnumFacing facing;

    public TileEntityBufferFifo()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, 117, 1, false, "Items", this);
        this.itemHandlerExternal = new ItemHandlerWrapperFifo(this.getBaseItemHandler());
        this.facing = EnumFacing.UP;
    }

    public int getInsertPos()
    {
        return this.insertPos;
    }

    public int getExtractPos()
    {
        return this.extractPos;
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer()
    {
        return new ItemHandlerWrapperContainer(this.getBaseItemHandler(), this.getBaseItemHandler());
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.insertPos = nbt.getInteger("InsertPos");
        this.extractPos = nbt.getInteger("ExtractPos");
        this.redstoneState = nbt.getBoolean("Redstone");

        this.facing = EnumFacing.getFront(this.rotation);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setInteger("InsertPos", this.insertPos);
        nbt.setInteger("ExtractPos", this.extractPos);
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

        BlockPos pos = this.getPos().offset(this.facing);
        TileEntity te = this.worldObj.getTileEntity(pos);

        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.facing.getOpposite()))
        {
            IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.facing.getOpposite());

            if (inv != null)
            {
                // First simulate adding the item, if that succeeds, then actually extract it and insert it into the adjacent inventory
                // TODO Add a version of the method that doesn't try to stack first
                stack = InventoryUtils.tryInsertItemStackToInventory(inv, stack, true);

                if (stack == null)
                {
                    stack = this.itemHandlerExternal.extractItem(0, 1, false);
                    InventoryUtils.tryInsertItemStackToInventory(inv, stack, false);

                    return true;
                }

                return false;
            }
        }

        stack = this.itemHandlerExternal.extractItem(0, 1, false);
        EntityUtils.dropItemStacksInWorld(this.worldObj, pos, stack, -1, true);

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
            return super.getStackInSlot(TileEntityBufferFifo.this.extractPos);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (stack == null)
            {
                return null;
            }

            int sizeOrig = stack.stackSize;

            ItemStack stackRet = super.insertItem(TileEntityBufferFifo.this.insertPos, stack, simulate);

            if (simulate == false && (stackRet == null || stackRet.stackSize != sizeOrig) && ++TileEntityBufferFifo.this.insertPos >= super.getSlots())
            {
                TileEntityBufferFifo.this.insertPos = 0;
            }

            return stackRet;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            ItemStack stackRet = super.extractItem(TileEntityBufferFifo.this.extractPos, amount, simulate);

            if (simulate == false && stackRet != null && ++TileEntityBufferFifo.this.extractPos >= super.getSlots())
            {
                TileEntityBufferFifo.this.extractPos = 0;
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
