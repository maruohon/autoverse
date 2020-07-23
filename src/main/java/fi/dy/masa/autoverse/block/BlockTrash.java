package fi.dy.masa.autoverse.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.item.block.ItemBlockStorage;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityTrashBin;
import fi.dy.masa.autoverse.tileentity.TileEntityTrashBuffer;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.util.TileUtils;

public class BlockTrash extends BlockAutoverseInventory
{
    protected static final AxisAlignedBB BIN_AABB = new AxisAlignedBB(0.125D, 0.0D, 0.125D, 0.875D, 0.875D, 0.875D);
    public static final PropertyEnum<TrashType> TYPE = PropertyEnum.<TrashType>create("type", TrashType.class);

    public BlockTrash(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.hasFacing = false;
        this.getFacingFromTE = false;

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, TrashType.BIN));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
            ReferenceNames.NAME_TILE_ENTITY_TRASH_BIN,
            ReferenceNames.NAME_TILE_ENTITY_TRASH_BUFFER
        };
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { TYPE });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        switch (state.getValue(TYPE))
        {
            case BUFFER:
                return new TileEntityTrashBuffer();
            default:
                return new TileEntityTrashBin();
        }
    }

    @Override
    public ItemBlockAutoverse createItemBlock()
    {
        return new ItemBlockStorage(this);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return state.getValue(TYPE) == TrashType.BUFFER;
    }

    @Override
    public boolean isFullCube(IBlockState state)
    {
        return state.getValue(TYPE) == TrashType.BUFFER;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return state.getValue(TYPE) == TrashType.BIN ? BIN_AABB : FULL_BLOCK_AABB;
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, TrashType.fromMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getMeta();
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
    {
        if (willHarvest)
        {
            this.onBlockHarvested(world, pos, state, player);
            return true;
        }

        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack)
    {
        // This will cascade down to getDrops()
        super.harvestBlock(world, player, pos, state, te, stack);

        world.setBlockToAir(pos);
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(TYPE).getMeta();
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        List<ItemStack> items = new ArrayList<ItemStack>();

        items.add(this.getDroppedItemWithNBT(world, pos, state, false));

        return items;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state)
    {
        return this.getDroppedItemWithNBT(worldIn, pos, state, false);
    }

    protected ItemStack getDroppedItemWithNBT(IBlockAccess world, BlockPos pos, IBlockState state, boolean addNBTLore)
    {
        ItemStack stack = new ItemStack(this, 1, this.damageDropped(state));

        TileEntityTrashBin te = getTileEntitySafely(world, pos, TileEntityTrashBin.class);

        if (te != null)
        {
            return TileUtils.storeTileEntityInStackWithCachedInventory(stack, te, addNBTLore, 9);
        }

        return stack;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState iBlockState)
    {
        world.updateComparatorOutputLevel(pos, this);
        world.removeTileEntity(pos);
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        list.add(new ItemStack(this, 1, 0)); // Trash Bin
        list.add(new ItemStack(this, 1, 1)); // Trash Buffer
    }

    public static enum TrashType implements IStringSerializable
    {
        BIN     (0, "bin"),
        BUFFER  (1, "buffer");

        private final int meta;
        private final String name;

        private TrashType(int meta, String name)
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

        public static TrashType fromMeta(int meta)
        {
            return meta == BUFFER.meta ? BUFFER : BIN;
        }
    }
}
