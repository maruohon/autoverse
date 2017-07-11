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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;
import fi.dy.masa.autoverse.tileentity.TileEntityPipe;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockPipe extends BlockAutoverseInventory
{
    public static final PropertyEnum<PipeType> TYPE = PropertyEnum.<PipeType>create("type", PipeType.class);

    public BlockPipe(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.hasFacing = false;
        this.getFacingFromTE = false;

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, PipeType.BASIC));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                this.blockName + "_basic",
                this.blockName + "_extraction"
        };
    }

    @Override
    public ItemBlock createItemBlock()
    {
        ItemBlockAutoverse item = new ItemBlockAutoverse(this);
        item.addPlacementProperty(OreDictionary.WILDCARD_VALUE, "pipe.delay", Constants.NBT.TAG_INT, 1, 72000);
        item.addPlacementProperty(OreDictionary.WILDCARD_VALUE, "pipe.max_stack", Constants.NBT.TAG_BYTE, 1, 64);
        return item;
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { TYPE });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, PipeType.fromBlockMeta(meta));
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
            default: return new TileEntityPipe();
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state)
    {
        TileEntityPipe te = getTileEntitySafely(world, pos, TileEntityPipe.class);

        if (te != null)
        {
            te.updateConnectedSides();
        }
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileEntityPipe te = getTileEntitySafely(world, pos, TileEntityPipe.class);

        if (te != null)
        {
        }

        return state;
    }

    @Override
    public boolean isFullCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (PipeType type : PipeType.values())
        {
            list.add(new ItemStack(this, 1, type.getItemMeta()));
        }
    }

    public static enum PipeType implements IStringSerializable
    {
        BASIC       (0, 0, "basic"),
        EXTRACTION  (1, 1, "extraction");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        private PipeType(int blockMeta, int itemMeta, String name)
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

        public static PipeType fromBlockMeta(int meta)
        {
            return meta == EXTRACTION.blockMeta ? EXTRACTION : BASIC;
        }
    }
}
