package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityPlacer;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockPlacer extends BlockAutoverseInventory
{
    public BlockPlacer(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, DEFAULT_FACING));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        return new TileEntityPlacer();
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
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntityPlacer te = getTileEntitySafely(world, pos, TileEntityPlacer.class);

        if (te != null)
        {
            if (te.getFacing().getAxis().isHorizontal() == false)
            {
                te.setHorizontalFacing(placer.getHorizontalFacing());
            }
            else
            {
                te.setHorizontalFacing(placer.getHorizontalFacing().getOpposite());
            }
        }
    }
}
