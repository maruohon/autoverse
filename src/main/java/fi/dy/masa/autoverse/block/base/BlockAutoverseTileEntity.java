package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverseInventory;

public class BlockAutoverseTileEntity extends BlockAutoverse
{
    public static final PropertyDirection FACING = BlockProperties.FACING;

    public BlockAutoverseTileEntity(String name, float hardness, int harvestLevel, Material material)
    {
        super(name, hardness, harvestLevel, material);
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        this.onBlockPlacedBy(worldIn, pos, EnumFacing.UP, state, placer, stack);
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, EnumFacing side, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te == null || (te instanceof TileEntityAutoverse) == false)
        {
            return;
        }

        TileEntityAutoverse teav = (TileEntityAutoverse)te;

        if (teav instanceof TileEntityAutoverseInventory && stack.hasDisplayName())
        {
            ((TileEntityAutoverseInventory)teav).setInventoryName(stack.getDisplayName());
        }

        //teav.setFacing(side.getOpposite());
        teav.setFacing(BlockPistonBase.getFacingFromEntity(pos, placer));
    }

    @Override
    public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn)
    {
        if (worldIn.isRemote == false)
        {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityAutoverse)
            {
                ((TileEntityAutoverse)te).onLeftClickBlock(playerIn);
            }
        }
    }

    public boolean isTileEntityValid(TileEntity te)
    {
        return te != null && te.isInvalid() == false;
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        if (worldIn.isRemote == true)
        {
            return;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityAutoverse)
        {
            ((TileEntityAutoverse)te).onNeighborBlockChange(worldIn, pos, state, neighborBlock);
        }
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot)
    {
        return state.withProperty(FACING, rot.rotate((EnumFacing)state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
    {
        return state.withRotation(mirrorIn.toRotation((EnumFacing)state.getValue(FACING)));
    }
}
