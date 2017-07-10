package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockRedstoneEmitter;
import fi.dy.masa.autoverse.gui.client.GuiRedstoneEmitter;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerRedstoneEmitter;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperRedstoneEmitter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityRedstoneEmitter extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemHandlerWrapperRedstoneEmitter emitter;
    private int sideMask;
    private boolean changePending;
    private boolean powered;

    public TileEntityRedstoneEmitter()
    {
        super(ReferenceNames.NAME_BLOCK_REDSTONE_EMITTER);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput     = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutput    = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut", this);
        this.emitter            = new ItemHandlerWrapperRedstoneEmitter(this.inventoryInput, this.inventoryOutput, this);
        this.itemHandlerBase    = this.inventoryInput;
        this.itemHandlerExternal = this.emitter;
    }

    public IItemHandler getInventoryIn()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOut()
    {
        return this.inventoryOutput;
    }

    public ItemHandlerWrapperRedstoneEmitter getEmitterHandler()
    {
        return this.emitter;
    }

    public void setSideEnabled(int side, boolean enabled)
    {
        if (enabled)
        {
            this.sideMask |= (1 << side);
        }
        else
        {
            this.sideMask &= ~(1 << side);
        }

        this.notifyBlockUpdate(this.getPos());
    }

    public void setSideMask(int mask)
    {
        this.sideMask = mask;
        this.notifyBlockUpdate(this.getPos());
    }

    public int getSideMask()
    {
        return this.sideMask;
    }

    public void setIsPowered(boolean powered)
    {
        this.powered = powered;
        this.changePending = true;
        this.scheduleBlockUpdate(1, true);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState)
    {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = this.pushItemsToAdjacentInventory(this.inventoryOutput, 0, this.posFront, this.facingOpposite, false);
        boolean movedIn = this.emitter.moveItems();

        if (movedIn || movedOut)
        {
            this.scheduleUpdateIfNeeded(movedIn);
        }

        if (this.changePending)
        {
            IBlockState newState = this.getWorld().getBlockState(this.getPos());
            newState = newState.withProperty(BlockRedstoneEmitter.POWERED, this.powered);
            this.getWorld().setBlockState(this.getPos(), newState);
            this.changePending = false;
        }
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded(false);
    }

    private void scheduleUpdateIfNeeded(boolean force)
    {
        if (force || 
            this.inventoryInput.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOutput.getStackInSlot(0).isEmpty() == false)
        {
            this.scheduleBlockUpdate(1, false);
        }
    }

    @Override
    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutput);
        this.emitter.dropAllItems(this.getWorld(), this.getPos());
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        // Input inventory
        if (inventoryId == 0)
        {
            this.scheduleBlockUpdate(1, false);
        }
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        super.readFromNBTCustom(tag);

        int mask = tag.getByte("StateMask");
        this.sideMask = mask & 0x3F;
        this.changePending = (mask & 0x40) != 0;
        this.powered = (mask & 0x80) != 0;

        this.inventoryInput.deserializeNBT(tag);
        this.inventoryOutput.deserializeNBT(tag);
        this.emitter.deserializeNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        int mask = this.sideMask;
        mask |= (this.changePending ? 0x40 : 0x00);
        mask |= (this.powered ? 0x80 : 0x00);

        nbt.setByte("StateMask", (byte) mask);

        nbt.merge(this.emitter.serializeNBT());

        return nbt;
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        // Do nothing here, see readFromNBTCustom() above...
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        nbt.merge(this.inventoryInput.serializeNBT());
        nbt.merge(this.inventoryOutput.serializeNBT());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag = super.getUpdatePacketTag(tag);
        tag.setByte("msk", (byte) this.sideMask);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.sideMask = tag.getByte("msk");

        super.handleUpdateTag(tag);
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return new ContainerRedstoneEmitter(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiRedstoneEmitter(this.getContainer(player), this);
    }
}
