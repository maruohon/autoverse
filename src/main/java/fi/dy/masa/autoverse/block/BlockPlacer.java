package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
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
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityPlacer;
import fi.dy.masa.autoverse.tileentity.TileEntityPlacerProgrammable;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class BlockPlacer extends BlockAutoverseInventory
{
    public static final PropertyEnum<PlacerType> TYPE = PropertyEnum.<PlacerType>create("type", PlacerType.class);

    public BlockPlacer(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(TYPE, PlacerType.NBT));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, TYPE });
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
                this.blockName,
                this.blockName + "_programmable"
        };
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        switch (state.getValue(TYPE))
        {
            case NBT:
                return new TileEntityPlacer();
            case PROGRAMMABLE:
                return new TileEntityPlacerProgrammable();
        }

        return new TileEntityPlacer();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, PlacerType.fromBlockMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getBlockMeta();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntityAutoverseInventory te = getTileEntitySafely(world, pos, TileEntityAutoverseInventory.class);

        if (te != null)
        {
            EnumFacing horizontalFacing = placer.getHorizontalFacing();

            if (te.getFacing().getAxis().isHorizontal())
            {
                horizontalFacing = horizontalFacing.getOpposite();
            }

            if (te instanceof TileEntityPlacer)
            {
                ((TileEntityPlacer) te).setHorizontalFacing(horizontalFacing);
            }
            else if (te instanceof TileEntityPlacerProgrammable)
            {
                ((TileEntityPlacerProgrammable) te).setHorizontalFacing(horizontalFacing);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (PlacerType type : PlacerType.values())
        {
            list.add(new ItemStack(item, 1, type.getItemMeta()));
        }
    }

    public static enum PlacerType implements IStringSerializable
    {
        NBT             (0, 0, "nbt"),
        PROGRAMMABLE    (1, 1, "programmable");

        private final int blockMeta;
        private final int itemMeta;
        private final String name;

        private PlacerType(int blockMeta, int itemMeta, String name)
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

        public static PlacerType fromItemMeta(int meta)
        {
            return meta == PROGRAMMABLE.getItemMeta() ? PROGRAMMABLE : NBT;
        }

        public static PlacerType fromBlockMeta(int meta)
        {
            return meta == PROGRAMMABLE.getBlockMeta() ? PROGRAMMABLE : NBT;
        }
    }
}
