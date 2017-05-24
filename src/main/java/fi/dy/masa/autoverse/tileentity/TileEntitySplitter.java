package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiSplitter;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerSplitter;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperExtractOnly;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFilter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntitySplitter extends TileEntityAutoverseInventory
{
    protected ItemStackHandlerTileEntity inventoryInputManual;
    protected ItemStackHandlerTileEntity inventoryReset;
    protected ItemStackHandlerTileEntity inventorySequence;
    protected ItemStackHandlerTileEntity inventoryOut1;
    protected ItemStackHandlerTileEntity inventoryOut2;

    protected IItemHandler wrappedInventoryOut1;
    protected IItemHandler wrappedInventoryOut2;
    protected ItemHandlerWrapperFilter inventoryInput;

    protected EnumFacing facing2 = EnumFacing.WEST;
    protected BlockPos posOut2;

    public TileEntitySplitter()
    {
        this(ReferenceNames.NAME_TILE_ENTITY_SPLITTER);
    }

    public TileEntitySplitter(String name)
    {
        super(name);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInputManual   = new ItemStackHandlerTileEntity(9, 1,  1, false, "InputItems", this);
        this.inventoryReset         = new ItemStackHandlerTileEntity(0, 4,  1, false, "ResetItems", this);
        this.inventorySequence      = new ItemStackHandlerTileEntity(1, 2, 64, false, "SequenceItems", this);
        this.inventoryOut1          = new ItemStackHandlerTileEntity(2, 1,  1, false, "OutItems1", this);
        this.inventoryOut2          = new ItemStackHandlerTileEntity(3, 1,  1, false, "OutItems2", this);
        this.itemHandlerBase        = this.inventoryOut1;

        this.wrappedInventoryOut1   = new ItemHandlerWrapperExtractOnly(this.inventoryOut1);
        this.wrappedInventoryOut2   = new ItemHandlerWrapperExtractOnly(this.inventoryOut2);

        this.inventoryInput = new ItemHandlerWrapperFilter(
                this.inventoryReset,
                this.inventorySequence,
                this.inventoryOut1,
                this.inventoryOut2,
                this);
    }

    public IItemHandler getInputInventory()
    {
        return this.inventoryInputManual;
    }

    public IItemHandler getResetInventory()
    {
        return this.inventoryReset;
    }

    public IItemHandler getSequenceInventory()
    {
        return this.inventorySequence;
    }

    public IItemHandler getResetSequenceBuffer()
    {
        return this.inventoryInput.getSequenceBuffer();
    }

    public IItemHandler getOut1Inventory()
    {
        return this.inventoryOut1;
    }

    public IItemHandler getOut2Inventory()
    {
        return this.inventoryOut2;
    }

    public EnumFacing getOutputFacing2()
    {
        return this.facing2;
    }

    public void setSecondOutputSide(EnumFacing side)
    {
        if (side != this.facing)
        {
            this.facing2 = side;
            this.posOut2 = this.getPos().offset(side);
        }
    }

    /**
     * This returns the filter-out side's facing as what it would be if the non-match-out
     * side was North, which is the default rotation for the model.
     * That way the filter-out side's texture will be placed on the correct face
     * of the non-rotated model, before the primary facing's rotation is applied to the entire model.
     */
    public EnumFacing getSecondOutputRelativeFacing()
    {
        switch (this.facing)
        {
            // North is the default model rotation, don't modify the filter-out for this facing
            case NORTH: return this.facing2;
            case SOUTH:
                if (this.facing2.getAxis().isHorizontal())
                {
                    return this.facing2.getOpposite();
                }
                return this.facing2;

            default:
                EnumFacing axis = PositionUtils.getCWRotationAxis(EnumFacing.NORTH, this.facing).getOpposite();

                if (this.facing2.getAxis() != axis.getAxis())
                {
                    EnumFacing result = PositionUtils.rotateAround(this.facing2, axis);
                    //System.out.printf("facing: %s axis: %s filter: %s result: %s\n", facing, axis, facingFilteredOut, result);
                    return result;
                }

                return facing2;
        }
    }

    @Override
    public boolean onRightClickBlock(EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        ItemStack stack = player.getHeldItem(hand);

        if (stack.isEmpty() && player.isSneaking())
        {
            this.setSecondOutputSide(side);
            this.markDirty();
            this.notifyBlockUpdate(this.getPos());
            return true;
        }

        return false;
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        super.readFromNBTCustom(tag);

        // Setting the tier and thus initializing the inventories needs to
        // happen before reading the inventories!
        this.setSecondOutputSide(EnumFacing.getFront(tag.getByte("Facing2")));

        this.inventoryInputManual.deserializeNBT(tag);
        this.inventoryReset.deserializeNBT(tag);
        this.inventorySequence.deserializeNBT(tag);
        this.inventoryOut1.deserializeNBT(tag);
        this.inventoryOut2.deserializeNBT(tag);
        this.inventoryInput.deserializeNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("Facing2", (byte)this.facing2.getIndex());
        nbt.merge(this.inventoryInput.serializeNBT());

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

        nbt.merge(this.inventoryInputManual.serializeNBT());
        nbt.merge(this.inventoryReset.serializeNBT());
        nbt.merge(this.inventorySequence.serializeNBT());
        nbt.merge(this.inventoryOut1.serializeNBT());
        nbt.merge(this.inventoryOut2.serializeNBT());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        //tag.setByte("m", (byte)this.inventoryInput.getMode().getId());
        tag.setByte("f", (byte) ((this.facing2.getIndex() << 4) | this.getFacing().getIndex()));
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        //this.inventoryInput.setMode(ItemHandlerWrapperFilter.EnumMode.fromId(tag.getByte("m")));
        int facings = tag.getByte("f");
        this.setFacing(EnumFacing.getFront(facings & 0x7));
        this.setSecondOutputSide(EnumFacing.getFront((facings >>> 4) & 0x7));

        IBlockState state = this.getWorld().getBlockState(this.getPos());
        this.getWorld().notifyBlockUpdate(this.getPos(), state, state, 3);
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        this.pushItemsToAdjacentInventory(this.inventoryOut1, 0, this.posFront, this.facingOpposite, false);
        this.pushItemsToAdjacentInventory(this.inventoryOut2, 0, this.posOut2, this.facing2.getOpposite(), false);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return true;
        }

        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            if (facing == this.facing)
            {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.wrappedInventoryOut1);
            }

            if (facing == this.facing2)
            {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.wrappedInventoryOut2);
            }

            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.inventoryInput);
        }

        return super.getCapability(capability, facing);
    }

    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInputManual);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryReset);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventorySequence);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOut1);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOut2);
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        // Manual input inventory
        if (inventoryId == 9)
        {
            this.scheduleBlockUpdate(1, true);
        }
    }

    @Override
    public ContainerSplitter getContainer(EntityPlayer player)
    {
        return new ContainerSplitter(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiSplitter(this.getContainer(player), this);
    }
}
