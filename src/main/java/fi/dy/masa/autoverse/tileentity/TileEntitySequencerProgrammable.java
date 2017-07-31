package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiSequencerProgrammable;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerSequencerProgrammable;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequencerProgrammable;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntitySequencerProgrammable extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemHandlerWrapperSequencerProgrammable sequencer;

    public TileEntitySequencerProgrammable()
    {
        super(ReferenceNames.NAME_BLOCK_SEQUENCER_PROGRAMMABLE);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput     = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutput    = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut", this);
        this.sequencer          = new ItemHandlerWrapperSequencerProgrammable(this.inventoryInput, this.inventoryOutput, this);
        this.itemHandlerBase    = this.inventoryInput;
        this.itemHandlerExternal = this.sequencer;
    }

    public IItemHandler getInventoryIn()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOut()
    {
        return this.inventoryOutput;
    }

    public ItemHandlerWrapperSequencerProgrammable getSequencerHandler()
    {
        return this.sequencer;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = this.pushItemsToAdjacentInventory(this.inventoryOutput, 0, this.posFront, this.facingOpposite, false);
        boolean movedIn = this.sequencer.moveItems();

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
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.sequencer.getSequenceInventory());
        this.sequencer.dropAllItems(this.getWorld(), this.getPos());
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        this.scheduleUpdateIfNeeded(false);
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        // Do nothing here, see readFromNBTCustom() above...
        this.inventoryInput.deserializeNBT(nbt);
        this.inventoryOutput.deserializeNBT(nbt);
        this.sequencer.deserializeNBT(nbt);
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        nbt.merge(this.inventoryInput.serializeNBT());
        nbt.merge(this.inventoryOutput.serializeNBT());
        nbt.merge(this.sequencer.serializeNBT());
    }

    @Override
    public ContainerSequencerProgrammable getContainer(EntityPlayer player)
    {
        return new ContainerSequencerProgrammable(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiSequencerProgrammable(this.getContainer(player), this);
    }
}
