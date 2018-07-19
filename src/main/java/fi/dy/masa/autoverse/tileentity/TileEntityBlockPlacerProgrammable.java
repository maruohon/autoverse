package fi.dy.masa.autoverse.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.config.Configs;
import fi.dy.masa.autoverse.gui.client.GuiBlockPlacerProgrammable;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBlockPlacerProgrammable;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperPlacerProgrammable;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.BlockUtils;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityBlockPlacerProgrammable extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemHandlerWrapperPlacerProgrammable placer;
    private int placementOffset;
    private EnumFacing facingHorizontal = BlockAutoverse.DEFAULT_FACING;

    public TileEntityBlockPlacerProgrammable()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_PLACER_PROGRAMMABLE);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput     = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutput    = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut", this);
        this.placer             = new ItemHandlerWrapperPlacerProgrammable(this.inventoryInput, this.inventoryOutput, this);
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
                this.setHorizontalFacing(EnumFacing.byIndex(value));
                return true;

            case 2:
                this.setPlacementOffset(value);
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    @Override
    public int[] getProperties()
    {
        int[] values = super.getProperties();

        values[1] = this.facingHorizontal.getIndex();
        values[2] = this.placementOffset;

        return values;
    }

    public void setHorizontalFacing(EnumFacing facing)
    {
        if (facing.getAxis().isHorizontal())
        {
            this.facingHorizontal = facing;
        }
    }

    @Override
    public void setFacing(EnumFacing facing)
    {
        if (facing.getAxis().isHorizontal())
        {
            this.facingHorizontal = facing;
        }

        super.setFacing(facing);
    }

    @Override
    public void rotate(Rotation rotationIn)
    {
        this.setHorizontalFacing(rotationIn.rotate(this.facingHorizontal));
        super.rotate(rotationIn);
    }

    public void setPlacementOffset(int offset)
    {
        this.placementOffset = MathHelper.clamp(offset, 0, 15);
        this.markDirty();
    }

    private BlockPos getPlacementPosition()
    {
        return this.getPos().offset(this.getFacing(), this.placementOffset + 1);
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

        this.setHorizontalFacing(EnumFacing.byIndex(nbt.getByte("FacingHorizontal")));
        this.setPlacementOffset(nbt.getByte("PlacementOffset"));
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        this.inventoryInput.deserializeNBT(nbt);
        this.inventoryOutput.deserializeNBT(nbt);
        this.placer.deserializeNBT(nbt);
    }

    @Nullable
    private IBlockState getPlacementStateForPosition(ItemStack stack, World world, BlockPos pos, EntityPlayer player)
    {
        if (stack.isEmpty() == false && stack.getItem() instanceof ItemBlock)
        {
            ItemBlock itemBlock = (ItemBlock) stack.getItem();
            int meta = itemBlock.getMetadata(stack.getMetadata());
            player.rotationYaw = this.facingHorizontal.getHorizontalAngle();

            return itemBlock.getBlock().getStateForPlacement(world, pos, EnumFacing.UP, 0.5f, 1f, 0.5f, meta, player, EnumHand.MAIN_HAND);
        }

        return null;
    }

    public static List<String> getConfigurableBlockPropertiesSorted(IBlockState state)
    {
        List<String> propNamesFacing = new ArrayList<String>();
        List<String> propNamesInteger = new ArrayList<String>();

        for (Entry <IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet())
        {
            IProperty<?> property = entry.getKey();

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

        return propNames;
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> IBlockState getFinalPlacementState(IBlockState state)
    {
        final int propCount = this.placer.getPropertyCount();
        List<String> propNames = getConfigurableBlockPropertiesSorted(state);

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
                        if (property instanceof PropertyDirection)
                        {
                            EnumFacing facing = EnumFacing.byIndex(value);

                            if (property.getAllowedValues().contains(facing))
                            {
                                state = state.withProperty(property, (T) facing);
                                break;
                            }
                        }
                        else if (property instanceof PropertyInteger && Configs.isBlockWhitelistedForIntegerProperties(state.getBlock()))
                        {
                            Integer intValue = Integer.valueOf(value);

                            if (property.getAllowedValues().contains(intValue))
                            {
                                state = state.withProperty(property, (T) intValue);
                                break;
                            }
                        }
                    }
                }
            }
        }

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
                int value = this.placer.getPropertyValue(i);

                if (value != -1)
                {
                    ((TileEntityAutoverse) te).applyProperty(i, value);
                }
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
        BlockPos pos = this.getPlacementPosition();

        if (world.isBlockLoaded(pos, true) &&
            world.getBlockState(pos).getBlock().isReplaceable(world, pos))
        {
            EntityPlayer player = this.getPlayer();
            EntityUtils.setHeldItemWithoutEquipSound(player, EnumHand.MAIN_HAND, stack);
            IBlockState stateInitial = this.getPlacementStateForPosition(stack, world, pos, player);
            EntityUtils.setHeldItemWithoutEquipSound(player, EnumHand.MAIN_HAND, ItemStack.EMPTY);

            if (stateInitial != null)
            {
                IBlockState placementState = this.getFinalPlacementState(stateInitial);

                if (world.mayPlace(placementState.getBlock(), pos, true, EnumFacing.UP, null) &&
                    BlockUtils.setBlockStateWithPlaceSound(world, pos, placementState, 3))
                {
                    this.applyTileEntityProperties(world, pos);
                    stack.shrink(1);
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
        EntityUtils.setHeldItemWithoutEquipSound(player, EnumHand.MAIN_HAND, stack);
        EnumActionResult result = stack.onItemUse(player, world, pos.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 1.0f, 0.5f);
        EntityUtils.setHeldItemWithoutEquipSound(player, EnumHand.MAIN_HAND, ItemStack.EMPTY);

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
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded(false);
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        this.scheduleUpdateIfNeeded(false);
    }

    @Override
    public void onScheduledBlockUpdate(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (this.placer.moveItems())
        {
            this.scheduleUpdateIfNeeded(true);
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        this.scheduleUpdateIfNeeded(true);
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
        this.placer.dropAllItems(this.getWorld(), this.getPos());
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return new ContainerBlockPlacerProgrammable(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiBlockPlacerProgrammable(this.getContainer(player), this);
    }
}
