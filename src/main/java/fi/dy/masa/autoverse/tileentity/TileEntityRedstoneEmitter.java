package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiRedstoneEmitter;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerRedstoneEmitter;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperRedstoneEmitter;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequenceBase;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityRedstoneEmitter extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemHandlerWrapperRedstoneEmitter emitter;
    private int powerMask;
    private int sideMask;

    public TileEntityRedstoneEmitter()
    {
        super(ReferenceNames.NAME_BLOCK_REDSTONE_EMITTER);
    }

    protected TileEntityRedstoneEmitter(String name)
    {
        super(name);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput     = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutput    = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut", this);
        this.itemHandlerBase    = this.inventoryInput;

        this.createEmitterInventory();

        this.itemHandlerExternal = this.getEmitterHandlerBase();
    }

    protected void createEmitterInventory()
    {
        this.emitter = new ItemHandlerWrapperRedstoneEmitter(this.inventoryInput, this.inventoryOutput, this);
    }

    public IItemHandler getInventoryIn()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOut()
    {
        return this.inventoryOutput;
    }

    public ItemHandlerWrapperRedstoneEmitter getEmitterHandlerBasic()
    {
        return this.emitter;
    }

    public ItemHandlerWrapperSequenceBase getEmitterHandlerBase()
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

    public void setSidePowered(int side, boolean powered)
    {
        if (powered)
        {
            this.powerMask |= (1 << side);
        }
        else
        {
            this.powerMask &= ~(1 << side);
        }

        this.notifyBlockUpdate(this.getPos());
        this.notifyNeighbor(side);
    }

    public void setPoweredMask(int mask)
    {
        int powerMaskOld = this.powerMask;
        this.powerMask = mask;

        for (int side = 0, bit = 0x1; side < 6; side++, bit <<= 1)
        {
            if ((powerMaskOld & bit) != (mask & bit))
            {
                this.notifyNeighbor(side);
            }
        }

        this.notifyBlockUpdate(this.getPos());
    }

    public int getPoweredMask()
    {
        return this.powerMask;
    }

    public boolean isSidePowered(int side)
    {
        return (this.powerMask & (1 << side)) != 0;
    }

    public void setIsPowered(boolean powered)
    {
        this.setPoweredMask(powered ? this.sideMask : 0);
    }

    protected void notifyNeighbor(int sideIndex)
    {
        EnumFacing side = EnumFacing.byIndex(sideIndex);
        this.getWorld().neighborChanged(this.getPos().offset(side), this.getBlockType(), this.getPos());
        this.getWorld().observedNeighborChanged(this.getPos().offset(side), this.getBlockType(), this.getPos());
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = this.pushItemsToAdjacentInventory(this.inventoryOutput, 0, this.getFrontPosition(), this.getOppositeFacing(), false);
        boolean movedIn = this.getEmitterHandlerBase().moveItems();

        if (movedIn || movedOut)
        {
            this.scheduleUpdateIfNeeded(movedIn);
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
        this.getEmitterHandlerBase().dropAllItems(this.getWorld(), this.getPos());
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        this.scheduleUpdateIfNeeded(false);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        super.readFromNBTCustom(tag);

        this.sideMask = tag.getByte("SideMask");
        this.powerMask = tag.getByte("PowerMask");

        this.inventoryInput.deserializeNBT(tag);
        this.inventoryOutput.deserializeNBT(tag);
        this.getEmitterHandlerBase().deserializeNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("SideMask", (byte) this.sideMask);
        nbt.setByte("PowerMask", (byte) this.powerMask);

        nbt.merge(this.getEmitterHandlerBase().serializeNBT());

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
        tag.setByte("smsk", (byte) this.sideMask);
        tag.setByte("pmsk", (byte) this.powerMask);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.sideMask = tag.getByte("smsk");
        this.powerMask = tag.getByte("pmsk");

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
