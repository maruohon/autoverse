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
import fi.dy.masa.autoverse.block.base.BlockMachineSlimBase;
import fi.dy.masa.autoverse.tileentity.TileEntityMuxer;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockMuxer extends BlockMachineSlimBase
{
    public static final PropertyEnum<MuxerType> TYPE = PropertyEnum.<MuxerType>create("type", MuxerType.class);
    public static final PropertyDirection FACING_IN2 = PropertyDirection.create("facing_in2");

    public BlockMuxer(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.numModelSideFacings = 1;
        this.propSideFacing0 = FACING_IN2;

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(SLIM, false)
                .withProperty(TYPE, MuxerType.REDSTONE)
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(FACING_IN2, EnumFacing.EAST));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                this.blockName + "_redstone",
                this.blockName + "_priority",
                this.blockName + "_programmable"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING_IN2, SLIM, TYPE });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, MuxerType.fromBlockMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getBlockMeta();
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World worldIn, IBlockState state)
    {
        TileEntityMuxer te = new TileEntityMuxer();
        te.setMuxerType(state.getValue(TYPE));
        return te;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        state = super.getActualState(state, world, pos);

        TileEntityMuxer te = getTileEntitySafely(world, pos, TileEntityMuxer.class);

        if (te != null)
        {
            state = state.withProperty(FACING, te.getFacing())
                         .withProperty(FACING_IN2, te.getInputRelativeFacing());
        }

        return state;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntityMuxer te = getTileEntitySafely(world, pos, TileEntityMuxer.class);

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

            te.setInputSide(filterFacing, false);
        }
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (MuxerType type : MuxerType.values())
        {
            list.add(new ItemStack(this, 1, type.getItemMeta()));
        }
    }

    public static enum MuxerType implements IStringSerializable
    {
        REDSTONE        (0, 0, "redstone"),
        PRIORITY        (1, 1, "priority"),
        PROGRAMMABLE    (2, 2, "programmable");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        private MuxerType(int blockMeta, int itemMeta, String name)
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

        public static MuxerType fromBlockMeta(int meta)
        {
            switch (meta)
            {
                case 2: return PROGRAMMABLE;
                case 1: return PRIORITY;
                default: return REDSTONE;
            }
        }
    }
}
