package fi.dy.masa.autoverse.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiPlacerProgrammable;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerPlacerProgrammable;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperPlacerProgrammable;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.BlockUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityPlacerProgrammable extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemHandlerWrapperPlacerProgrammable placer;
    private BlockPos placementPosition = BlockPos.ORIGIN;
    private int placementOffset = 1;
    private EnumFacing facingHorizontal = BlockAutoverse.DEFAULT_FACING;

    public TileEntityPlacerProgrammable()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_PLACER_PROGRAMMABLE);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput     = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutput    = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut", this);
        this.placer             = new ItemHandlerWrapperPlacerProgrammable(4, this.inventoryInput, this.inventoryOutput, this);
        this.itemHandlerBase    = this.inventoryInput;
        this.itemHandlerExternal = this.placer;
    }

    public IItemHandler getInventoryIn()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOut()
    {
        return this.inventoryOutput;
    }

    public ItemHandlerWrapperPlacerProgrammable getPlacerHandler()
    {
        return this.placer;
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 1:
                this.setHorizontalFacing(EnumFacing.getFront(value));
                return true;

            case 2:
                this.placementOffset = value;
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    public void setHorizontalFacing(EnumFacing facing)
    {
        if (facing.getAxis().isHorizontal())
        {
            this.facingHorizontal = facing;
            this.markDirty();
        }
    }

    @Override
    public void setFacing(EnumFacing facing)
    {
        super.setFacing(facing);

        this.updatePlacementPosition();
    }

    @Override
    public void rotate(Rotation rotationIn)
    {
        super.rotate(rotationIn);

        this.setHorizontalFacing(rotationIn.rotate(this.facingHorizontal));
        this.updatePlacementPosition();
    }

    @Override
    public void setPos(BlockPos posIn)
    {
        super.setPos(posIn);

        this.updatePlacementPosition();
    }

    private void updatePlacementPosition()
    {
        this.placementPosition = this.getPos().offset(this.getFacing(), this.placementOffset);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);
        nbt.setByte("FacingHorizontal", (byte) this.facingHorizontal.getIndex());
        nbt.setByte("PlacementOffset", (byte) this.placementOffset);
        return nbt;
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        nbt.merge(this.inventoryInput.serializeNBT());
        nbt.merge(this.inventoryOutput.serializeNBT());
        nbt.merge(this.placer.serializeNBT());
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.placementOffset = nbt.getByte("PlacementOffset");
        this.setHorizontalFacing(EnumFacing.getFront(nbt.getByte("FacingHorizontal")));
        this.updatePlacementPosition();
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        this.inventoryInput.deserializeNBT(nbt);
        this.inventoryOutput.deserializeNBT(nbt);
        this.placer.deserializeNBT(nbt);
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private IBlockState getPlacementStateForPosition(ItemStack stack, World world, BlockPos pos, EntityPlayer player)
    {
        if (stack.isEmpty() == false)
        {
            if (stack.getItem() instanceof ItemBlock)
            {
                ItemBlock itemBlock = (ItemBlock) stack.getItem();
                int meta = itemBlock.getMetadata(stack.getMetadata());
                player.rotationYaw = this.facingHorizontal.getHorizontalAngle();

                return itemBlock.block.getStateForPlacement(world, pos, EnumFacing.UP, 0.5f, 1f, 0.5f, meta, player);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> IBlockState getModifiedPlacementState(IBlockState state)
    {
        final int propCount = this.placer.getPropertyCount();
        List<String> propNames = new ArrayList<String>();

        for (Entry <IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet())
        {
            IProperty<T> property = (IProperty<T>) entry.getKey();

            if (property instanceof PropertyDirection)
            {
                propNames.add(property.getName());
            }
        }

        Collections.sort(propNames);

        for (int i = 0; i < propCount && i < propNames.size(); i++)
        {
            int value = this.placer.getPropertyValue(i);

            if (value != -1)
            {
                EnumFacing facing = EnumFacing.getFront(value);
                String propName = propNames.get(i);

                for (Entry <IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet())
                {
                    IProperty<T> property = (IProperty<T>) entry.getKey();

                    if (propName.equals(property.getName()) && property.getAllowedValues().contains(facing))
                    {
                        state = state.withProperty(property, (T) facing);
                    }
                }
            }
        }

        return state;
    }

    public boolean tryPlaceBlock(ItemStack stack)
    {
        World world = this.getWorld();
        BlockPos pos = this.placementPosition;

        if (world.isBlockLoaded(pos, true) &&
            world.getBlockState(pos).getBlock().isReplaceable(world, pos))
        {
            EntityPlayer player = this.getPlayer();
            IBlockState stateInitial = this.getPlacementStateForPosition(stack, world, pos, player);

            if (stateInitial != null)
            {
                IBlockState placementState = this.getModifiedPlacementState(stateInitial);

                if (world.mayPlace(placementState.getBlock(), pos, true, EnumFacing.UP, null) &&
                    BlockUtils.setBlockStateWithPlaceSound(world, pos, placementState, 3))
                {
                    TileEntity te = world.getTileEntity(pos);

                    if (te instanceof TileEntityAutoverse)
                    {
                        int count = this.placer.getPropertyCount();

                        for (int i = 0; i < count; i++)
                        {
                            ((TileEntityAutoverse) te).applyProperty(i, this.placer.getPropertyValue(i));
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onScheduledBlockUpdate(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (this.placer.moveItems())
        {
            this.scheduleUpdateIfNeeded();
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
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

    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutput);
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return new ContainerPlacerProgrammable(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiPlacerProgrammable(this.getContainer(player), this);
    }
}
