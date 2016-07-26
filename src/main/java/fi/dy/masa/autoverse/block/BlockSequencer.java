package fi.dy.masa.autoverse.block;

import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;

public class BlockSequencer extends BlockAutoverseInventory
{
    public static final int MAX_TIER = 4;
    public static final PropertyInteger TIER = PropertyInteger.create("tier", 0, MAX_TIER);

    public BlockSequencer(String name, float hardness, int harvestLevel, Material material)
    {
        super(name, hardness, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(TIER, 0));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, TIER });
    }

    @Override
    public TileEntity createTileEntity(World worldIn, IBlockState state)
    {
        return new TileEntitySequencer();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TIER, MathHelper.clamp_int(meta, 0, MAX_TIER));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TIER);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityAutoverse)
        {
            state = state.withProperty(FACING, ((TileEntityAutoverse) te).getFacing());
        }

        return state;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, EnumFacing side, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, side, state, placer, stack);

        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntitySequencer)
        {
            int tier = MathHelper.clamp_int(stack.getMetadata() & 0xF, 0, MAX_TIER);
            ((TileEntitySequencer) te).setTier(tier);
        }
    }

    @Override
    protected String[] createUnlocalizedNames()
    {
        String[] names = new String[MAX_TIER + 1];

        for (int i = 0; i < names.length; i++)
        {
            names[i] = this.blockName + "_" + i;
        }

        return names;
    }

    @Override
    public String[] getItemBlockVariantStrings()
    {
        String[] strings = new String[MAX_TIER + 1];

        for (int i = 0; i < strings.length; i++)
        {
            strings[i] = String.valueOf(i);
        }

        return strings;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list)
    {
        for (int meta = 0; meta <= MAX_TIER; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }
}
