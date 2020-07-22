package fi.dy.masa.autoverse.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockMachineSlimBase;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitter;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitterAdvanced;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.util.PositionUtils;

public class BlockRedstoneEmitter extends BlockMachineSlimBase
{
    private static final AxisAlignedBB BOUNDS_SLIM_BASE_10 = new AxisAlignedBB(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.8125);

    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_06x03_D = new AxisAlignedBB(0.3125, 0.0,    0.3125, 0.6875, 0.1875, 0.6875);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_06x03_U = new AxisAlignedBB(0.3125, 0.8125, 0.3125, 0.6875, 1.0,    0.6875);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_06x03_N = new AxisAlignedBB(0.3125, 0.3125, 0.0,    0.6875, 0.6875, 0.1875);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_06x03_S = new AxisAlignedBB(0.3125, 0.3125, 0.8125, 0.6875, 0.6875, 1.0   );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_06x03_W = new AxisAlignedBB(0.0,    0.3125, 0.3125, 0.1875, 0.6875, 0.6875);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_06x03_E = new AxisAlignedBB(0.8125, 0.3125, 0.3125, 1.0,    0.6875, 0.6875);

    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x03_D = new AxisAlignedBB(0.25,   0.0,    0.25,   0.75,   0.1875, 0.75  );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x03_U = new AxisAlignedBB(0.25,   0.8125, 0.25,   0.75,   1.0,    0.75  );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x03_N = new AxisAlignedBB(0.25,   0.25,   0.0,    0.75,   0.75,   0.1875);
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x03_S = new AxisAlignedBB(0.25,   0.25,   0.8125, 0.75,   0.75,   1.0   );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x03_W = new AxisAlignedBB(0.0,    0.25,   0.25,   0.1875, 0.75,   0.75  );
    private static final AxisAlignedBB BOUNDS_SLIM_SIDE_08x03_E = new AxisAlignedBB(0.8125, 0.25,   0.25,   1.0,    0.75,   0.75  );

    public static final PropertyEnum<EmitterType> TYPE = PropertyEnum.create("type", EmitterType.class);

    public static final PropertyEnum<SideStatus> DOWN  = PropertyEnum.create("down",  SideStatus.class);
    public static final PropertyEnum<SideStatus> UP    = PropertyEnum.create("up",    SideStatus.class);
    public static final PropertyEnum<SideStatus> NORTH = PropertyEnum.create("north", SideStatus.class);
    public static final PropertyEnum<SideStatus> SOUTH = PropertyEnum.create("south", SideStatus.class);
    public static final PropertyEnum<SideStatus> WEST  = PropertyEnum.create("west",  SideStatus.class);
    public static final PropertyEnum<SideStatus> EAST  = PropertyEnum.create("east",  SideStatus.class);

    public static final List<PropertyEnum<SideStatus>> SIDES = new ArrayList<>();

    static
    {
        SIDES.add(DOWN);
        SIDES.add(UP);
        SIDES.add(NORTH);
        SIDES.add(SOUTH);
        SIDES.add(WEST);
        SIDES.add(EAST);
    }

