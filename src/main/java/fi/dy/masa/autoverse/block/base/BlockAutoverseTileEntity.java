package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
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
        TileEntity te = worldIn.getTileEntity(pos);
        if (te == null || (te instanceof TileEntityAutoverse) == false)
        {
            return;
        }

        TileEntityAutoverse teav = (TileEntityAutoverse)te;
        NBTTagCompound nbt = stack.getTagCompound();

        // If the ItemStack has a tag containing saved TE data, restore it to the just placed block/TE
        if (nbt != null && nbt.hasKey("BlockEntityData", Constants.NBT.TAG_COMPOUND) == true)
        {
            teav.readFromNBTCustom(nbt.getCompoundTag("BlockEntityData"));
        }
        else
        {
            if (teav instanceof TileEntityAutoverseInventory && stack.hasDisplayName())
            {
                ((TileEntityAutoverseInventory)teav).setInventoryName(stack.getDisplayName());
            }
        }

        // FIXME add the 24-way rotation stuff
        EnumFacing facing = placer.getHorizontalFacing().getOpposite();
        teav.setRotation(facing.getIndex());
    }

    public boolean isTileEntityValid(TileEntity te)
    {
        return te != null && te.isInvalid() == false;
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
