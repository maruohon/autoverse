package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockSequencer extends BlockAutoverseInventory
{
    public static final int NUM_TIERS = 5;
    public static final PropertyInteger TIER = PropertyInteger.create("tier", 0, NUM_TIERS - 1);

    public BlockSequencer(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(TIER, 0));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, TIER });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        TileEntitySequencer te = new TileEntitySequencer();
        te.setTier(state.getValue(TIER));
        return te;
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TIER, MathHelper.clamp(meta, 0, NUM_TIERS - 1));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TIER);
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        String[] names = new String[NUM_TIERS];

        for (int i = 0; i < names.length; i++)
        {
            names[i] = this.blockName + "_" + i;
        }

        return names;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (int meta = 0; meta < NUM_TIERS; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }
}
