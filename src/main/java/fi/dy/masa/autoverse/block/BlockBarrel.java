package fi.dy.masa.autoverse.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
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
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
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
        this.getFacingFromTE = false;

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(PULSED, false)
                .withProperty(TIER, 0));
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
            ReferenceNames.NAME_BLOCK_BARREL,
            ReferenceNames.NAME_BLOCK_BARREL + "_pulsed"
        };
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
        te.setIsPulsed(state.getValue(PULSED));
        return te;
    }

    @Override
    public ItemBlock createItemBlock()
    {
        ItemBlockStorage item = new ItemBlockStorage(this);
        item.addPlacementProperty(OreDictionary.WILDCARD_VALUE, "barrel.tier", Constants.NBT.TAG_BYTE, 0, 15);

        String[] names = new String[] {
                "1 (max: 1)",     "2 (max: 2)",     "3 (max: 4)",      "4 (max: 8)",
                "5 (max: 16)",    "6 (max: 32)",    "7 (max: 64)",     "8 (max: 128)",
                "9 (max: 256)",   "10 (max: 512)",  "11 (max: 1024)",  "12 (max: 2048)",
                "13 (max: 4096)", "14 (max: 8192)", "15 (max: 16384)", "16 (max: 32768)"
        };
        item.addPlacementPropertyValueNames(OreDictionary.WILDCARD_VALUE, "barrel.tier", names);

        return item;
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
        return this.getDefaultState().withProperty(PULSED, (meta & 0x1) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(PULSED) ? 0x1 : 0x0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileEntityBarrel te = getTileEntitySafely(world, pos, TileEntityBarrel.class);

        if (te != null)
        {
            return state.withProperty(TIER, te.getTier());
        }

        return state;
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
        return state.getValue(PULSED) ? 1 : 0;
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

        TileEntityBarrel te = getTileEntitySafely(world, pos, TileEntityBarrel.class);

        if (te != null)
        {
            if (InventoryUtils.getFirstNonEmptySlot(te.getBaseItemHandler()) != -1)
            {
                return TileUtils.storeTileEntityInStackWithCachedInventory(stack, te, addNBTLore, 9);
            }
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
    public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list)
    {
        list.add(new ItemStack(item, 1, 0)); // Normal
        list.add(new ItemStack(item, 1, 1)); // Pulsed
    }
}
