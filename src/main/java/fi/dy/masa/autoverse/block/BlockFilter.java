package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequential;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequentialStrict;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockFilter extends BlockAutoverseInventory
{
    public static final PropertyEnum<FilterType> TYPE = PropertyEnum.<FilterType>create("type", FilterType.class);
    public static final PropertyDirection FACING_FILTER = PropertyDirection.create("facing_filter");

    public BlockFilter(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, FilterType.BASIC)
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(FACING_FILTER, EnumFacing.EAST));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                this.blockName + "_basic",
                this.blockName + "_sequential",
                this.blockName + "_sequential_strict"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING_FILTER, TYPE });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, FilterType.fromBlockMeta(meta));
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
            case SEQUENTIAL:
                return new TileEntityFilterSequential();

            case SEQUENTIAL_STRICT:
                return new TileEntityFilterSequentialStrict();

            default:
                return new TileEntityFilter();
        }
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileEntityFilter te = getTileEntitySafely(world, pos, TileEntityFilter.class);

        if (te != null)
        {
            state = state.withProperty(FACING, te.getFacing())
                         .withProperty(FACING_FILTER, te.getFilterOutRelativeFacing());
        }

        return state;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntityFilter te = getTileEntitySafely(world, pos, TileEntityFilter.class);

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

            te.setFilterOutputSide(filterFacing);
        }
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        TileEntityFilter te = getTileEntitySafely(worldIn, pos, TileEntityFilter.class);
        return te != null ? te.getComparatorOutput() : 0;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (FilterType type : FilterType.values())
        {
            list.add(new ItemStack(this, 1, type.getItemMeta()));
        }
    }

    public static enum FilterType implements IStringSerializable
    {
        BASIC               (0, 0, "basic"),
        SEQUENTIAL          (1, 1, "sequential"),
        SEQUENTIAL_STRICT   (2, 2, "sequential_strict");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        private FilterType(int blockMeta, int itemMeta, String name)
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

        public static FilterType fromBlockMeta(int meta)
        {
            switch (meta)
            {
                case 2: return SEQUENTIAL_STRICT;
                case 1: return SEQUENTIAL;
                default: return BASIC;
            }
        }
    }
}
