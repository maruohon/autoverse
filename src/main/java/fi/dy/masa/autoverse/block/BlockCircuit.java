package fi.dy.masa.autoverse.block;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import fi.dy.masa.autoverse.block.base.BlockAutoverseTileEntity;
import fi.dy.masa.autoverse.event.RenderEventHandler;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityLatch;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.util.PositionUtils;

public class BlockCircuit extends BlockAutoverseTileEntity
{
    private static final AxisAlignedBB BOUNDS_BASE_08 = new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

    private static final AxisAlignedBB BOUNDS_SIDE_06x04_D = new AxisAlignedBB(0.3125, 0.0,    0.3125, 0.6875, 0.25,   0.6875);
    private static final AxisAlignedBB BOUNDS_SIDE_06x04_U = new AxisAlignedBB(0.3125, 0.75,   0.3125, 0.6875, 1.0,    0.6875);
    private static final AxisAlignedBB BOUNDS_SIDE_06x04_N = new AxisAlignedBB(0.3125, 0.3125, 0.0,    0.6875, 0.6875, 0.25  );
    private static final AxisAlignedBB BOUNDS_SIDE_06x04_S = new AxisAlignedBB(0.3125, 0.3125, 0.75,   0.6875, 0.6875, 1.0   );
    private static final AxisAlignedBB BOUNDS_SIDE_06x04_W = new AxisAlignedBB(0.0,    0.3125, 0.3125, 0.25,   0.6875, 0.6875);
    private static final AxisAlignedBB BOUNDS_SIDE_06x04_E = new AxisAlignedBB(0.75,   0.3125, 0.3125, 1.0,    0.6875, 0.6875);

