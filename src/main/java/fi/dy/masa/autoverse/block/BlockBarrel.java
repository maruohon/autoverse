package fi.dy.masa.autoverse.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.item.block.ItemBlockStorage;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityBarrel;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.TileUtils;

public class BlockBarrel extends BlockAutoverseInventory
{
    protected static final AxisAlignedBB BARREL_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);
    public static final PropertyBool PULSED = PropertyBool.create("pulsed");
    public static final PropertyInteger TIER = PropertyInteger.create("tier", 0, 15);

    public BlockBarrel(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.hasFacing = false;
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(PULSED, false)
                .withProperty(TIER, 0));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        String[] names = new String[32];

        for (int i = 0; i < 16; i++)
        {
            names[i] = ReferenceNames.NAME_TILE_ENTITY_BARREL + "_" + i;
        }

        for (int i = 16; i < 32; i++)
        {
            names[i] = ReferenceNames.NAME_TILE_ENTITY_BARREL + "_pulsed_" + (i - 16);
        }

        return names;
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { PULSED, TIER });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        TileEntityBarrel te = new TileEntityBarrel();
        te.setTier(state.getValue(TIER));
        return te;
    }

    @Override
    public ItemBlock createItemBlock()
    {
        return new ItemBlockStorage(this);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state)
    {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return BARREL_AABB;
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TIER, meta & 0xF);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TIER);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileEntityBarrel te = getTileEntitySafely(world, pos, TileEntityBarrel.class);

        if (te != null)
        {
            return state.withProperty(PULSED, te.isPulsed());
        }

        return state;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileEntityBarrel te = getTileEntitySafely(world, pos, TileEntityBarrel.class);

        if (te != null)
        {
            te.setIsPulsed(stack.getMetadata() >= 16);
        }
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
        Random rand = world instanceof World ? ((World) world).rand : RANDOM;
        int meta = state.getValue(TIER);
        ItemStack stack = new ItemStack(this.getItemDropped(state, rand, 0), 1, meta);

        TileEntityBarrel te = getTileEntitySafely(world, pos, TileEntityBarrel.class);

        if (te != null && InventoryUtils.getFirstNonEmptySlot(te.getBaseItemHandler()) != -1)
        {
            if (te.isPulsed())
            {
                stack.setItemDamage(meta + 16);
            }

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

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (int meta = 0; meta < 32; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }
}
