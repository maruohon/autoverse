package fi.dy.masa.autoverse.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
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
    private <T extends Comparable<T>> IBlockState getFinalPlacementState(IBlockState state)
    {
        //System.out.printf("Original state: %s\n", state);
        final int propCount = this.placer.getPropertyCount();
        List<String> propNamesFacing = new ArrayList<String>();
        List<String> propNamesInteger = new ArrayList<String>();

        for (Entry <IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet())
        {
            IProperty<T> property = (IProperty<T>) entry.getKey();

            if (property instanceof PropertyDirection)
            {
                propNamesFacing.add(property.getName());
            }
            else if (property instanceof PropertyInteger)
            {
                propNamesInteger.add(property.getName());
            }
        }

        Collections.sort(propNamesFacing);
        Collections.sort(propNamesInteger);

        // Facing properties first, then integer properties
        List<String> propNames = new ArrayList<String>();
        propNames.addAll(propNamesFacing);
        propNames.addAll(propNamesInteger);

        for (int i = 0; i < propCount && i < propNames.size(); i++)
        {
            int value = this.placer.getPropertyValue(i);

            if (value != -1)
            {
                String propName = propNames.get(i);

                for (Entry <IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet())
                {
                    IProperty<T> property = (IProperty<T>) entry.getKey();

                    if (propName.equals(property.getName()))
                    {
                        EnumFacing facing = EnumFacing.getFront(value);
                        Integer intValue = Integer.valueOf(value);

                        if ((property instanceof PropertyDirection) && property.getAllowedValues().contains(facing))
                        {
                            //System.out.printf("Setting property %s to %s\n", propName, facing);
                            state = state.withProperty(property, (T) facing);
                            break;
                        }
                        // TODO: Add some kind of property white listing system, since this may allow exploits!
                        else if ((property instanceof PropertyInteger) && property.getAllowedValues().contains(intValue))
                        {
                            //System.out.printf("Setting property %s to %d\n", propName, intValue);
                            state = state.withProperty(property, (T) intValue);
                            break;
                        }
                    }
                }
            }
        }

        //System.out.printf("Final state: %s\n", state);
        return state;
    }

    private void applyTileEntityProperties(World world, BlockPos pos)
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
    }

    /**
     * Tries to place a block.
     * @param stack
     * @return true if the placement succeeded and the item should be used
     */
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
                IBlockState placementState = this.getFinalPlacementState(stateInitial);

                if (world.mayPlace(placementState.getBlock(), pos, true, EnumFacing.UP, null) &&
                    BlockUtils.setBlockStateWithPlaceSound(world, pos, placementState, 3))
                {
                    this.applyTileEntityProperties(world, pos);
                    return true;
                }
            }
            // Not an ItemBlock, try to use the item instead
            else
            {
                return this.tryUseItem(world, pos, stack);
            }
        }

        return false;
    }

    private boolean tryUseItem(World world, BlockPos pos, ItemStack stack)
    {
        EntityPlayer player = this.getPlayer();
        IBlockState stateOriginal = world.getBlockState(pos);

        player.setPosition(pos.getX(), pos.getY() + 1, pos.getZ());
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        EnumActionResult result = stack.onItemUse(player, world, pos, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.0f, 0.5f);

        if (result == EnumActionResult.SUCCESS)
        {
            IBlockState stateAfter = world.getBlockState(pos);

            // The block changed as a result of the item use, try to apply the properties
            if (stateOriginal != stateAfter)
            {
                IBlockState stateFinal = this.getFinalPlacementState(stateAfter);

                if (stateFinal != stateAfter)
                {
                    world.setBlockState(pos, stateFinal);
                }

                // We won't try to apply TileEntity properties here, because only Autoverse TEs are supported,
                // and all Autoverse blocks have ItemBlocks, which are placed by the other method.
                return true;
            }
        }
        /*
        else
        {
            System.out.printf("item use failed: %s\n", result);
        }
        */

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

    @Override
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
