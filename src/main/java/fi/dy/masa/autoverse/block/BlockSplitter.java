package fi.dy.masa.autoverse.block;

import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.block.base.BlockAutoverseInventory;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class BlockSplitter extends BlockAutoverseInventory
{
    public static final PropertyDirection FACING2 = PropertyDirection.create("facing_out2");

    public BlockSplitter(String name, float hardness, int harvestLevel, Material material)
    {
        super(name, hardness, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(FACING2, EnumFacing.DOWN));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, FACING2 });
    }

    @Override
    public TileEntity createTileEntity(World worldIn, IBlockState state)
    {
        return new TileEntitySplitter();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntitySplitter)
        {
            state = state.withProperty(FACING, ((TileEntitySplitter) te).getFacing())
                    .withProperty(FACING2, ((TileEntitySplitter) te).getOut2RelativeFacing());
        }

        return state;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState iBlockState)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntitySplitter)
        {
            ((TileEntitySplitter) te).dropInventories();
            worldIn.updateComparatorOutputLevel(pos, this);
        }

        worldIn.removeTileEntity(pos);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, EnumFacing side, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, side, state, placer, stack);

        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntitySplitter)
        {
            EnumFacing facing2 = BlockPistonBase.getFacingFromEntity(pos, placer);
            if (facing2.getAxis().isVertical())
            {
                facing2 = placer.getHorizontalFacing().rotateY();
            }
            else
            {
                facing2 = facing2.rotateYCCW();
            }

            ((TileEntitySplitter) te).setOutputSide2(facing2);
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (heldItem != null && heldItem.getItem() == Items.STICK)
        {
            if (worldIn.isRemote == false)
            {
                TileEntity te = worldIn.getTileEntity(pos);

                if (te instanceof TileEntitySplitter)
                {
                    ((TileEntitySplitter) te).setOutputSide2(side);
                    worldIn.notifyBlockUpdate(pos, state, state, 3);
                }
            }

            return true;
        }

        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, heldItem, side, hitX, hitY, hitZ);
    }
}
