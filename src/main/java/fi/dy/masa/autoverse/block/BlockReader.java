package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityBlockReaderNBT;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockReader extends BlockAutoverseInventory
{
    public static final PropertyEnum<ReaderType> TYPE = PropertyEnum.<ReaderType>create("type", ReaderType.class);

    public BlockReader(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, ReaderType.NBT)
                .withProperty(FACING, DEFAULT_FACING));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                this.blockName + "_nbt"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, TYPE });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        return new TileEntityBlockReaderNBT();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, ReaderType.fromMeta(meta & 0x1));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getMeta();
    }

    /*
    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (int meta = 0; meta < ReaderType.values().length; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }
    */

    public enum ReaderType implements IStringSerializable
    {
        NBT         (0, "nbt");
        //SEQUENCE    (1, "sequence");

        private final int meta;
        private final String name;

        private ReaderType(int meta, String name)
        {
            this.meta = meta;
            this.name = name;
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

        public int getMeta()
        {
            return this.meta;
        }

        public static ReaderType fromMeta(int meta)
        {
            return values()[meta % values().length];
        }
    }
}
