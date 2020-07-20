package fi.dy.masa.autoverse.block;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class BlockInventoryReader extends BlockAutoverseTileEntity
{
    protected static final AxisAlignedBB BOUNDS_ROD_DOWN  = new AxisAlignedBB(0.3125, 0.0625, 0.3125, 0.6875,   0.25, 0.6875);
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

    private static final AxisAlignedBB BOUNDS_BASE_DOWN  = new AxisAlignedBB( 0.125,    0.0,  0.125,  0.875, 0.0625,  0.875);
    private static final AxisAlignedBB BOUNDS_BASE_UP    = new AxisAlignedBB( 0.125, 0.9375,  0.125,  0.875,    1.0,  0.875);
    private static final AxisAlignedBB BOUNDS_BASE_NORTH = new AxisAlignedBB( 0.125,  0.125,    0.0,  0.875,  0.875, 0.0625);
    private static final AxisAlignedBB BOUNDS_BASE_SOUTH = new AxisAlignedBB( 0.125,  0.125, 0.9375,  0.875,  0.875,    1.0);
    private static final AxisAlignedBB BOUNDS_BASE_WEST  = new AxisAlignedBB(   0.0,  0.125,  0.125, 0.0625,  0.875,  0.875);
    private static final AxisAlignedBB BOUNDS_BASE_EAST  = new AxisAlignedBB(0.9375,  0.125,  0.125,    1.0,  0.875,  0.875);

    public static final PropertyEnum<ReaderType> TYPE = PropertyEnum.create("type", ReaderType.class);

    public BlockInventoryReader(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.getFacingFromTE = false;
        this.createHilightBoxMap();

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(FACING_OUT, DEFAULT_FACING.getOpposite())
                .withProperty(POWERED, false)
                .withProperty(TYPE, ReaderType.ITEMS));
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
                ReferenceNames.NAME_BLOCK_INVENTORY_READER + "_items",
                ReferenceNames.NAME_BLOCK_INVENTORY_READER + "_slots"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING_OUT, POWERED, TYPE });
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
                .withProperty(FACING, facing.getOpposite())
                .withProperty(FACING_OUT, facing);
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
                .withProperty(FACING, EnumFacing.byIndex(meta & 0x7))
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
            state = state.withProperty(POWERED, te.getOutputStrength() > 0);
            state = state.withProperty(FACING_OUT, te.getOutputFacing());
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
        state = state.getActualState(world, pos);
        return state.getValue(FACING_OUT).getOpposite() == side;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        state = state.getActualState(blockAccess, pos);

        if (side == state.getValue(FACING_OUT).getOpposite())
        {
            TileEntityInventoryReader te = getTileEntitySafely(blockAccess, pos, TileEntityInventoryReader.class);
            return te != null ? te.getOutputStrength() : 0;
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

    /*
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
    {
        if (willHarvest)
        {
            return true;
        }

        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack)
    {
        //state = state.getActualState(world, pos);
        //this.notifyNeighbors(world, pos, state.getValue(FACING_OUT));

        // This will cascade down to getDrops()
        super.harvestBlock(world, player, pos, state, te, stack);

        world.setBlockToAir(pos);
    }
    */

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        this.updateStateIfFront(state, world, pos, fromPos);
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor)
    {
        if (blockAccess instanceof World)
        {
            this.updateStateIfFront(blockAccess.getBlockState(pos), (World) blockAccess, pos, neighbor);
        }
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntityInventoryReader te = getTileEntitySafely(world, pos, TileEntityInventoryReader.class);

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
            state = state.getActualState(world, pos);
            this.updateState(state, world, pos);
        }
    }

    private void updateStateIfFront(IBlockState state, World world, BlockPos pos, BlockPos posFrom)
    {
        state = state.getActualState(world, pos);
        this.updateState(state, world, pos);
    }

    private void updateState(IBlockState state, World world, BlockPos pos)
    {
        if (world.isRemote == false)
        {
            TileEntityInventoryReader te = getTileEntitySafely(world, pos, TileEntityInventoryReader.class);

            if (te != null)
            {
                int output = this.calculateOutputSignal(state, world, pos);

                if (output != -1)
                {
                    int old = te.getOutputStrength();

                    if (output != old)
                    {
                        te.setOutputStrength(output);
                        notifyOutputs(state, world, pos, state.getValue(FACING_OUT));

                        if (output == 0 || old == 0)
                        {
                            // This marks the block for render update, if the powered state changes
                            world.notifyBlockUpdate(pos, state, state, 3);
                        }
                    }
                }
            }
        }
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

    private int calculateOutputSignal(IBlockState state, IBlockAccess blockAccess, BlockPos pos)
    {
        EnumFacing targetSide = state.getValue(FACING_OUT);
        EnumFacing inputSide = state.getValue(FACING);
        BlockPos posTarget = pos.offset(inputSide);

        if (blockAccess instanceof World && ((World) blockAccess).isBlockLoaded(posTarget, true) == false)
        {
            return -1;
        }

        TileEntity te = blockAccess.getTileEntity(posTarget);
        IItemHandler inv = null;

        // If there is no inventory adjacent to this block, then offset the position one more
        if ((te == null || te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, targetSide) == false) &&
            blockAccess.getBlockState(posTarget).isNormalCube())
        {
            posTarget = posTarget.offset(inputSide);
            te = blockAccess.getTileEntity(posTarget);
        }

        if (te instanceof TileEntityAutoverseInventory)
        {
            inv = ((TileEntityAutoverseInventory) te).getInventoryForInventoryReader(targetSide);
        }
        else if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, targetSide))
        {
            inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, targetSide);
        }

        if (inv != null)
        {
            int output;

            if (state.getValue(TYPE) == ReaderType.ITEMS)
            {
                output = inv != null ? InventoryUtils.calcRedstoneFromInventory(inv) : -1;
            }
            else
            {
                output = inv != null ? this.calculateStrengthFromSlots(inv) : -1;
            }

            if (output > 0)
            {
                return output;
            }
            else if (state.getValue(TYPE) == ReaderType.ITEMS && blockAccess instanceof World)
            {
                IBlockState stateTarget = blockAccess.getBlockState(posTarget);

                if (stateTarget.hasComparatorInputOverride())
                {
                    return stateTarget.getComparatorInputOverride((World) blockAccess, posTarget);
                }
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
        return BOUNDS_BULGE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
            AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean p_185477_7_)
    {
        state = state.getActualState(world, pos);

        addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BULGE);

        EnumFacing baseSide = state.getValue(FACING);

        switch (baseSide)
        {
            case DOWN:
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_DOWN);
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_ROD_DOWN);
                break;
            case UP:
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_UP);
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_ROD_UP);
                break;
            case NORTH:
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_NORTH);
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_ROD_NORTH);
                break;
            case SOUTH:
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_SOUTH);
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_ROD_SOUTH);
                break;
            case WEST:
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_WEST);
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_ROD_WEST);
                break;
            case EAST:
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_BASE_EAST);
                addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_ROD_EAST);
                break;
        }

        EnumFacing outputSide = state.getValue(FACING_OUT);

        switch (outputSide)
        {
            case DOWN:  addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_OUT_DOWN);  break;
            case UP:    addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_OUT_UP);    break;
            case NORTH: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_OUT_NORTH); break;
            case SOUTH: addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_OUT_SOUTH); break;
            case WEST:  addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_OUT_WEST);  break;
            case EAST:  addCollisionBoxToList(pos, entityBox, collidingBoxes, BOUNDS_OUT_EAST);  break;
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
        state = state.getActualState(world, pos);
        return collisionRayTraceToBoxes(state, this, world, pos, start, end);
    }

    @Override
    public void updateBlockHilightBoxes(IBlockState actualState, World world, BlockPos pos)
    {
        Map<Integer, AxisAlignedBB> boxMap = this.getHilightBoxMap();
        boxMap.clear();

        AxisAlignedBB bbMain;
        AxisAlignedBB bbRod;
        AxisAlignedBB bbOut;
        final EnumFacing inputFacing = actualState.getValue(FACING);
        final EnumFacing outputFacing = actualState.getValue(FACING_OUT);

        switch (inputFacing)
        {
            case DOWN:
                bbMain = BOUNDS_BASE_DOWN;
                bbRod = BOUNDS_ROD_DOWN;
                break;
            case UP:
                bbMain = BOUNDS_BASE_UP;
                bbRod = BOUNDS_ROD_UP;
                break;
            case NORTH:
                bbMain = BOUNDS_BASE_NORTH;
                bbRod = BOUNDS_ROD_NORTH;
                break;
            case SOUTH:
                bbMain = BOUNDS_BASE_SOUTH;
                bbRod = BOUNDS_ROD_SOUTH;
                break;
            case WEST:
                bbMain = BOUNDS_BASE_WEST;
                bbRod = BOUNDS_ROD_WEST;
                break;
            case EAST:
            default:
                bbMain = BOUNDS_BASE_EAST;
                bbRod = BOUNDS_ROD_EAST;
                break;
        }

        switch (outputFacing)
        {
            case DOWN:
                bbOut = BOUNDS_OUT_DOWN;
                break;
            case UP:
                bbOut = BOUNDS_OUT_UP;
                break;
            case NORTH:
                bbOut = BOUNDS_OUT_NORTH;
                break;
            case SOUTH:
                bbOut = BOUNDS_OUT_SOUTH;
                break;
            case WEST:
                bbOut = BOUNDS_OUT_WEST;
                break;
            case EAST:
            default:
                bbOut = BOUNDS_OUT_EAST;
                break;
        }

        boxMap.put(BOX_ID_MAIN, bbMain.offset(pos));
        boxMap.put(BOX_ID_MAIN + 1, BOUNDS_BULGE.offset(pos));
        boxMap.put(BOX_ID_MAIN + 2, bbRod.offset(pos));

        boxMap.put(outputFacing.getIndex(), bbOut.offset(pos));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (ReaderType type : ReaderType.values())
        {
            list.add(new ItemStack(this, 1, type.getItemMeta()));
        }
    }

    public enum ReaderType implements IStringSerializable
    {
        ITEMS   (0, 0, "items"),
        SLOTS   (8, 1, "slots");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        ReaderType(int blockMeta, int itemMeta, String name)
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
