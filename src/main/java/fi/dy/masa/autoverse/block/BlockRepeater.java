package fi.dy.masa.autoverse.block;

import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityRepeater;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockRepeater extends BlockRepeaterBase
{
    private static final AxisAlignedBB BOUNDS_BASE_DOWN  = new AxisAlignedBB(  0.25,    0.0,   0.25,   0.75, 0.0625,   0.75);
    private static final AxisAlignedBB BOUNDS_BASE_UP    = new AxisAlignedBB(  0.25, 0.9375,   0.25,   0.75,    1.0,   0.75);
    private static final AxisAlignedBB BOUNDS_BASE_NORTH = new AxisAlignedBB(  0.25,   0.25,    0.0,   0.75,   0.75, 0.0625);
    private static final AxisAlignedBB BOUNDS_BASE_SOUTH = new AxisAlignedBB(  0.25,   0.25, 0.9375,   0.75,   0.75,    1.0);
    private static final AxisAlignedBB BOUNDS_BASE_WEST  = new AxisAlignedBB(   0.0,   0.25,   0.25, 0.0625,   0.75,   0.75);
    private static final AxisAlignedBB BOUNDS_BASE_EAST  = new AxisAlignedBB(0.9375,   0.25,   0.25,    1.0,   0.75,   0.75);

    public static final PropertyInteger DELAY = PropertyInteger.create("delay", 1, 20);

    public BlockRepeater(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.getFacingFromTE = false;
        this.createHilightBoxMap();

        this.setDefaultState(this.blockState.getBaseState()
                                     .withProperty(DELAY, 2)
                                     .withProperty(FACING, DEFAULT_FACING)
                                     .withProperty(FACING_OUT, DEFAULT_FACING.getOpposite())
                                     .withProperty(POWERED, false));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                ReferenceNames.NAME_BLOCK_REPEATER
        };
    }

    @Override
    public BlockRenderLayer getRenderLayer()
    {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, DELAY, FACING, FACING_OUT, POWERED);
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        return new TileEntityRepeater();
    }

    @Override
    public ItemBlockAutoverse createItemBlock()
    {
        ItemBlockAutoverse item = super.createItemBlock();
        item.addPlacementProperty(OreDictionary.WILDCARD_VALUE, "repeater.delay", Constants.NBT.TAG_BYTE, 1, 20);
        return item;
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return 0;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
    {
        return this.getDefaultState()
                       .withProperty(FACING, facing.getOpposite())
                       .withProperty(FACING_OUT, facing);
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 0x7));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        state = super.getActualState(state, world, pos);

        TileEntityRepeater te = getTileEntitySafely(world, pos, TileEntityRepeater.class);

        if (te != null)
        {
            state = state.withProperty(DELAY, te.getDelay());
            state = state.withProperty(POWERED, te.getOutputStrength() > 0);
            state = state.withProperty(FACING_OUT, te.getOutputFacing());
        }

        return state;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        EnumFacing facing = state.getValue(FACING);

        if (pos.offset(facing).equals(fromPos))
        {
            IBlockState actualState = state.getActualState(world, pos);
            boolean shouldBePowered = this.shouldBePowered(world, pos, actualState);

            if (actualState.getValue(POWERED) != shouldBePowered && world.isBlockTickPending(pos, this) == false)
            {
                this.scheduleRepeaterUpdate(actualState, world, pos);
            }
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor)
    {
        // NO-OP
    }

    @Override
    protected void scheduleRepeaterUpdate(IBlockState actualState, World world, BlockPos pos)
    {
        this.scheduleBlockUpdate(world, pos, actualState, actualState.getValue(DELAY), false);
    }

    public static int calculateInputPower(World world, BlockPos pos, IBlockState state)
    {
        EnumFacing facing = state.getValue(FACING);
        BlockPos inputPos = pos.offset(facing);
        int power = world.getRedstonePower(inputPos, facing);

        if (power >= 15)
        {
            return power;
        }
        else
        {
            IBlockState inputState = world.getBlockState(inputPos);
            return Math.max(power, inputState.getBlock() == Blocks.REDSTONE_WIRE ? inputState.getValue(BlockRedstoneWire.POWER) : 0);
        }
    }

    protected boolean shouldBePowered(World world, BlockPos pos, IBlockState state)
    {
        return calculateInputPower(world, pos, state) > 0;
    }

    @Override
    protected void updateState(IBlockState actualState, World world, BlockPos pos)
    {
        boolean shouldBePowered = this.shouldBePowered(world, pos, actualState);
        boolean powered = actualState.getValue(POWERED);

        if (powered && shouldBePowered == false)
        {
            this.setOutput(actualState, world, pos, 0, shouldBePowered);
        }
        else if (powered == false)
        {
            this.setOutput(actualState, world, pos, 15, shouldBePowered);
        }
    }

    protected void setOutput(IBlockState actualState, World world, BlockPos pos, int powerLevel, boolean shouldBePowered)
    {
        TileEntityRepeater te = getTileEntitySafely(world, pos, TileEntityRepeater.class);

        if (te != null)
        {
            int oldPower = te.getOutputStrength();

            if (oldPower < 0 && powerLevel > 0)
            {
                powerLevel = calculateInputPower(world, pos, actualState);
            }

            te.setOutputStrength(powerLevel);
            notifyOutputs(actualState, world, pos, actualState.getValue(FACING_OUT));

            if (powerLevel == 0 || oldPower <= 0)
            {
                // This marks the block for render update, if the powered state changes
                world.notifyBlockUpdate(pos, actualState, actualState, 3);
            }

            if (oldPower >= 0 && powerLevel > 0 && shouldBePowered == false)
            {
                this.scheduleRepeaterUpdate(actualState, world, pos);
            }
        }
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
}
