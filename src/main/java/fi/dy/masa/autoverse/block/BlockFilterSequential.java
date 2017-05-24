package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequentialSmart;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockFilterSequential extends BlockFilter
{
    private static final int NUM_TIERS = 3;
    private static final PropertyInteger TIER = PropertyInteger.create("tier", 0, NUM_TIERS - 1);

    public BlockFilterSequential(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material, NUM_TIERS);
    }

    @Override
    protected PropertyInteger getTierProperty()
    {
        return TIER;
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World worldIn, IBlockState state)
    {
        TileEntityFilter te;

        if (this == AutoverseBlocks.FILTER_SEQUENTIAL_SMART)
        {
            te = new TileEntityFilterSequentialSmart();
        }
        else
        {
            te = new TileEntityFilterSequential();
        }

        te.setFilterTier(state.getValue(this.getTierProperty()));
        return te;
    }
}
