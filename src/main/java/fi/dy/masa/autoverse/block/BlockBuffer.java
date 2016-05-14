package fi.dy.masa.autoverse.block;

import java.util.List;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityBufferFifo;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class BlockBuffer extends BlockAutoverseInventory
{
    public static final PropertyEnum<EnumMachineType> TYPE =
            PropertyEnum.<EnumMachineType>create("type", EnumMachineType.class);

    public BlockBuffer(String name, float hardness, int harvestLevel, Material material)
    {
        super(name, hardness, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, EnumMachineType.FIFO)
                .withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { TYPE, FACING });
    }

    @Override
    public String[] getUnlocalizedNames()
    {
        return new String[] {
                ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO
        };
    }

    @Override
    public TileEntity createTileEntity(World worldIn, IBlockState state)
    {
        EnumMachineType type = state.getValue(TYPE);
        switch (type)
        {
            case FIFO: return new TileEntityBufferFifo();
        }

        return new TileEntityBufferFifo();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, EnumMachineType.fromMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getMeta();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityAutoverse)
        {
            state = state.withProperty(FACING, ((TileEntityAutoverse)te).getFacing());
        }

        return state;
    }

    @Override
    public void randomDisplayTick(IBlockState state, World worldIn, BlockPos pos, Random rand)
    {
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list)
    {
        for (int meta = 0; meta < EnumMachineType.values().length; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        if (blockState.getValue(TYPE) == EnumMachineType.FIFO)
        {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityBufferFifo)
            {
                return InventoryUtils.calcRedstoneFromInventory(((TileEntityBufferFifo)te).getBaseItemHandler());
            }

            return 0;
        }

        return super.getComparatorInputOverride(blockState, worldIn, pos);
    }

    public static enum EnumMachineType implements IStringSerializable
    {
        FIFO (ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);

        private final String name;

        private EnumMachineType(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return this.name;
        }

        public String getName()
        {
            return this.name;
        }

        public int getMeta()
        {
            return this.ordinal();
        }

        public static EnumMachineType fromMeta(int meta)
        {
            return meta < values().length ? values()[meta] : FIFO;
        }
    }
}
