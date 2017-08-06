package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifoAuto;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifoPulsed;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockBuffer extends BlockAutoverseInventory
{
    public static final PropertyEnum<BufferType> TYPE = PropertyEnum.<BufferType>create("type", BufferType.class);

    public BlockBuffer(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, BufferType.FIFO)
                .withProperty(FACING, DEFAULT_FACING));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { TYPE, FACING });
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO,
                ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_PULSED,
                ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_AUTO
        };
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        switch (state.getValue(TYPE))
        {
            case FIFO:
                return new TileEntityBufferFifo();
            case FIFO_PULSED:
                return new TileEntityBufferFifoPulsed();
            case FIFO_AUTO:
                return new TileEntityBufferFifoAuto();
        }

        return new TileEntityBufferFifo();
    }

    @Override
    public ItemBlock createItemBlock()
    {
        ItemBlockAutoverse item = new ItemBlockAutoverse(this);
        item.addPlacementProperty(OreDictionary.WILDCARD_VALUE, "buffer.size", Constants.NBT.TAG_BYTE, 1, TileEntityBufferFifo.MAX_LENGTH);
        item.addPlacementProperty(OreDictionary.WILDCARD_VALUE, "buffer.delay", Constants.NBT.TAG_BYTE, 1, 255);
        return item;
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, BufferType.fromMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getMeta();
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (int meta = 0; meta < BufferType.values().length; meta++)
        {
            list.add(new ItemStack(this, 1, meta));
        }
    }

    public static enum BufferType implements IStringSerializable
    {
        FIFO            (0, "fifo_normal"),
        FIFO_PULSED     (1, "fifo_pulsed"),
        FIFO_AUTO       (2, "fifo_auto");

        private final int meta;
        private final String name;

        private BufferType(int meta, String name)
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

        public static BufferType fromMeta(int meta)
        {
            return meta < values().length ? values()[meta] : FIFO;
        }
    }
}
