package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockBreaker;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockBreaker extends BlockAutoverseInventory
{
    public static final PropertyEnum<BreakerType> TYPE = PropertyEnum.<BreakerType>create("type", BreakerType.class);

    public BlockBreaker(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, BreakerType.NORMAL)
                .withProperty(FACING, DEFAULT_FACING));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                ReferenceNames.NAME_BLOCK_BREAKER + "_normal",
                ReferenceNames.NAME_BLOCK_BREAKER + "_greedy"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, TYPE });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, BreakerType.fromMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getMeta();
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World worldIn, IBlockState state)
    {
        TileEntityBlockBreaker te = new TileEntityBlockBreaker();
        te.setIsGreedy(state.getValue(TYPE) == BreakerType.GREEDY);
        return te;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (int meta = 0; meta < BreakerType.values().length; meta++)
        {
            list.add(new ItemStack(this, 1, meta));
        }
    }

    public static enum BreakerType implements IStringSerializable
    {
        NORMAL  (0, "normal"),
        GREEDY  (1, "greedy");

        private final int meta;
        private final String name;

        private BreakerType(int meta, String name)
        {
            this.meta = meta;
            this.name = name;
        }

        public int getMeta()
        {
            return meta;
        }

        public String toString()
        {
            return this.name;
        }

        public String getName()
        {
            return this.name;
        }

        public static BreakerType fromMeta(int meta)
        {
            return meta < values().length ? values()[meta] : NORMAL;
        }
    }
}
