package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.effects.Effects;
import fi.dy.masa.autoverse.gui.client.GuiBlockDetector;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBlockDetector;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperExtractOnly;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperBlockDetector;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntityBlockDetector extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutDetection;
    private ItemStackHandlerTileEntity inventoryOutNormal;
    private ItemHandlerWrapperBlockDetector detector;
    private IItemHandler inventoryWrapperOutDetection;

    private EnumFacing facingDetectionOut = EnumFacing.EAST;
    private int angle = 0;
    private int delay = 1;
    private int distance = 1;
    private long nextDetection;
    private boolean detectorRunning;
    private boolean useIndicators;

    public TileEntityBlockDetector()
    {
        super(ReferenceNames.NAME_BLOCK_DETECTOR);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput         = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutDetection  = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOutDetection", this);
        this.inventoryOutNormal     = new ItemStackHandlerTileEntity(2, 1, 64, false, "ItemsOutNormal", this);
        this.itemHandlerBase        = this.inventoryInput;
        this.inventoryWrapperOutDetection = new ItemHandlerWrapperExtractOnly(this.inventoryOutDetection);

        this.detector = new ItemHandlerWrapperBlockDetector(this.inventoryInput, this.inventoryOutNormal, this);

        this.itemHandlerExternal = this.detector;
    }

    public IItemHandler getInventoryInput()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOutDetection()
    {
        return this.inventoryOutDetection;
    }

    public IItemHandler getInventoryOutNormal()
    {
        return this.inventoryOutNormal;
    }

    public ItemHandlerWrapperBlockDetector getDetector()
    {
        return this.detector;
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 1:
                this.setDetectionOutputSide(EnumFacing.byIndex(value), false);
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    @Override
    public int[] getProperties()
    {
        int[] values = super.getProperties();

        values[1] = this.facingDetectionOut.getIndex();

        return values;
    }

    public boolean getUseIndicators()
    {
        return this.useIndicators;
    }

    public void setUseIndicators(boolean useIndicators)
    {
        this.useIndicators = useIndicators;
    }

    public void setAngle(int angle)
    {
        this.angle = MathHelper.clamp(angle, 0, 15);
    }

    public void setDelay(int delay)
    {
        this.delay = MathHelper.clamp(((int) delay) & 0xFF, 0, 255);
    }

    public void setDistance(int distance)
    {
        this.distance = MathHelper.clamp(distance, 0, 15);
    }

    public void startDetector()
    {
        this.detectorRunning = true;
        this.setNextDetectionTime();
        this.scheduleUpdateIfNeeded(false);
    }

    public void stopDetector()
    {
        this.detectorRunning = false;
        this.nextDetection = 0;
    }

    public void setDetectionOutputSide(EnumFacing side, boolean force)
    {
        if (force || side != this.getFacing())
        {
            this.facingDetectionOut = side;
        }
    }

    public EnumFacing getDetectionOutRelativeFacing()
    {
        return PositionUtils.getRelativeFacing(this.getFacing(), this.facingDetectionOut);
    }

    @Override
    public void rotate(Rotation rotationIn)
    {
        this.setDetectionOutputSide(rotationIn.rotate(this.facingDetectionOut), true);

        super.rotate(rotationIn);
    }

    @Override
    public boolean onRightClickBlock(World world, BlockPos pos, IBlockState state, EnumFacing side,
            EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ)
    {
        if (super.onRightClickBlock(world, pos, state, side, player, hand, hitX, hitY, hitZ))
        {
            return true;
        }

        ItemStack stack = player.getHeldItem(hand);

        if (stack.isEmpty() && player.isSneaking())
        {
            if (side == this.facingDetectionOut)
            {
                side = side.getOpposite();
            }

            this.setDetectionOutputSide(side, false);
            this.markDirty();
            this.notifyBlockUpdate(this.getPos());
            return true;
        }

        return false;
    }

    @Override
    protected void onRedstoneChange(boolean state)
    {
        if (state)
        {
            // If delay is set to 0, then the detection runs on the rising edge of a redstone pulse
            if (this.delay == 0)
            {
                this.nextDetection = this.getWorld().getTotalWorldTime() + 1;
                this.reScheduleUpdateIfSooner(1);
            }
        }
        else if (this.nextDetection == 0 || this.nextDetection <= this.getWorld().getTotalWorldTime())
        {
            this.setNextDetectionTime();
            this.scheduleUpdateIfNeeded(false);
        }
    }

    private BlockPos getDetectionPosition(int distance, float angle1, float angle2)
    {
        EnumFacing facing = this.getFacing();
        BlockPos pos = this.getFrontPosition();
        float offset1 = angle1 * (float) distance;
        float offset2 = angle2 * (float) distance;

        switch (facing.getAxis())
        {
            case X:
                return pos.add(facing.getXOffset() * distance + 0.5f, offset1 + 0.5f, offset2 + 0.5f);

            case Y:
                return pos.add(offset1 + 0.5f, facing.getYOffset() * distance + 0.5f, offset2 + 0.5f);

            case Z:
            default:
                return pos.add(offset1 + 0.5f, offset2 + 0.5f, facing.getZOffset() * distance + 0.5f);
        }
    }

    private boolean detectBlocks()
    {
        float angle1 = (Autoverse.RAND.nextFloat() - 0.5f) * 2f * (this.angle / 15f);
        float angle2 = (Autoverse.RAND.nextFloat() - 0.5f) * 2f * (this.angle / 15f);

        if (this.useIndicators)
        {
            this.getWorld().playSound(null, this.getPos(), SoundEvents.BLOCK_NOTE_HAT, SoundCategory.BLOCKS, 0.4f, 0.8f);
        }

        for (int distance = 0; distance <= this.distance; distance++)
        {
            BlockPos pos = this.getDetectionPosition(distance, angle1, angle2);

            if (this.getWorld().isBlockLoaded(pos, true) == false)
            {
                continue;
            }

            if (this.useIndicators)
            {
                Effects.spawnParticlesFromServer(this.getWorld().provider.getDimension(), pos,
                        EnumParticleTypes.REDSTONE, 4, 0f, 0f);
            }

            if (this.getWorld().isAirBlock(pos) == false)
            {
                IBlockState state = this.getWorld().getBlockState(pos);
                ItemStack stack = new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state));

                if (stack.isEmpty() == false)
                {
                    IItemHandler inv = this.detector.getDetectionInventory();
                    int slot = InventoryUtils.getSlotOfFirstMatchingItemStack(inv, stack);

                    if (slot != -1)
                    {
                        return InventoryUtils.tryMoveStack(inv, slot, this.inventoryOutDetection, 0, 1) != InvResult.MOVED_NOTHING;
                    }
                }

                // Other blocks than ones programmed to the detection inventory
                IItemHandler inv = this.detector.getOthersBufferInventory();
                return InventoryUtils.tryMoveStack(inv, 0, this.inventoryOutDetection, 0, 1) != InvResult.MOVED_NOTHING;

                // debugging
                //BlockUtils.setBlockStateWithPlaceSound(this.getWorld(), pos, Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.ORANGE), 3);
            }
        }

        return false;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedItems = false;
        BlockPos posDetectionOut = this.getPos().offset(this.facingDetectionOut);
        movedItems |= this.pushItemsToAdjacentInventory(this.inventoryOutDetection, 0, posDetectionOut, this.getOppositeFacing(), false);
        movedItems |= this.detector.moveItems();

        if (this.detectorRunning)
        {
            // In redstone mode nextDetection is initially 0, before a redstone pulse
            if ((this.delay == 0 || this.getRedstoneState() == false) &&
                (this.nextDetection != 0 && this.nextDetection <= this.getWorld().getTotalWorldTime()))
            {
                this.detectBlocks();
                this.setNextDetectionTime();
            }

            this.scheduleUpdateIfNeeded(movedItems);
        }
        else if (movedItems)
        {
            this.scheduleUpdateIfNeeded(true);
        }
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded(false);
    }

    private void scheduleUpdateIfNeeded(boolean immediate)
    {
        if (immediate ||
            this.inventoryInput.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOutDetection.getStackInSlot(0).isEmpty() == false)
        {
            this.reScheduleUpdateIfSooner(1);
        }
        else if (this.detectorRunning && this.nextDetection > this.getWorld().getTotalWorldTime())
        {
            this.scheduleBlockUpdate((int) (this.nextDetection - this.getWorld().getTotalWorldTime()), false);
        }
    }

    private void setNextDetectionTime()
    {
        if (this.delay > 0)
        {
            this.nextDetection = this.getWorld().getTotalWorldTime() + this.delay * 20;
        }
        else
        {
            this.nextDetection = 0;
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        this.scheduleUpdateIfNeeded(true);
    }

    @Override
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        if (action == 0)
        {
            this.useIndicators = ! this.useIndicators;
            this.markDirty();
        }
    }

    @Override
    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutDetection);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutNormal);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.detector.getDetectionInventory());
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.detector.getOthersBufferInventory());
        this.detector.dropAllItems(this.getWorld(), this.getPos());
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == this.facingDetectionOut)
        {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.inventoryWrapperOutDetection);
        }
        else
        {
            return super.getCapability(capability, facing);
        }
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        super.readFromNBTCustom(tag);

        this.setDetectionOutputSide(EnumFacing.byIndex(tag.getByte("Facing2")), false);
        this.angle = tag.getByte("Angle");
        this.setDelay(tag.getByte("Delay"));
        this.distance = tag.getByte("Distance");
        this.nextDetection = tag.getLong("Next");
        this.detectorRunning = tag.getBoolean("Running");
        this.useIndicators = tag.getBoolean("Indicators");

        this.inventoryInput.deserializeNBT(tag);
        this.inventoryOutNormal.deserializeNBT(tag);
        this.inventoryOutDetection.deserializeNBT(tag);

        this.detector.deserializeNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setByte("Facing2", (byte) this.facingDetectionOut.getIndex());
        nbt.setByte("Angle", (byte) this.angle);
        nbt.setByte("Delay", (byte) this.delay);
        nbt.setByte("Distance", (byte) this.distance);
        nbt.setLong("Next", this.nextDetection);
        nbt.setBoolean("Running", this.detectorRunning);
        nbt.setBoolean("Indicators", this.useIndicators);
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
        super.writeItemsToNBT(nbt);

        nbt.merge(this.inventoryInput.serializeNBT());
        nbt.merge(this.inventoryOutNormal.serializeNBT());
        nbt.merge(this.inventoryOutDetection.serializeNBT());

        nbt.merge(this.detector.serializeNBT());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag = super.getUpdatePacketTag(tag);
        tag.setByte("f", (byte) ((this.facingDetectionOut.getIndex() << 4) | tag.getByte("f")));
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        int facings = tag.getByte("f");
        this.setDetectionOutputSide(EnumFacing.byIndex((facings >>> 4) & 0x7), true);

        super.handleUpdateTag(tag);
    }

    @Override
    public ContainerBlockDetector getContainer(EntityPlayer player)
    {
        return new ContainerBlockDetector(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiBlockDetector(this.getContainer(player), this);
    }
}
