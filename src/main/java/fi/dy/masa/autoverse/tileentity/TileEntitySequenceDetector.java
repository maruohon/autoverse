package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockSequenceDetector;
import fi.dy.masa.autoverse.gui.client.GuiSequenceDetector;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerSequenceDetector;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequenceDetector;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntitySequenceDetector extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemHandlerWrapperSequenceDetector detector;
    private boolean changePending;
    private boolean powered;
    private int onTimeRSTicks = 1;
    private int scheduledTime = -1;

    public TileEntitySequenceDetector()
    {
        super(ReferenceNames.NAME_BLOCK_SEQUENCE_DETECTOR);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput     = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutput    = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut", this);
        this.detector           = new ItemHandlerWrapperSequenceDetector(this.inventoryInput, this.inventoryOutput, this);
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

    public int getPulseLength()
    {
        return this.onTimeRSTicks;
    }

    @Override
    public void setPlacementProperties(World world, BlockPos pos, ItemStack stack, NBTTagCompound tag)
    {
        super.setPlacementProperties(world, pos, stack, tag);

        if (tag.hasKey("sequence_detector.on_time", Constants.NBT.TAG_BYTE))
        {
            this.setOnTime(tag.getByte("sequence_detector.on_time"), true);
        }
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        if (propId == 1)
        {
            this.setOnTime(value, true);
            return true;
        }

        return super.applyProperty(propId, value);
    }

    @Override
    public int[] getProperties()
    {
        int[] values = super.getProperties();

        values[1] = this.onTimeRSTicks;

        return values;
    }

    private void setOnTime(int value, boolean markDirty)
    {
        this.onTimeRSTicks = MathHelper.clamp(value, 1, 127);

        if (markDirty)
        {
            this.markDirty();
        }
    }

    public void onSequenceMatch()
    {
        // Don't allow new pulses if the previous one is still ON
        if (this.changePending == false)
        {
            this.powered = true;
            this.changePending = true;
            this.scheduleBlockUpdate(1, false);
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState)
    {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void onLoad()
    {
        if (this.getWorld().isRemote == false)
        {
            this.setScheduledTimeFromDelay();
        }
    }

    private void setScheduledTimeFromDelay()
    {
        // Set the scheduled times on chunk load from the relative delays loaded from NBT.
        // This check is to ensure that the delays don't get modified multiple times.
        // Valid delay values are 1..255, thus this check can avoid adding an extra boolean field "initialized".
        if (this.scheduledTime >= 0 && this.scheduledTime <= 255)
        {
            final int currentTime = (int) (this.getWorld().getTotalWorldTime() & 0x3FFFFFFF);
            this.scheduledTime += currentTime;
        }
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = this.pushItemsToAdjacentInventory(this.inventoryOutput, 0, this.getFrontPosition(), this.getOppositeFacing(), false);
        boolean movedIn = this.detector.moveItems();
        final int currentTime = (int) (this.getWorld().getTotalWorldTime() & 0x3FFFFFFF);

        if (this.changePending && this.scheduledTime <= currentTime)
        {
            IBlockState newState = this.getWorld().getBlockState(this.getPos());
            newState = newState.withProperty(BlockSequenceDetector.POWERED, this.powered);
            this.getWorld().setBlockState(this.getPos(), newState);

            // Emit a redstone pulse when the detector triggers
            if (this.powered)
            {
                final int delay = this.onTimeRSTicks * 2;
                this.scheduledTime = delay + currentTime;
                this.powered = false;
                this.scheduleBlockUpdate(delay, false);
            }
            else
            {
                this.scheduledTime = -1;
                this.changePending = false;
            }
        }
        else if (movedIn || movedOut)
        {
            this.scheduleUpdateIfNeeded(movedIn);
        }
        else if (this.scheduledTime >= currentTime)
        {
            this.scheduleBlockUpdate(this.scheduledTime - currentTime, false);
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
            this.scheduleBlockUpdate(1, force);
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        this.scheduleUpdateIfNeeded(true);
    }

    @Override
    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutput);
        this.detector.dropAllItems(this.getWorld(), this.getPos());
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        super.readFromNBTCustom(tag);

        final int mask = tag.getByte("StateMask");
        this.changePending = (mask & 0x40) != 0;
        this.powered = (mask & 0x80) != 0;
        this.setOnTime(tag.getByte("OnTime"), false);

        // See onLoad() and setScheduledTimeFromDelay()
        this.scheduledTime = tag.getShort("Scheduled");

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
        nbt.setByte("OnTime", (byte) this.onTimeRSTicks);

        final int currentTime = (int) (this.getWorld().getTotalWorldTime() & 0x3FFFFFFF);
        nbt.setShort("Scheduled", (short) (this.scheduledTime - currentTime));

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
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        if (action == 0)
        {
            this.setOnTime(this.onTimeRSTicks + element, true);
        }
    }

    @Override
    public ContainerSequenceDetector getContainer(EntityPlayer player)
    {
        return new ContainerSequenceDetector(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiSequenceDetector(this.getContainer(player), this);
    }
}
