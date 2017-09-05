package fi.dy.masa.autoverse.block.base;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
import fi.dy.masa.autoverse.event.RenderEventHandler;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.util.PositionUtils;

public abstract class BlockMachineSlimBase extends BlockAutoverseInventory
{
    //private static final AxisAlignedBB BOUNDS_SLIM_BASE_10 = new AxisAlignedBB(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.8125);
    private static final AxisAlignedBB BOUNDS_SLIM_BASE_12 = new AxisAlignedBB(0.125, 0.125, 0.125, 0.875, 0.875, 0.875);

    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x02_D = new AxisAlignedBB(0.25,  0.0,   0.25,  0.75,  0.125, 0.75 );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x02_U = new AxisAlignedBB(0.25,  0.875, 0.25,  0.75,  1.0,   0.75 );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x02_N = new AxisAlignedBB(0.25,  0.25,  0.0,   0.75,  0.75,  0.125);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x02_S = new AxisAlignedBB(0.25,  0.25,  0.875, 0.75,  0.75,  1.0  );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x02_W = new AxisAlignedBB(0.0,   0.25,  0.25,  0.125, 0.75,  0.75 );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x02_E = new AxisAlignedBB(0.875, 0.25,  0.25,  1.0,   0.75,  0.75 );

    /*
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_10_D = new AxisAlignedBB(0.1875, 0.0,    0.1875, 0.8125, 0.1875, 0.8125);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_10_U = new AxisAlignedBB(0.1875, 0.8125, 0.1875, 0.8125, 1.0,    0.8125);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_10_N = new AxisAlignedBB(0.1875, 0.1875, 0.0,    0.8125, 0.8125, 0.1875);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_10_S = new AxisAlignedBB(0.1875, 0.1875, 0.8125, 0.8125, 0.8125, 1.0   );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_10_W = new AxisAlignedBB(0.0,    0.1875, 0.1875, 0.1875, 0.8125, 0.8125);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_10_E = new AxisAlignedBB(0.8125, 0.1875, 0.1875, 1.0,    0.8125, 0.8125);
    */

    public static final PropertyBool SLIM = PropertyBool.create("slim");

    protected PropertyDirection propSideFacing0;
    protected int numModelSideFacings;
    protected boolean hasMainOutput;

    public BlockMachineSlimBase(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.hasMainOutput = true;
        this.createHilightBoxMap();
    }

    public boolean hasMainOutput()
    {
        return this.hasMainOutput;
    }

    public int getNumModelSideFacings()
    {
        return this.numModelSideFacings;
    }

    public PropertyDirection getPropertyFacing(int sideId)
    {
        return this.propSideFacing0;
    }

    public boolean isMainOutputOppositeToFacing()
    {
        return false;
    }

    @Override
    public ItemBlockAutoverse createItemBlock()
    {
        ItemBlockAutoverse item = new ItemBlockAutoverse(this);
        item.addPlacementProperty(OreDictionary.WILDCARD_VALUE, "machine.slim_model", Constants.NBT.TAG_BYTE, 0, 1);
        item.addPlacementPropertyValueNames(OreDictionary.WILDCARD_VALUE, "machine.slim_model", new String[] { "false", "true" });
        return item;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        state = super.getActualState(state, world, pos);

        TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

        if (te != null)
        {
            state = state.withProperty(SLIM, te.isSlimModel());
        }

        return state;
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
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess blockAccess, BlockPos pos)
    {
        return state.getActualState(blockAccess, pos).getValue(SLIM) ? BOUNDS_SLIM_BASE_12 : FULL_BLOCK_AABB;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
            AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean p_185477_7_)
    {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, state.getCollisionBoundingBox(world, pos));

        state = state.getActualState(world, pos);

        if (state.getValue(SLIM))
        {
            this.addAllSideCollisionBoxes(state, pos, entityBox, collidingBoxes);
        }
    }

    protected void addAllSideCollisionBoxes(IBlockState actualState, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes)
    {
        EnumFacing facing = actualState.getValue(FACING);

        if (this.hasMainOutput())
        {
            this.addSideCollisionBox(pos, entityBox, collidingBoxes, this.isMainOutputOppositeToFacing() ? facing.getOpposite() : facing);
        }

        for (int sideIndex = 0; sideIndex < this.getNumModelSideFacings(); sideIndex++)
        {
            EnumFacing side = PositionUtils.getAbsoluteFacingFromNorth(facing, actualState.getValue(this.getPropertyFacing(sideIndex)));
            this.addSideCollisionBox(pos, entityBox, collidingBoxes, side);
        }
    }

    protected void addSideCollisionBox(BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, EnumFacing side)
    {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, this.getBoundsForSide(side));
    }

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
        return collisionRayTraceToBoxes(state, this, world, pos, start, end);
    }

    @Override
    public void updateBlockHilightBoxes(IBlockState actualState, World world, BlockPos pos)
    {
        Map<Integer, AxisAlignedBB> boxMap = this.getHilightBoxMap();
        boxMap.clear();

        if (actualState.getValue(SLIM))
        {
            final EnumFacing mainFacing = actualState.getValue(FACING);
            boxMap.put(BOX_ID_MAIN, BOUNDS_SLIM_BASE_12.offset(pos));

            if (this.hasMainOutput())
            {
                EnumFacing side = mainFacing;

                if (this.isMainOutputOppositeToFacing())
                {
                    side = side.getOpposite();
                }

                boxMap.put(side.getIndex(), this.getBoundsForSide(side).offset(pos));
            }

            for (int sideIndex = 0; sideIndex < this.getNumModelSideFacings(); sideIndex++)
            {
                EnumFacing side = PositionUtils.getAbsoluteFacingFromNorth(mainFacing, actualState.getValue(this.getPropertyFacing(sideIndex)));
                boxMap.put(side.getIndex(), this.getBoundsForSide(side).offset(pos));
            }
        }
        else
        {
            boxMap.put(BOX_ID_MAIN, FULL_BLOCK_AABB.offset(pos));
        }
    }

    private AxisAlignedBB getBoundsForSide(EnumFacing side)
    {
        switch (side)
        {
            case DOWN:  return BOUNDS_SLIM_SIDE_08x02_D;
            case UP:    return BOUNDS_SLIM_SIDE_08x02_U;
            case NORTH: return BOUNDS_SLIM_SIDE_08x02_N;
            case SOUTH: return BOUNDS_SLIM_SIDE_08x02_S;
            case WEST:  return BOUNDS_SLIM_SIDE_08x02_W;
            case EAST:  return BOUNDS_SLIM_SIDE_08x02_E;
        }

        return PositionUtils.ZERO_BB;
    }
}