    public BlockRedstoneEmitter(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(SLIM, false)
                .withProperty(TYPE, EmitterType.BASIC)
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(DOWN,  SideStatus.UNPOWERED)
                .withProperty(UP,    SideStatus.UNPOWERED)
                .withProperty(NORTH, SideStatus.UNPOWERED)
                .withProperty(SOUTH, SideStatus.UNPOWERED)
                .withProperty(WEST,  SideStatus.UNPOWERED)
                .withProperty(EAST,  SideStatus.UNPOWERED));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                this.blockName + "_basic",
                this.blockName + "_advanced"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { SLIM, TYPE, FACING, DOWN, UP, NORTH, SOUTH, WEST, EAST });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        if (state.getValue(TYPE) == EmitterType.ADVANCED)
        {
            return new TileEntityRedstoneEmitterAdvanced();
        }

        return new TileEntityRedstoneEmitter();
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(TYPE).getItemMeta();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, EmitterType.fromBlockMeta(meta & 0x1));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getBlockMeta();
    }

    @Override
    public boolean canProvidePower(IBlockState state)
    {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        state = state.getActualState(blockAccess, pos);
        return state.getValue(SIDES.get(side.getOpposite().getIndex())) == SideStatus.POWERED ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return this.getWeakPower(state, blockAccess, pos, side);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        state = super.getActualState(state, world, pos);

        TileEntityRedstoneEmitter te = getTileEntitySafely(world, pos, TileEntityRedstoneEmitter.class);

        if (te != null)
        {
            state = state.withProperty(FACING, te.getFacing());

            int poweredMask = te.getPoweredMask();
            int sideMask = te.getSideMask();

            if (state.getValue(TYPE) == EmitterType.ADVANCED)
            {
                for (EnumFacing side : EnumFacing.VALUES)
                {
                    state = state.withProperty(SIDES.get(side.getIndex()), (poweredMask & (1 << side.getIndex())) != 0 ? SideStatus.POWERED : SideStatus.UNPOWERED);
                }
            }
            else
            {
                for (EnumFacing side : EnumFacing.VALUES)
                {
                    state = state.withProperty(SIDES.get(side.getIndex()), this.getSideStatus(side, sideMask, poweredMask));
                }
            }
        }

        return state;
    }

    private SideStatus getSideStatus(EnumFacing side, int sideMask, int poweredMask)
    {
        if ((sideMask & (1 << side.getIndex())) != 0)
        {
            return (poweredMask & (1 << side.getIndex())) != 0 ? SideStatus.POWERED : SideStatus.UNPOWERED;
        }
        else
        {
            return SideStatus.DISABLED;
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess blockAccess, BlockPos pos)
    {
        return state.getActualState(blockAccess, pos).getValue(SLIM) ? BOUNDS_SLIM_BASE_10 : FULL_BLOCK_AABB;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void addAllSideCollisionBoxes(IBlockState actualState, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes)
    {
        final EnumFacing facing = actualState.getValue(FACING);
        final boolean advanced = actualState.getValue(TYPE) == EmitterType.ADVANCED;

        addCollisionBoxToList(pos, entityBox, collidingBoxes, this.getBoundsForSide8(facing));

        for (EnumFacing side : EnumFacing.VALUES)
        {
            if (side != facing && (advanced || actualState.getValue(SIDES.get(side.getIndex())) != SideStatus.DISABLED))
            {
                addCollisionBoxToList(pos, entityBox, collidingBoxes, this.getBoundsForSide6(side));
            }
        }
    }

    @Override
    public void updateBlockHilightBoxes(IBlockState actualState, World world, BlockPos pos)
    {
        Map<Integer, AxisAlignedBB> boxMap = this.getHilightBoxMap();
        boxMap.clear();

        if (actualState.getValue(SLIM))
        {
            final EnumFacing mainFacing = actualState.getValue(FACING);
            final boolean advanced = actualState.getValue(TYPE) == EmitterType.ADVANCED;

            // Base/main model/box
            boxMap.put(BOX_ID_MAIN, BOUNDS_SLIM_BASE_10.offset(pos));

            // Output side model
            boxMap.put(mainFacing.getIndex(), this.getBoundsForSide8(mainFacing).offset(pos));

            // Redstone output side models
            for (int sideIndex = 0; sideIndex < 6; sideIndex++)
            {
                final EnumFacing side = EnumFacing.byIndex(sideIndex);

                // This side should have its "connection"
                if (side != mainFacing && (advanced || actualState.getValue(SIDES.get(sideIndex)) != SideStatus.DISABLED))
                {
                    boxMap.put(sideIndex, this.getBoundsForSide6(side).offset(pos));
                }
            }
        }
        else
        {
            boxMap.put(BOX_ID_MAIN, FULL_BLOCK_AABB.offset(pos));
        }
    }

    private AxisAlignedBB getBoundsForSide8(EnumFacing side)
    {
        switch (side)
        {
            case DOWN:  return BOUNDS_SLIM_SIDE_08x03_D;
            case UP:    return BOUNDS_SLIM_SIDE_08x03_U;
            case NORTH: return BOUNDS_SLIM_SIDE_08x03_N;
            case SOUTH: return BOUNDS_SLIM_SIDE_08x03_S;
            case WEST:  return BOUNDS_SLIM_SIDE_08x03_W;
            case EAST:  return BOUNDS_SLIM_SIDE_08x03_E;
        }

        return PositionUtils.ZERO_BB;
    }

    private AxisAlignedBB getBoundsForSide6(EnumFacing side)
    {
        switch (side)
        {
            case DOWN:  return BOUNDS_SLIM_SIDE_06x03_D;
            case UP:    return BOUNDS_SLIM_SIDE_06x03_U;
            case NORTH: return BOUNDS_SLIM_SIDE_06x03_N;
            case SOUTH: return BOUNDS_SLIM_SIDE_06x03_S;
            case WEST:  return BOUNDS_SLIM_SIDE_06x03_W;
            case EAST:  return BOUNDS_SLIM_SIDE_06x03_E;
        }

        return PositionUtils.ZERO_BB;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (EmitterType type : EmitterType.values())
        {
            list.add(new ItemStack(this, 1, type.getItemMeta()));
        }
    }

    public enum EmitterType implements IStringSerializable
    {
        BASIC       (0, 0, "basic"),
        ADVANCED    (1, 1, "advanced");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        EmitterType(int blockMeta, int itemMeta, String name)
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

        public static EmitterType fromBlockMeta(int meta)
        {
            switch (meta)
            {
                case 0: return BASIC;
                case 1: return ADVANCED;
            }

            return BASIC;
        }
    }

    public enum SideStatus implements IStringSerializable
    {
        DISABLED    ("disabled"),
        POWERED     ("powered"),
        UNPOWERED   ("unpowered");

        private final String name;

        SideStatus(String name)
        {
            this.name = name;
        }

        @Override
        public String getName()
        {
            return this.name;
        }
    }
}
