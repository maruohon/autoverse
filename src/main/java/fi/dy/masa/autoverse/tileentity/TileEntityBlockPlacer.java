package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiBlockPlacer;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBlockPlacer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperSelective;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.BlockUtils;
import fi.dy.masa.autoverse.util.PositionUtils;
import fi.dy.masa.autoverse.util.TileUtils;

public class TileEntityBlockPlacer extends TileEntityAutoverseInventory
{
    public static final int MAX_LENGTH = 32;
    private int position;
    private int delay = 4;
    private EnumFacing facingHorizontal = BlockAutoverse.DEFAULT_FACING;
    private State state = State.IDLE;

    public TileEntityBlockPlacer()
    {
        super(ReferenceNames.NAME_BLOCK_PLACER);

        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, MAX_LENGTH, 1, false, "Items", this);
        this.itemHandlerExternal = new ItemHandlerWrapperPlacer(this.itemHandlerBase);
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
    public void rotate(Rotation rotationIn)
    {
        super.rotate(rotationIn);

        this.setHorizontalFacing(rotationIn.rotate(this.facingHorizontal));
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
                this.delay = value;
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private IBlockState getPlacementStateForPosition(int position, World world, BlockPos pos, FakePlayer player)
    {
        ItemStack stack = this.itemHandlerBase.getStackInSlot(position);

        if (stack.isEmpty() == false)
        {
            NBTTagCompound nbt = stack.getTagCompound();

            if (nbt != null && nbt.hasKey("PlacementState", Constants.NBT.TAG_COMPOUND))
            {
                NBTTagCompound tag = nbt.getCompoundTag("PlacementState");
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(tag.getString("name")));

                if (block != null && block != Blocks.AIR)
                {
                    IBlockState state = block.getStateFromMeta(tag.getByte("meta"));
                    // This is the facing of the Block State Reader when it read this block state from the world.
                    // We will now apply a relative rotation based on that facing, and the Placer's horizontal facing.
                    EnumFacing facingOriginal = EnumFacing.getFront(tag.getByte("ReadFacing"));
                    Rotation rotation = PositionUtils.getRotation(facingOriginal, this.facingHorizontal);
                    return state.withRotation(rotation);
                }
            }
            else if (stack.getItem() instanceof ItemBlock)
            {
                ItemBlock itemBlock = (ItemBlock) stack.getItem();
                int meta = itemBlock.getMetadata(stack.getMetadata());
                player.rotationYaw = this.facingHorizontal.getHorizontalAngle();

                return itemBlock.getBlock().getStateForPlacement(world, pos, EnumFacing.UP, 0.5f, 1f, 0.5f, meta, player);
            }
        }

        return null;
    }

    @Nullable
    private NBTTagCompound getPlacementTileNBT(ItemStack stack)
    {
        // This fixes TE data loss on the placed blocks in case blocks with stored TE data
        // were manually placed into the slots, and not taken from the world by the drawbridge
        if (stack.isEmpty() == false && stack.getTagCompound() != null &&
            stack.getTagCompound().hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
        {
            return stack.getTagCompound().getCompoundTag("BlockEntityTag");
        }

        return null;
    }

    private boolean extendOneBlock(int position, FakePlayer player, boolean playPistonSoundInsteadOfPlaceSound)
    {
        World world = this.getWorld();
        BlockPos pos = this.getPos().offset(this.getFacing(), position + 1);
        ItemStack stack = this.itemHandlerBase.getStackInSlot(position);

        if (stack.isEmpty() == false &&
            world.isBlockLoaded(pos, true) &&
            world.getBlockState(pos).getBlock().isReplaceable(world, pos))
        {
            IBlockState placementState = this.getPlacementStateForPosition(position, world, pos, player);

            if (placementState != null &&
                world.mayPlace(placementState.getBlock(), pos, true, EnumFacing.UP, null) &&
                ((playPistonSoundInsteadOfPlaceSound && world.setBlockState(pos, placementState)) ||
                 (playPistonSoundInsteadOfPlaceSound == false && BlockUtils.setBlockStateWithPlaceSound(world, pos, placementState, 3))))
            {
                NBTTagCompound nbt = this.getPlacementTileNBT(stack);

                // Consume the item
                this.itemHandlerBase.extractItem(position, 1, false);

                if (nbt != null && placementState.getBlock().hasTileEntity(placementState))
                {
                    TileUtils.createAndAddTileEntity(world, pos, nbt);
                }

                if (playPistonSoundInsteadOfPlaceSound)
                {
                    world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5f, 0.8f);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        boolean powered = false;

        for (EnumFacing facing : EnumFacing.values())
        {
            if (facing != this.getFacing() && worldIn.isSidePowered(pos.offset(facing), facing))
            {
                powered = true;
                break;
            }
        }

        if (powered != this.redstoneState)
        {
            this.redstoneState = powered;
            this.state = powered ? State.PLACE : State.IDLE;

            if (powered)
            {
                this.scheduleBlockUpdate(this.delay, true);
            }
            else
            {
                this.position = 0;
            }
        }
    }

    @Override
    public void onScheduledBlockUpdate(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (this.state == State.PLACE)
        {
            while (this.position < MAX_LENGTH)
            {
                boolean result = this.extendOneBlock(this.position, this.getPlayer(), true);
                this.position++;

                if (result)
                {
                    break;
                }
            }

            if (this.position >= MAX_LENGTH)
            {
                this.position = 0;
            }
            else
            {
                this.scheduleBlockUpdate(this.delay, false);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);
        nbt.setByte("Position", (byte) this.position);
        nbt.setByte("State", (byte) this.state.getId());
        nbt.setByte("FacingHorizontal", (byte) this.facingHorizontal.getIndex());
        nbt.setByte("Delay", (byte) this.delay);
        return nbt;
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.setHorizontalFacing(EnumFacing.getFront(nbt.getByte("FacingHorizontal")));
        this.position = nbt.getByte("Position");
        this.state = State.fromId(nbt.getByte("State"));
        this.delay = ((int) nbt.getByte("Delay")) & 0xFF;
    }

    private class ItemHandlerWrapperPlacer extends ItemHandlerWrapperSelective
    {
        public ItemHandlerWrapperPlacer(IItemHandler baseHandler)
        {
            super(baseHandler);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            int sizeOrig = stack.getCount();
            stack = super.insertItem(slot, stack, simulate);

            if (simulate == false && (stack.isEmpty() || stack.getCount() < sizeOrig))
            {
                TileEntityBlockPlacer.this.scheduleBlockUpdate(TileEntityBlockPlacer.this.delay, false);
            }

            return stack;
        }
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return new ContainerBlockPlacer(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiBlockPlacer(this.getContainer(player), this);
    }

    private enum State
    {
        IDLE    (0),
        PLACE   (1);

        private final int id;

        private State(int id)
        {
            this.id = id;
        }

        public int getId()
        {
            return id;
        }

        public static State fromId(int id)
        {
            return values()[id % values().length];
        }
    }
}
