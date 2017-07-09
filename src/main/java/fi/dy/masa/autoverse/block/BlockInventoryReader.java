package fi.dy.masa.autoverse.block;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.base.BlockAutoverseTileEntity;
import fi.dy.masa.autoverse.event.RenderEventHandler;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityInventoryReader;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class BlockInventoryReader extends BlockAutoverseTileEntity
{
    private static final AxisAlignedBB BOUNDS_ROD_NS = new AxisAlignedBB(0.3125, 0.3125,    0.0, 0.6875, 0.6875,    1.0);
    private static final AxisAlignedBB BOUNDS_ROD_WE = new AxisAlignedBB(   0.0, 0.3125, 0.3125,    1.0, 0.6875, 0.6875);
    private static final AxisAlignedBB BOUNDS_ROD_DU = new AxisAlignedBB(0.3125,    0.0, 0.3125, 0.6875,    1.0, 0.6875);

    private static final AxisAlignedBB BOUNDS_BASE_DOWN  = new AxisAlignedBB(0.125,   0.0, 0.125, 0.875, 0.125, 0.875);
    private static final AxisAlignedBB BOUNDS_BASE_UP    = new AxisAlignedBB(0.125, 0.875, 0.125, 0.875,   1.0, 0.875);
    private static final AxisAlignedBB BOUNDS_BASE_NORTH = new AxisAlignedBB(0.125, 0.125,   0.0, 0.875, 0.875, 0.125);
    private static final AxisAlignedBB BOUNDS_BASE_SOUTH = new AxisAlignedBB(0.125, 0.125, 0.875, 0.875, 0.875,   1.0);
    private static final AxisAlignedBB BOUNDS_BASE_WEST  = new AxisAlignedBB(  0.0, 0.125, 0.125, 0.125, 0.875, 0.875);
    private static final AxisAlignedBB BOUNDS_BASE_EAST  = new AxisAlignedBB(0.875, 0.125, 0.125,   1.0, 0.875, 0.875);

    public static final PropertyEnum<ReaderType> TYPE = PropertyEnum.<ReaderType>create("type", ReaderType.class);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    private final Map<Integer, AxisAlignedBB> hilightBoxMap = new HashMap<Integer, AxisAlignedBB>();

    public BlockInventoryReader(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.getFacingFromTE = false;

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(POWERED, false)
                .withProperty(TYPE, ReaderType.ITEMS));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                ReferenceNames.NAME_BLOCK_INVENTORY_READER + "_items",
                ReferenceNames.NAME_BLOCK_INVENTORY_READER + "_slots"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, POWERED, TYPE });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        return new TileEntityInventoryReader();
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(TYPE).getItemMeta();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
            float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
    {
        return this.getDefaultState()
                .withProperty(TYPE, ReaderType.fromItemMeta(meta))
                .withProperty(FACING, facing);
    }

    @Override
    protected EnumFacing getPlacementFacing(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        // Retain the facing from getStateForPlacement
        return state.getValue(FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getFront(meta & 0x7))
                .withProperty(TYPE, ReaderType.fromBlockMeta((meta & 0x8)));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getBlockMeta() | state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        state = super.getActualState(state, world, pos);

        TileEntityInventoryReader te = getTileEntitySafely(world, pos, TileEntityInventoryReader.class);

        if (te != null)
        {
            state = state.withProperty(POWERED, te.getOutpuStrength() > 0);
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
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        if (side == state.getValue(FACING).getOpposite())
        {
            TileEntityInventoryReader te = getTileEntitySafely(blockAccess, pos, TileEntityInventoryReader.class);
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
    public void onBlockAdded(World world, BlockPos pos, IBlockState state)
    {
        this.updateState(state, world, pos);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        super.breakBlock(world, pos, state);

        this.notifyNeighbors(world, pos, state.getValue(FACING));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        // Don't update due to changes in the output face
        if (pos.offset(state.getValue(FACING)).equals(fromPos) == false)
        {
            this.updateState(state, world, pos);
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor)
    {
        if (blockAccess instanceof World)
        {
            this.updateState(blockAccess.getBlockState(pos), (World) blockAccess, pos);
        }
    }

    private void updateState(IBlockState state, World world, BlockPos pos)
    {
        if (world.isRemote == false)
        {
            TileEntityInventoryReader te = getTileEntitySafely(world, pos, TileEntityInventoryReader.class);

            if (te != null)
            {
                int output = this.calculateOutputSignal(state, world, pos);
                int old = te.getOutpuStrength();

                if (output != old)
                {
                    te.setOutputStrength(output);
                    this.notifyNeighbors(world, pos, state.getValue(FACING));

                    if (output == 0 || old == 0)
                    {
                        // This marks the block for render update, if the powered state changes
                        world.notifyBlockUpdate(pos, state, state, 3);
                    }
                }
            }
        }
    }

    private void notifyNeighbors(World world, BlockPos pos, EnumFacing readerFacing)
    {
        if (ForgeEventFactory.onNeighborNotify(world, pos, world.getBlockState(pos), EnumSet.of(readerFacing), false).isCanceled())
        {
            return;
        }

        BlockPos neighborPos = pos.offset(readerFacing);
        world.neighborChanged(neighborPos, this, pos);
        world.notifyNeighborsOfStateExcept(neighborPos, this, readerFacing.getOpposite());
    }

    private int calculateOutputSignal(IBlockState state, IBlockAccess blockAccess, BlockPos pos)
    {
        EnumFacing targetSide = state.getValue(FACING);
        EnumFacing inputSide = targetSide.getOpposite();
        BlockPos posTarget = pos.offset(inputSide);
        TileEntity te = blockAccess.getTileEntity(posTarget);

        // If there is no inventory adjacent to this block, then offset the position one more
        if (te == null || te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, targetSide) == false)
        {
            posTarget = posTarget.offset(inputSide);
            te = blockAccess.getTileEntity(posTarget);
        }

        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, targetSide))
        {
            IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, targetSide);

            if (state.getValue(TYPE) == ReaderType.ITEMS)
            {
                return inv != null ? InventoryUtils.calcRedstoneFromInventory(inv) : 0;
            }
            else
            {
                return inv != null ? this.calculateStrengthFromSlots(inv) : 0;
            }
        }

        return 0;
    }

    private int calculateStrengthFromSlots(IItemHandler inv)
    {
        final int slots = inv.getSlots();
        int slotsWithItems = 0;

        for (int slot = 0; slot < slots; ++slot)
        {
            if (inv.getStackInSlot(slot).isEmpty() == false)
            {
                slotsWithItems++;
            }
        }

        if (slotsWithItems > 0)
        {
            int strength = (14 * slotsWithItems) / slots;

            // Emit a signal strength of 1 as soon as there is one item in the inventory
            if (slotsWithItems > 0)
            {
                strength += 1;
            }

            return strength;
        }

        return 0;
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
        switch (state.getValue(FACING))
        {
            case NORTH:
            case SOUTH:
                return BOUNDS_ROD_NS;
            case WEST:
            case EAST:
                return BOUNDS_ROD_WE;
            case DOWN:
            case UP:
            default:
                return BOUNDS_ROD_DU;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
            AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean p_185477_7_)
    {
        super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, p_185477_7_);

        EnumFacing baseSide = state.getValue(FACING).getOpposite();

        switch (baseSide)
        {
            case DOWN:  addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_DOWN);  break;
            case UP:    addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_UP);    break;
            case NORTH: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_NORTH); break;
            case SOUTH: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_SOUTH); break;
            case WEST:  addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_WEST);  break;
            case EAST:  addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_EAST);  break;
        }
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
        return this.collisionRayTraceToBoxes(state, world, pos, start, end);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Integer, AxisAlignedBB> getHilightBoxMap()
    {
        return this.hilightBoxMap;
    }

    @Override
    public void updateBlockHilightBoxes(World world, BlockPos pos, EnumFacing facing)
    {
        Map<Integer, AxisAlignedBB> boxMap = this.getHilightBoxMap();
        boxMap.clear();

        // Note: The base plate is on the back end!
        switch (facing)
        {
            case DOWN:
                boxMap.put(0, BOUNDS_BASE_UP.offset(pos));
                boxMap.put(1, BOUNDS_ROD_DU.offset(pos));
                break;
            case UP:
                boxMap.put(0, BOUNDS_BASE_DOWN.offset(pos));
                boxMap.put(1, BOUNDS_ROD_DU.offset(pos));
                break;
            case NORTH:
                boxMap.put(0, BOUNDS_BASE_SOUTH.offset(pos));
                boxMap.put(1, BOUNDS_ROD_NS.offset(pos));
                break;
            case SOUTH:
                boxMap.put(0, BOUNDS_BASE_NORTH.offset(pos));
                boxMap.put(1, BOUNDS_ROD_NS.offset(pos));
                break;
            case WEST:
                boxMap.put(0, BOUNDS_BASE_EAST.offset(pos));
                boxMap.put(1, BOUNDS_ROD_WE.offset(pos));
                break;
            case EAST:
                boxMap.put(0, BOUNDS_BASE_WEST.offset(pos));
                boxMap.put(1, BOUNDS_ROD_WE.offset(pos));
                break;
        }
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (ReaderType type : ReaderType.values())
        {
            list.add(new ItemStack(item, 1, type.getItemMeta()));
        }
    }

    public static enum ReaderType implements IStringSerializable
    {
        ITEMS   (0, 0, "items"),
        SLOTS   (8, 1, "slots");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        private ReaderType(int blockMeta, int itemMeta, String name)
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

        public static ReaderType fromItemMeta(int meta)
        {
            return meta == SLOTS.getItemMeta() ? SLOTS : ITEMS;
        }

        public static ReaderType fromBlockMeta(int meta)
        {
            return meta == SLOTS.getBlockMeta() ? SLOTS : ITEMS;
        }
    }
}
