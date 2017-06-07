package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitter;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockRedstoneEmitter extends BlockAutoverseInventory
{
    public static final PropertyBool POWERED    = PropertyBool.create("powered");
    public static final PropertyBool DOWN       = PropertyBool.create("down");
    public static final PropertyBool UP         = PropertyBool.create("up");
    public static final PropertyBool NORTH      = PropertyBool.create("north");
    public static final PropertyBool SOUTH      = PropertyBool.create("south");
    public static final PropertyBool WEST       = PropertyBool.create("west");
    public static final PropertyBool EAST       = PropertyBool.create("east");

    public BlockRedstoneEmitter(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.getFacingFromTE = false;

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(POWERED, false)
                .withProperty(DOWN, false)  .withProperty(UP, false)
                .withProperty(NORTH, false) .withProperty(SOUTH, false)
                .withProperty(WEST, false)  .withProperty(EAST, false));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, POWERED, DOWN, UP, NORTH, SOUTH, WEST, EAST });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        return new TileEntityRedstoneEmitter();
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState()
                .withProperty(POWERED, (meta & 0x8) != 0)
                .withProperty(FACING, EnumFacing.getFront(meta & 0x7));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(FACING).getIndex() | (state.getValue(POWERED) ? 0x8 : 0x0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
    {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand);
        state = state.withProperty(FACING, this.getPlacementFacing(world, pos, state, placer, placer.getHeldItem(hand)));
        return state;
    }

    @Override
    public boolean canProvidePower(IBlockState state)
    {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        if (state.getValue(POWERED))
        {
            TileEntityRedstoneEmitter te = getTileEntitySafely(blockAccess, pos, TileEntityRedstoneEmitter.class);
            return te != null && (te.getSideMask() & (1 << side.getOpposite().getIndex())) != 0 ? 15 : 0;
        }

        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileEntityRedstoneEmitter te = getTileEntitySafely(world, pos, TileEntityRedstoneEmitter.class);

        if (te != null)
        {
            int sideMask = te.getSideMask();

            state = state.withProperty(DOWN,  (sideMask & (1 << EnumFacing.DOWN.getIndex())) != 0);
            state = state.withProperty(UP,    (sideMask & (1 << EnumFacing.UP.getIndex())) != 0);
            state = state.withProperty(NORTH, (sideMask & (1 << EnumFacing.NORTH.getIndex())) != 0);
            state = state.withProperty(SOUTH, (sideMask & (1 << EnumFacing.SOUTH.getIndex())) != 0);
            state = state.withProperty(WEST,  (sideMask & (1 << EnumFacing.WEST.getIndex())) != 0);
            state = state.withProperty(EAST,  (sideMask & (1 << EnumFacing.EAST.getIndex())) != 0);
        }

        return state;
    }
}
