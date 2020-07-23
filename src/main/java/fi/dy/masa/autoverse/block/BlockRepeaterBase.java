package fi.dy.masa.autoverse.block;

import java.util.EnumSet;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import fi.dy.masa.autoverse.block.base.BlockAutoverseTileEntity;
import fi.dy.masa.autoverse.event.RenderEventHandler;
import fi.dy.masa.autoverse.tileentity.TileEntityRepeaterBase;

public abstract class BlockRepeaterBase extends BlockAutoverseTileEntity
{
    protected static final AxisAlignedBB BOUNDS_ROD_DOWN  = new AxisAlignedBB(0.3125, 0.0625, 0.3125, 0.6875, 0.25, 0.6875);
    protected static final AxisAlignedBB BOUNDS_ROD_UP    = new AxisAlignedBB(0.3125,   0.75, 0.3125, 0.6875, 0.9375, 0.6875);
    protected static final AxisAlignedBB BOUNDS_ROD_NORTH = new AxisAlignedBB(0.3125, 0.3125, 0.0625, 0.6875, 0.6875,   0.25);
    protected static final AxisAlignedBB BOUNDS_ROD_SOUTH = new AxisAlignedBB(0.3125, 0.3125,   0.75, 0.6875, 0.6875, 0.9375);
    protected static final AxisAlignedBB BOUNDS_ROD_WEST  = new AxisAlignedBB(0.0625, 0.3125, 0.3125,   0.25, 0.6875, 0.6875);
    protected static final AxisAlignedBB BOUNDS_ROD_EAST  = new AxisAlignedBB(  0.75, 0.3125, 0.3125, 0.9375, 0.6875, 0.6875);

    protected static final AxisAlignedBB BOUNDS_OUT_DOWN  = new AxisAlignedBB(0.3125,    0.0, 0.3125, 0.6875,   0.25, 0.6875);
    protected static final AxisAlignedBB BOUNDS_OUT_UP    = new AxisAlignedBB(0.3125,   0.75, 0.3125, 0.6875,    1.0, 0.6875);
    protected static final AxisAlignedBB BOUNDS_OUT_NORTH = new AxisAlignedBB(0.3125, 0.3125,    0.0, 0.6875, 0.6875,   0.25);
    protected static final AxisAlignedBB BOUNDS_OUT_SOUTH = new AxisAlignedBB(0.3125, 0.3125,   0.75, 0.6875, 0.6875,    1.0);
    protected static final AxisAlignedBB BOUNDS_OUT_WEST  = new AxisAlignedBB(   0.0, 0.3125, 0.3125,   0.25, 0.6875, 0.6875);
    protected static final AxisAlignedBB BOUNDS_OUT_EAST  = new AxisAlignedBB(  0.75, 0.3125, 0.3125,    1.0, 0.6875, 0.6875);

    protected static final AxisAlignedBB BOUNDS_BULGE = new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

    public BlockRepeaterBase(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);
    }

    @Override
    public boolean hasSpecialHitbox()
    {
        return true;
    }

    @Override
    protected EnumFacing getPlacementFacing(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        // Retain the facing from getStateForPlacement
        return state.getValue(FACING);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state)
    {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
    {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean isNormalCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return false;
    }

    @Override
    public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
    {
        return false;
    }

    @Override
    public boolean isTopSolid(IBlockState state)
    {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess blockAccess, BlockPos pos)
    {
        return BOUNDS_BULGE;
    }

    @Override
    public boolean getWeakChanges(IBlockAccess world, BlockPos pos)
    {
        return true;
    }

    @Override
    public boolean canProvidePower(IBlockState state)
    {
        return true;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EnumFacing side)
    {
        return state.getActualState(world, pos).getValue(FACING_OUT).getOpposite() == side;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return this.getWeakPower(state, blockAccess, pos, side);
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        state = state.getActualState(blockAccess, pos);

        if (side == state.getValue(FACING_OUT).getOpposite())
        {
            TileEntityRepeaterBase te = getTileEntitySafely(blockAccess, pos, TileEntityRepeaterBase.class);
            return te != null && te.getOutputStrength() >= 0 ? te.getOutputStrength() : 0;
        }

        return 0;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state)
    {
        super.onBlockAdded(world, pos, state);

        // FIXME: This actually doesn't work, the TileEntity is not there yet and the facing hasn't been set yet...
        //state = state.getActualState(world, pos);
        //this.updateState(state, world, pos);

        // So instead schedule an update from which the outputs will get updated after all the data is present...
        world.scheduleUpdate(pos, state.getBlock(), 2);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        state = state.getActualState(world, pos);
        notifyOutputs(state, world, pos, state.getValue(FACING_OUT));

        world.removeTileEntity(pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        this.scheduleRepeaterUpdate(state, world, pos);
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor)
    {
        if (blockAccess instanceof World)
        {
            World world = (World) blockAccess;
            this.scheduleRepeaterUpdate(world.getBlockState(pos), world, pos);
        }
    }

    protected void scheduleRepeaterUpdate(IBlockState state, World world, BlockPos pos)
    {
        this.scheduleBlockUpdate(world, pos, state, 2, false);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntityRepeaterBase te = getTileEntitySafely(world, pos, TileEntityRepeaterBase.class);

        if (te != null)
        {
            te.setOutputFacing(te.getFacing().getOpposite());
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand)
    {
        if (world.isRemote == false)
        {
            this.updateState(state.getActualState(world, pos), world, pos);
        }
    }

    @Override
    public int tickRate(World world)
    {
        return 2;
    }

    protected abstract void updateState(IBlockState actualState, World world, BlockPos pos);

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos)
    {
        AxisAlignedBB bb = RenderEventHandler.getInstance().getPointedHilightBox(this);

        if (bb != null)
        {
            return bb;
        }

        return state.getBoundingBox(worldIn, pos).offset(pos);
    }

    @Override
    @Nullable
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end)
    {
        state = state.getActualState(world, pos);
        return collisionRayTraceToBoxes(state, this, world, pos, start, end);
    }

    public static void notifyOutputs(IBlockState state, World world, BlockPos pos, EnumFacing outputSide)
    {
        if (ForgeEventFactory.onNeighborNotify(world, pos, world.getBlockState(pos), EnumSet.of(outputSide), false).isCanceled())
        {
            return;
        }

        BlockPos neighborPos = pos.offset(outputSide);
        world.neighborChanged(neighborPos, state.getBlock(), pos);
        world.notifyNeighborsOfStateExcept(neighborPos, state.getBlock(), outputSide.getOpposite());
    }
}
