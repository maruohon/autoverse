package fi.dy.masa.autoverse.block.base;

import java.util.Random;
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
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.EntityUtils;

public abstract class BlockAutoverseTileEntity extends BlockAutoverse
{
    protected boolean getFacingFromTE;

    public BlockAutoverseTileEntity(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.hasFacing = true;
        this.getFacingFromTE = true;
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
    public int tickRate(World world)
    {
        return 1;
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random)
    {
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state)
    {
        TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

        if (te != null)
        {
            te.updateRedstoneState(false);
        }
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
            side = this.getTargetedSide(world, pos, state, side, player);

            if (te.onRightClickBlock(world, pos, state, side, player, hand, hitX, hitY, hitZ))
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
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player)
    {
        if (world.isRemote == false)
        {
            TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

            if (te != null)
            {
                RayTraceResult trace = EntityUtils.getRayTraceFromEntity(world, player, false);

                if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK && trace.getBlockPos().equals(pos))
                {
                    EnumFacing side = this.getTargetedSide(world, pos, world.getBlockState(pos), trace.sideHit, player);
                    te.onLeftClickBlock(world, pos, side, player);
                }
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
        if (this.getFacingFromTE)
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
