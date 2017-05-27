package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public abstract class BlockAutoverseTileEntity extends BlockAutoverse
{
    protected boolean hasFacing;

    public BlockAutoverseTileEntity(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.hasFacing = true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return this.createTileEntityInstance(world, state);
    }

    protected abstract TileEntityAutoverse createTileEntityInstance(World world, IBlockState state);

    public boolean isTileEntityValid(TileEntity te)
    {
        return te != null && te.isInvalid() == false;
    }

    protected EnumFacing getPlacementFacing(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        return EnumFacing.getDirectionFromEntityLiving(pos, placer);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

        if (te != null)
        {
            NBTTagCompound nbt = stack.getTagCompound();

            // If the ItemStack has a tag containing saved TE data, restore it to the just placed block/TE
            if (nbt != null && nbt.hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
            {
                te.readFromNBTCustom(nbt.getCompoundTag("BlockEntityTag"));
            }
            else
            {
                if (te instanceof TileEntityAutoverseInventory && stack.hasDisplayName())
                {
                    ((TileEntityAutoverseInventory) te).setInventoryName(stack.getDisplayName());
                }
            }

            // This will also call markDirty()
            te.setFacing(this.getPlacementFacing(world, pos, state, placer, stack));

            // This is to fix the modular inventories not loading properly when placed from a Ctrl + pick-blocked stack
            te.onLoad();
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
            EnumFacing side, float hitX, float hitY, float hitZ)
    {
        TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

        if (te != null && this.isTileEntityValid(te))
        {
            if (te.onRightClickBlock(player, hand, side, hitX, hitY, hitZ))
            {
                return true;
            }
            else if (te.hasGui())
            {
                if (world.isRemote == false)
                {
                    player.openGui(Autoverse.instance, ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC, world, pos.getX(), pos.getY(), pos.getZ());
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer playerIn)
    {
        if (world.isRemote == false)
        {
            TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

            if (te != null)
            {
                te.onLeftClickBlock(playerIn);
            }
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        if (world.isRemote == false)
        {
            TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

            if (te != null)
            {
                te.onNeighborBlockChange(world, pos, state, blockIn);
            }
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        if (world instanceof World && ((World) world).isRemote == false)
        {
            TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

            if (te != null)
            {
                te.onNeighborTileChange(world, pos, neighbor);
            }
        }
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        if (this.hasFacing)
        {
            TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

            if (te != null)
            {
                state = state.withProperty(FACING, te.getFacing());
            }
        }

        return state;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation)
    {
        return this.hasFacing ? state.withProperty(FACING, rotation.rotate(state.getValue(FACING))) : state;
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror)
    {
        return this.hasFacing ? state.withRotation(mirror.toRotation(state.getValue(FACING))) : state;
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis)
    {
        TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

        if (te != null)
        {
            te.rotate(Rotation.CLOCKWISE_90);
            IBlockState state = world.getBlockState(pos).getActualState(world, pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            return true;
        }

        return false;
    }
}