    public static final PropertyEnum<CircuitType> TYPE = PropertyEnum.<CircuitType>create("type", CircuitType.class);
    public static final PropertyDirection FACING2 = PropertyDirection.create("facing2");
    public static final PropertyDirection FACING3 = PropertyDirection.create("facing3");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockCircuit(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.createHilightBoxMap();

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(FACING2, EnumFacing.EAST)
                .withProperty(FACING3, EnumFacing.WEST)
                .withProperty(POWERED, false)
                .withProperty(TYPE, CircuitType.LATCH_RS));
    }

    @Override
    public boolean hasSpecialHitbox()
    {
        return true;
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                ReferenceNames.NAME_BLOCK_CIRCUIT + "_latch_rs",
                ReferenceNames.NAME_BLOCK_CIRCUIT + "_latch_t"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING2, FACING3, POWERED, TYPE });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        return new TileEntityLatch();
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(TYPE).getItemMeta();
    }

    /*
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
            float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
    {
        return this.getDefaultState()
                .withProperty(TYPE, CircuitType.fromItemMeta(meta))
                .withProperty(FACING, facing);
    }

    @Override
    protected EnumFacing getPlacementFacing(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        // Retain the facing from getStateForPlacement
        return state.getValue(FACING);
    }
    */

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, CircuitType.fromBlockMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getBlockMeta();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        state = super.getActualState(state, world, pos);

        TileEntityLatch te = getTileEntitySafely(world, pos, TileEntityLatch.class);

        if (te != null)
        {
            state = state.withProperty(POWERED, te.getOutpuStrength() > 0);

            if (state.getValue(TYPE) == CircuitType.LATCH_RS)
            {
                state = state.withProperty(FACING2, te.getFacing2());
                state = state.withProperty(FACING3, te.getFacing3());
            }
        }

        return state;
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
        switch (state.getValue(TYPE))
        {
            case LATCH_RS:
                if (side != null)
                {
                    side = side.getOpposite();
                    state = state.getActualState(world, pos);
                    // Output, Set or Reset side
                    return side == state.getValue(FACING) || side == state.getValue(FACING2) || side == state.getValue(FACING3);
                }
                break;

            case LATCH_T:
                return true;
            default:
        }

        return false;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        state = state.getActualState(blockAccess, pos);

        if (side == state.getValue(FACING).getOpposite())
        {
            TileEntityLatch te = getTileEntitySafely(blockAccess, pos, TileEntityLatch.class);
            return te != null ? te.getOutpuStrength() : 0;
        }

        return 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return this.getWeakPower(state, blockAccess, pos, side);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        EnumFacing facing = placer.getHorizontalFacing().getOpposite();
        TileEntityLatch te = getTileEntitySafely(world, pos, TileEntityLatch.class);

        if (te != null)
        {
            te.setFacing2(facing.rotateY());
            te.setFacing3(facing.rotateYCCW());
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        state = state.getActualState(world, pos);

        super.breakBlock(world, pos, state);

        this.notifyNeighbors(world, pos, state.getValue(FACING));
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state)
    {
        world.scheduleUpdate(pos, state.getBlock(), 1);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        state = state.getActualState(world, pos);

        // Don't update due to changes in the output face
        if (pos.offset(state.getValue(FACING)).equals(fromPos) == false)
        {
            world.scheduleUpdate(pos, state.getBlock(), 1);
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor)
    {
        if (blockAccess instanceof World)
        {
            ((World) blockAccess).scheduleUpdate(pos, blockAccess.getBlockState(pos).getBlock(), 1);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand)
    {
        if (world.isRemote == false)
        {
            TileEntityLatch te = getTileEntitySafely(world, pos, TileEntityLatch.class);

            if (te != null)
            {
                switch (state.getValue(TYPE))
                {
                    case LATCH_RS:
                        this.updateStateRS(state, world, pos, te);
                        break;

                    case LATCH_T:
                        this.updateStateT(state, world, pos, te);
                        break;

                    default:
                }
            }
        }
    }

    private void updateStateRS(IBlockState state, World world, BlockPos pos, TileEntityLatch te)
    {
        state = state.getActualState(world, pos);
        EnumFacing facing = state.getValue(FACING3);
        int oldPower = te.getOutpuStrength();
        int newPower = oldPower;

        // Reset active
        if (world.getRedstonePower(pos.offset(facing), facing) > 0)
        {
            newPower = 0;
        }
        else
        {
            facing = state.getValue(FACING2);

            // Set active
            if (world.getRedstonePower(pos.offset(facing), facing) > 0)
            {
                newPower = 15;
            }
        }

        if (newPower != oldPower)
        {
            te.setOutputStrength(newPower);
            this.notifyNeighbors(world, pos, state.getValue(FACING));
            // This marks the block for render update, if the powered state changes
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    private void updateStateT(IBlockState state, World world, BlockPos pos, TileEntityLatch te)
    {
        state = state.getActualState(world, pos);

        boolean powered = false;
        EnumFacing facing = state.getValue(FACING);

        for (EnumFacing side : EnumFacing.values())
        {
            if (side != facing && world.isSidePowered(pos.offset(side), side))
            {
                powered = true;
                break;
            }
        }

        // External power status changed
        if (powered != te.externallyPowered())
        {
            te.setExternallyPowered(powered);

            // External power status changed to powered (ie. a rising edge)
            if (powered)
            {
                int newPower = te.getOutpuStrength() ^ 0xF;
                te.setOutputStrength(newPower);

                this.notifyNeighbors(world, pos, state.getValue(FACING));
                // This marks the block for render update, if the powered state changes
                world.notifyBlockUpdate(pos, state, state, 3);
            }
        }
    }

    public void notifyNeighbors(World world, BlockPos pos, EnumFacing ownFacing)
    {
        if (ForgeEventFactory.onNeighborNotify(world, pos, world.getBlockState(pos), EnumSet.of(ownFacing), false).isCanceled())
        {
            return;
        }

        BlockPos neighborPos = pos.offset(ownFacing);
        world.neighborChanged(neighborPos, this, pos);
        world.notifyNeighborsOfStateExcept(neighborPos, this, ownFacing.getOpposite());
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
        return BOUNDS_BASE_08;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
            AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean p_185477_7_)
    {
        super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, p_185477_7_);

        // Output side's box
        state = state.getActualState(world, pos);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, this.getBoundsForSide(state.getValue(FACING)));

        if (state.getValue(TYPE) == CircuitType.LATCH_RS)
        {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, this.getBoundsForSide(state.getValue(FACING2)));
            addCollisionBoxToList(pos, entityBox, collidingBoxes, this.getBoundsForSide(state.getValue(FACING3)));
        }
    }

    private AxisAlignedBB getBoundsForSide(EnumFacing side)
    {
        switch (side)
        {
            case DOWN:  return BOUNDS_SIDE_06x04_D;
            case UP:    return BOUNDS_SIDE_06x04_U;
            case NORTH: return BOUNDS_SIDE_06x04_N;
            case SOUTH: return BOUNDS_SIDE_06x04_S;
            case WEST:  return BOUNDS_SIDE_06x04_W;
            case EAST:  return BOUNDS_SIDE_06x04_E;
        }

        return PositionUtils.ZERO_BB;
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

        boxMap.put(BOX_ID_MAIN, BOUNDS_BASE_08.offset(pos));

        // Output side's box
        EnumFacing side = actualState.getValue(FACING);
        boxMap.put(side.getIndex(), this.getBoundsForSide(side).offset(pos));

        if (actualState.getValue(TYPE) == CircuitType.LATCH_RS)
        {
            side = actualState.getValue(FACING2);
            boxMap.put(side.getIndex(), this.getBoundsForSide(side).offset(pos));

            side = actualState.getValue(FACING3);
            boxMap.put(side.getIndex(), this.getBoundsForSide(side).offset(pos));
        }
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (CircuitType type : CircuitType.values())
        {
            list.add(new ItemStack(this, 1, type.getItemMeta()));
        }
    }

    public static enum CircuitType implements IStringSerializable
    {
        LATCH_RS  (0, 0, "latch_rs"),
        LATCH_T   (1, 1, "latch_t");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        private CircuitType(int blockMeta, int itemMeta, String name)
        {
            this.blockMeta = blockMeta;
            this.itemMeta = itemMeta;
            this.name = name;
        }

        public int getBlockMeta()
        {
            return this.blockMeta;
        }

        public int getItemMeta()
        {
            return this.itemMeta;
        }

        @Override
        public String toString()
        {
            return this.name;
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        public static CircuitType fromItemMeta(int meta)
        {
            return meta == LATCH_T.getItemMeta() ? LATCH_T : LATCH_RS;
        }

        public static CircuitType fromBlockMeta(int meta)
        {
            return meta == LATCH_T.getBlockMeta() ? LATCH_T : LATCH_RS;
        }
    }
}
