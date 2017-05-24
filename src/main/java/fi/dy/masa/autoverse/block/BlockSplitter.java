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
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockSplitter extends BlockAutoverseInventory
{
    public static final PropertyDirection FACING2 = PropertyDirection.create("facing_out2");

    public BlockSplitter(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(FACING2, EnumFacing.DOWN));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING2 });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        return new TileEntitySplitter();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileEntitySplitter te = getTileEntitySafely(world, pos, TileEntitySplitter.class);

        if (te != null)
        {
            state = state.withProperty(FACING, te.getFacing())
                         .withProperty(FACING2, te.getSecondOutputRelativeFacing());
        }

        return state;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        TileEntitySplitter te = getTileEntitySafely(world, pos, TileEntitySplitter.class);

        if (te != null)
        {
            te.dropInventories();
            world.updateComparatorOutputLevel(pos, this);
        }

        world.removeTileEntity(pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntitySplitter te = getTileEntitySafely(world, pos, TileEntitySplitter.class);

        if (te != null)
        {
            EnumFacing facing = EnumFacing.getDirectionFromEntityLiving(pos, placer);

            if (facing.getAxis().isVertical())
            {
                facing = placer.getHorizontalFacing().rotateY();
            }
            else
            {
                facing = facing.rotateYCCW();
            }

            te.setSecondOutputSide(facing);
        }
    }
}
