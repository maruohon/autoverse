package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockMachineSlimBase;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockDetector;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockDetector extends BlockMachineSlimBase
{
    protected static final PropertyDirection FACING_OUT = PropertyDirection.create("facing_out");

    public BlockDetector(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.hasMainOutput = false;
        this.numModelSideFacings = 1;
        this.propSideFacing0 = FACING_OUT;

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(SLIM, false)
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(FACING_OUT, EnumFacing.EAST));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING_OUT, SLIM });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        return new TileEntityBlockDetector();
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        state = super.getActualState(state, world, pos);

        TileEntityBlockDetector te = getTileEntitySafely(world, pos, TileEntityBlockDetector.class);

        if (te != null)
        {
            state = state.withProperty(FACING, te.getFacing())
                         .withProperty(FACING_OUT, te.getDetectionOutRelativeFacing());
        }

        return state;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntityBlockDetector te = getTileEntitySafely(world, pos, TileEntityBlockDetector.class);

        if (te != null)
        {
            EnumFacing filterFacing = EnumFacing.getDirectionFromEntityLiving(pos, placer);

            if (filterFacing.getAxis().isVertical())
            {
                filterFacing = placer.getHorizontalFacing().rotateY();
            }
            else
            {
                filterFacing = filterFacing.rotateYCCW();
            }

            te.setDetectionOutputSide(filterFacing, false);
        }
    }
}
