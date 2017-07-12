package fi.dy.masa.autoverse.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
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
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.event.RenderEventHandler;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityPipe;
import fi.dy.masa.autoverse.tileentity.TileEntityPipeDirectional;
import fi.dy.masa.autoverse.tileentity.TileEntityPipeExtraction;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockPipe extends BlockAutoverseInventory
{
    protected static final AxisAlignedBB BOUNDS_MIDDLE  = new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    protected static final AxisAlignedBB BOUNDS_MAIN_NS = new AxisAlignedBB(0.25, 0.25,  0.0, 0.75, 0.75,  1.0);
    protected static final AxisAlignedBB BOUNDS_MAIN_WE = new AxisAlignedBB( 0.0, 0.25, 0.25,  1.0, 0.75, 0.75);
    protected static final AxisAlignedBB BOUNDS_MAIN_DU = new AxisAlignedBB(0.25,  0.0, 0.25, 0.75,  1.0, 0.75);

    protected static final AxisAlignedBB BOUNDS_SIDE_DOWN  = new AxisAlignedBB(0.25,  0.0, 0.25, 0.75, 0.25, 0.75);
    protected static final AxisAlignedBB BOUNDS_SIDE_UP    = new AxisAlignedBB(0.25, 0.75, 0.25, 0.75,  1.0, 0.75);
    protected static final AxisAlignedBB BOUNDS_SIDE_NORTH = new AxisAlignedBB(0.25, 0.25,  0.0, 0.75, 0.75, 0.25);
    protected static final AxisAlignedBB BOUNDS_SIDE_SOUTH = new AxisAlignedBB(0.25, 0.25, 0.75, 0.75, 0.75,  1.0);
    protected static final AxisAlignedBB BOUNDS_SIDE_WEST  = new AxisAlignedBB( 0.0, 0.25, 0.25, 0.25, 0.75, 0.75);
    protected static final AxisAlignedBB BOUNDS_SIDE_EAST  = new AxisAlignedBB(0.75, 0.25, 0.25,  1.0, 0.75, 0.75);

    private static final AxisAlignedBB[] SIDE_BOUNDS_BY_FACING = new AxisAlignedBB[] { BOUNDS_SIDE_DOWN, BOUNDS_SIDE_UP, BOUNDS_SIDE_NORTH, BOUNDS_SIDE_SOUTH, BOUNDS_SIDE_WEST, BOUNDS_SIDE_EAST };

    public static final PropertyEnum<PipeType> TYPE = PropertyEnum.<PipeType>create("type", PipeType.class);

    public static final PropertyEnum<Connection> CONN_UP    = PropertyEnum.<Connection>create("up",    Connection.class);
    public static final PropertyEnum<Connection> CONN_DOWN  = PropertyEnum.<Connection>create("down",  Connection.class);
    public static final PropertyEnum<Connection> CONN_NORTH = PropertyEnum.<Connection>create("north", Connection.class);
    public static final PropertyEnum<Connection> CONN_SOUTH = PropertyEnum.<Connection>create("south", Connection.class);
    public static final PropertyEnum<Connection> CONN_WEST  = PropertyEnum.<Connection>create("west",  Connection.class);
    public static final PropertyEnum<Connection> CONN_EAST  = PropertyEnum.<Connection>create("east",  Connection.class);

    public static final List<PropertyEnum<Connection>> CONNECTIONS = new ArrayList<PropertyEnum<Connection>>();

    private final Map<Pair<PipePart, EnumFacing>, AxisAlignedBB> hilightBoxMap = new HashMap<Pair<PipePart, EnumFacing>, AxisAlignedBB>();

    static
    {
        CONNECTIONS.add(CONN_DOWN);
        CONNECTIONS.add(CONN_UP);
        CONNECTIONS.add(CONN_NORTH);
        CONNECTIONS.add(CONN_SOUTH);
        CONNECTIONS.add(CONN_WEST);
        CONNECTIONS.add(CONN_EAST);
    }

    public BlockPipe(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.hasFacing = false;
        this.getFacingFromTE = false;

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, PipeType.BASIC)
                .withProperty(CONN_UP,    Connection.NONE)
                .withProperty(CONN_DOWN,  Connection.NONE)
                .withProperty(CONN_NORTH, Connection.NONE)
                .withProperty(CONN_SOUTH, Connection.NONE)
                .withProperty(CONN_WEST,  Connection.NONE)
                .withProperty(CONN_EAST,  Connection.NONE));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                this.blockName + "_basic",
                this.blockName + "_extraction",
                this.blockName + "_directional"
        };
    }

    @Override
    public ItemBlock createItemBlock()
    {
        ItemBlockAutoverse item = new ItemBlockAutoverse(this);
        item.addPlacementProperty(OreDictionary.WILDCARD_VALUE, "pipe.delay", Constants.NBT.TAG_INT, 1, 72000);
        item.addPlacementProperty(OreDictionary.WILDCARD_VALUE, "pipe.max_stack", Constants.NBT.TAG_BYTE, 1, 64);
        return item;
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { TYPE, CONN_UP, CONN_DOWN, CONN_NORTH, CONN_SOUTH, CONN_WEST, CONN_EAST });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, PipeType.fromBlockMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getBlockMeta();
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World worldIn, IBlockState state)
    {
        switch (state.getValue(TYPE))
        {
            case EXTRACTION:    return new TileEntityPipeExtraction();
            case DIRECTIONAL:   return new TileEntityPipeDirectional();
            default:            return new TileEntityPipe();
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state)
    {
        super.onBlockAdded(world, pos, state);

        TileEntityPipe te = getTileEntitySafely(world, pos, TileEntityPipe.class);

        if (te != null)
        {
            te.updateConnectedSides();

            // This will schedule the first update after placing down the Inserter.
            // Otherwise it would need an external update to start moving items.
            te.onNeighborBlockChange(world, pos, state, state.getBlock());
        }
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileEntityPipe te = getTileEntitySafely(world, pos, TileEntityPipe.class);

        if (te != null)
        {
            int mask = te.getConnectedSidesMask();

            for (int i = 0, bit = 0x1; i < 6; i++, bit <<= 1)
            {
                if ((mask & bit) != 0)
                {
                    // FIXME connection type
                    state = state.withProperty(CONNECTIONS.get(i), Connection.BASIC);
                }
            }
        }

        return state;
    }

    @Override
    public boolean isFullCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess blockAccess, BlockPos pos)
    {
        return BOUNDS_MIDDLE;
    }

    //@SuppressWarnings("deprecation")
    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
            AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean p_185477_7_)
    {
        //super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, p_185477_7_);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, state.getCollisionBoundingBox(world, pos));

        TileEntityPipe te = getTileEntitySafely(world, pos, TileEntityPipe.class);

        if (te != null)
        {
            int mask = te.getConnectedSidesMask();

            for (int bit = 0x1; bit <= 32; bit <<= 1)
            {
                if ((mask & bit) != 0)
                {
                    switch (bit)
                    {
                        case  1: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_SIDE_DOWN); break;
                        case  2: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_SIDE_UP); break;
                        case  4: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_SIDE_NORTH); break;
                        case  8: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_SIDE_SOUTH); break;
                        case 16: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_SIDE_WEST); break;
                        case 32: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_SIDE_EAST); break;
                    }
                }
            }
        }
    }

    public static List<EnumFacing> getSidesFromMask(int mask)
    {
        List<EnumFacing> sides = new ArrayList<EnumFacing>(6);

        for (int i = 0, bit = 0x1; i < 6; i++, bit <<= 1)
        {
            if ((mask & bit) != 0)
            {
                sides.add(EnumFacing.getFront(i));
            }
        }

        return sides;
    }

    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end)
    {
        return collisionRayTraceToBoxes(state, this, world, pos, start, end);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos)
    {
        AxisAlignedBB bb = RenderEventHandler.getInstance().getPointedHilightBox(this);

        if (bb != null)
        {
            return bb;
        }

        return state.getBoundingBox(world, pos).offset(pos);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Pair<PipePart, EnumFacing>, AxisAlignedBB> getHilightBoxMap()
    {
        return this.hilightBoxMap;
    }

    @Override
    public void updateBlockHilightBoxes(World world, BlockPos pos, @Nullable EnumFacing facing)
    {
        TileEntityPipe te = getTileEntitySafely(world, pos, TileEntityPipe.class);
        Map<Pair<PipePart, EnumFacing>, AxisAlignedBB> boxMap = this.getHilightBoxMap();
        boxMap.clear();

        if (te != null)
        {
            int mask = te.getConnectedSidesMask();

            for (int i = 0, bit = 0x1; i < 6; i++, bit <<= 1)
            {
                if ((mask & bit) != 0)
                {
                    boxMap.put(Pair.of(PipePart.SIDE, EnumFacing.getFront(i)), SIDE_BOUNDS_BY_FACING[i].offset(pos));
                }
            }
        }

        boxMap.put(Pair.of(PipePart.MAIN, EnumFacing.DOWN), BOUNDS_MIDDLE.offset(pos));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (PipeType type : PipeType.values())
        {
            list.add(new ItemStack(this, 1, type.getItemMeta()));
        }
    }

    public static enum PipeType implements IStringSerializable
    {
        BASIC       (0, 0, "basic"),
        EXTRACTION  (1, 1, "extraction"),
        DIRECTIONAL (2, 2, "directional");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        private PipeType(int blockMeta, int itemMeta, String name)
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

        public static PipeType fromBlockMeta(int meta)
        {
            switch (meta)
            {
                case 0: return BASIC;
                case 1: return EXTRACTION;
                case 2: return DIRECTIONAL;
            }

            return BASIC;
        }
    }

    public enum Connection implements IStringSerializable
    {
        NONE    ("none"),
        BASIC   ("basic"),
        EXTRACT ("extract"),
        OUTPUT  ("output");

        private final String name;

        private Connection(String name)
        {
            this.name = name;
        }

        @Override
        public String getName()
        {
            return this.name;
        }
    }

    public enum PipePart
    {
        MAIN,
        SIDE
    }
}
