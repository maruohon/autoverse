package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockRedstoneEmitter;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiSequenceDetector;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerSequenceDetector;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequenceDetector;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class TileEntitySequenceDetector extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemHandlerWrapperSequenceDetector detector;
    private boolean changePending;
    private boolean powered;

    public TileEntitySequenceDetector()
    {
        super(ReferenceNames.NAME_BLOCK_SEQUENCE_DETECTOR);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput     = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutput    = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut", this);
        this.detector           = new ItemHandlerWrapperSequenceDetector(this.inventoryInput, this);
        this.itemHandlerBase    = this.inventoryInput;
        this.itemHandlerExternal = this.detector;
    }

    public IItemHandler getInventoryIn()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOut()
    {
        return this.inventoryOutput;
    }

    public ItemHandlerWrapperSequenceDetector getDetectorHandler()
    {
        return this.detector;
    }

    public void onSequenceMatch()
    {
        this.powered = true;
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
        boolean movedIn = InventoryUtils.tryMoveEntireStackOnly(this.inventoryInput, 0, this.inventoryOutput, 0) != InvResult.MOVED_NOTHING;

        // Emit a 1 redstone tick pulse when the detector triggers
        if (this.changePending)
        {
            IBlockState newState = this.getWorld().getBlockState(this.getPos());
            newState = newState.withProperty(BlockRedstoneEmitter.POWERED, this.powered);
            this.getWorld().setBlockState(this.getPos(), newState);

            if (this.powered == false)
            {
                this.changePending = false;
            }
            else
            {
                this.powered = false;
                this.scheduleBlockUpdate(2, true);
            }
        }
        else if (movedIn || movedOut)
        {
            this.scheduleUpdateIfNeeded();
        }
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded();
    }

    private void scheduleUpdateIfNeeded()
    {
        if (this.inventoryInput.getStackInSlot(0).isEmpty() == false ||
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
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        // Input inventory
        this.scheduleBlockUpdate(1, false);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        super.readFromNBTCustom(tag);

        int mask = tag.getByte("StateMask");
        this.changePending = (mask & 0x40) != 0;
        this.powered = (mask & 0x80) != 0;

        this.inventoryInput.deserializeNBT(tag);
        this.inventoryOutput.deserializeNBT(tag);
        this.detector.deserializeNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        int mask = 0;
        mask |= (this.changePending ? 0x40 : 0x00);
        mask |= (this.powered ? 0x80 : 0x00);

        nbt.setByte("StateMask", (byte) mask);

        nbt.merge(this.detector.serializeNBT());

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
    public ContainerSequenceDetector getContainer(EntityPlayer player)
    {
        return new ContainerSequenceDetector(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiSequenceDetector(this.getContainer(player), this);
    }
}
