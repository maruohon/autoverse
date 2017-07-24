package fi.dy.masa.autoverse.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitter;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitterAdvanced;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockRedstoneEmitter extends BlockAutoverseInventory
{
    public static final PropertyEnum<EmitterType> TYPE = PropertyEnum.<EmitterType>create("type", EmitterType.class);

    public static final PropertyEnum<SideStatus> DOWN  = PropertyEnum.<SideStatus>create("down",  SideStatus.class);
    public static final PropertyEnum<SideStatus> UP    = PropertyEnum.<SideStatus>create("up",    SideStatus.class);
    public static final PropertyEnum<SideStatus> NORTH = PropertyEnum.<SideStatus>create("north", SideStatus.class);
    public static final PropertyEnum<SideStatus> SOUTH = PropertyEnum.<SideStatus>create("south", SideStatus.class);
    public static final PropertyEnum<SideStatus> WEST  = PropertyEnum.<SideStatus>create("west",  SideStatus.class);
    public static final PropertyEnum<SideStatus> EAST  = PropertyEnum.<SideStatus>create("east",  SideStatus.class);

    public static final List<PropertyEnum<SideStatus>> SIDES = new ArrayList<PropertyEnum<SideStatus>>();

    static
    {
        SIDES.add(DOWN);
        SIDES.add(UP);
        SIDES.add(NORTH);
        SIDES.add(SOUTH);
        SIDES.add(WEST);
        SIDES.add(EAST);
    }

    public BlockRedstoneEmitter(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, EmitterType.BASIC)
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(DOWN,  SideStatus.DISABLED)
                .withProperty(UP,    SideStatus.DISABLED)
                .withProperty(NORTH, SideStatus.DISABLED)
                .withProperty(SOUTH, SideStatus.DISABLED)
                .withProperty(WEST,  SideStatus.DISABLED)
                .withProperty(EAST,  SideStatus.DISABLED));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                this.blockName + "_basic",
                this.blockName + "_advanced"
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { TYPE, FACING, DOWN, UP, NORTH, SOUTH, WEST, EAST });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        if (state.getValue(TYPE) == EmitterType.ADVANCED)
        {
            return new TileEntityRedstoneEmitterAdvanced();
        }

        return new TileEntityRedstoneEmitter();
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(TYPE).getItemMeta();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, EmitterType.fromBlockMeta(meta & 0x1));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getBlockMeta();
    }

    @Override
    public boolean canProvidePower(IBlockState state)
    {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        state = state.getActualState(blockAccess, pos);
        return state.getValue(SIDES.get(side.getOpposite().getIndex())) == SideStatus.POWERED ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return this.getWeakPower(state, blockAccess, pos, side);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileEntityRedstoneEmitter te = getTileEntitySafely(world, pos, TileEntityRedstoneEmitter.class);

        if (te != null)
        {
            state = state.withProperty(FACING, te.getFacing());

            int poweredMask = te.getPoweredMask();
            int sideMask = te.getSideMask();

            if (state.getValue(TYPE) == EmitterType.ADVANCED)
            {
                for (EnumFacing side : EnumFacing.values())
                {
                    state = state.withProperty(SIDES.get(side.getIndex()), (poweredMask & (1 << side.getIndex())) != 0 ? SideStatus.POWERED : SideStatus.UNPOWERED);
                }
            }
            else
            {
                for (EnumFacing side : EnumFacing.values())
                {
                    state = state.withProperty(SIDES.get(side.getIndex()), this.getSideStatus(side, sideMask, poweredMask));
                }
            }
        }

        return state;
    }

    private SideStatus getSideStatus(EnumFacing side, int sideMask, int poweredMask)
    {
        if ((sideMask & (1 << side.getIndex())) != 0)
        {
            return (poweredMask & (1 << side.getIndex())) != 0 ? SideStatus.POWERED : SideStatus.UNPOWERED;
        }
        else
        {
            return SideStatus.DISABLED;
        }
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (EmitterType type : EmitterType.values())
        {
            list.add(new ItemStack(this, 1, type.getItemMeta()));
        }
    }

    public static enum EmitterType implements IStringSerializable
    {
        BASIC       (0, 0, "basic"),
        ADVANCED    (1, 1, "advanced");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        private EmitterType(int blockMeta, int itemMeta, String name)
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

        public static EmitterType fromBlockMeta(int meta)
        {
            switch (meta)
            {
                case 0: return BASIC;
                case 1: return ADVANCED;
            }

            return BASIC;
        }
    }

    public enum SideStatus implements IStringSerializable
    {
        DISABLED    ("disabled"),
        POWERED     ("powered"),
        UNPOWERED   ("unpowered");

        private final String name;

        private SideStatus(String name)
        {
            this.name = name;
        }

        @Override
        public String getName()
        {
            return this.name;
        }
    }
}
