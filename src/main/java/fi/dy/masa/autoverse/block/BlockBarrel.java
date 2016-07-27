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
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.TileEntityBarrel;

public class BlockBarrel extends BlockAutoverseInventory
{
    protected static final AxisAlignedBB BARREL_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);
    public static final PropertyBool PULSED = PropertyBool.create("pulsed");
    public static final PropertyInteger TIER = PropertyInteger.create("tier", 0, 15);

    public BlockBarrel(String name, float hardness, int harvestLevel, Material material)
    {
        super(name, hardness, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState().withProperty(PULSED, false).withProperty(TIER, 0));
    }

    @Override
    protected String[] createUnlocalizedNames()
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
    public String[] getItemBlockVariantStrings()
    {
        return null;
    }

    @Override
    public TileEntity createTileEntity(World worldIn, IBlockState state)
    {
        return new TileEntityBarrel();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        //return this.getDefaultState().withProperty(TIER, meta & 0xF);
        return this.getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        //return state.getValue(TIER) & 0xF;
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBarrel)
        {
            return state.withProperty(PULSED, ((TileEntityBarrel) te).isPulsed()).withProperty(TIER, ((TileEntityBarrel) te).getTier());
        }

        return state;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, EnumFacing side, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, side, state, placer, stack);

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBarrel)
        {
            ((TileEntityBarrel) te).setTier(stack.getMetadata() & 0xF);
            ((TileEntityBarrel) te).setIsPulsed(stack.getMetadata() >= 16);
        }
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
    {
        if (willHarvest == true)
        {
            this.onBlockHarvested(world, pos, state, player);
            return true;
        }

        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack)
    {
        // This will cascade down to getDrops()
        super.harvestBlock(worldIn, player, pos, state, te, stack);

        worldIn.setBlockToAir(pos);
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess worldIn, BlockPos pos, IBlockState state, int fortune)
    {
        List<ItemStack> items = new ArrayList<ItemStack>();

        items.add(this.getDroppedItem(worldIn, pos, state, fortune));

        return items;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state)
    {
        return this.getDroppedItem(worldIn, pos, state, 0);
    }

    protected ItemStack getDroppedItem(IBlockAccess worldIn, BlockPos pos, IBlockState state, int fortune)
    {
        Random rand = worldIn instanceof World ? ((World)worldIn).rand : RANDOM;
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityBarrel)
        {
            int meta = ((TileEntityBarrel) te).getTier() & 0xF;

            if (((TileEntityBarrel) te).isPulsed())
            {
                meta += 16;
            }

            ItemStack stack = new ItemStack(this.getItemDropped(state, rand, 0), 1, meta);
            return ((TileEntityBarrel) te).addBlockEntityTag(stack);
        }

        return new ItemStack(this.getItemDropped(state, rand, 0), 1, 0);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (worldIn.isRemote == false && playerIn.capabilities.isCreativeMode &&
            heldItem != null && heldItem.getItem() == Items.NETHER_STAR)
        {
            TileEntity te = worldIn.getTileEntity(pos);

            if (te instanceof TileEntityBarrel)
            {
                boolean success = ((TileEntityBarrel) te).fillBarrel();

                if (success)
                {
                    playerIn.addChatMessage(new TextComponentTranslation("autoverse.chat.barrel.filled"));
                }

                return success;
            }
        }

        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, heldItem, side, hitX, hitY, hitZ);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState iBlockState)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBarrel)
        {
            worldIn.updateComparatorOutputLevel(pos, this);
        }

        worldIn.removeTileEntity(pos);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot)
    {
        return state;
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
    {
        return state;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list)
    {
        for (int meta = 0; meta < 32; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }
}
