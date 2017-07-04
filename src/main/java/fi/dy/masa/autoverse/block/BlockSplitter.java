package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockSplitter extends BlockAutoverseInventory
{
    public static final PropertyEnum<SplitterType> TYPE = PropertyEnum.<SplitterType>create("type", SplitterType.class);
    public static final PropertyDirection FACING2 = PropertyDirection.create("facing_out2");

    public BlockSplitter(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, SplitterType.SELECTABLE)
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(FACING2, EnumFacing.DOWN));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                this.blockName + "_selectable",
                this.blockName + "_redstone"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING2, TYPE });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        TileEntitySplitter te = new TileEntitySplitter();
        te.setSplitterType(state.getValue(TYPE));
        return te;
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, SplitterType.fromMeta(meta & 0x3));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getMeta();
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

            te.setSecondOutputSide(facing, false);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (int meta = 0; meta < SplitterType.values().length; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }

    public enum SplitterType implements IStringSerializable
    {
        SELECTABLE      (0, "selectable"),
        REDSTONE        (1, "redstone");

        private final int meta;
        private final String name;

        private SplitterType(int meta, String name)
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

        public static SplitterType fromMeta(int meta)
        {
            return values()[meta % values().length];
        }
    }
}
