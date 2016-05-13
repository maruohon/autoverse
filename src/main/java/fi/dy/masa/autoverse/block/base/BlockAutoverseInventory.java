package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.EntityUtils;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class BlockAutoverseInventory extends BlockAutoverseTileEntity
{
    public BlockAutoverseInventory(String name, float hardness, int harvestLevel, Material material)
    {
        super(name, hardness, harvestLevel, material);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState iBlockState)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityAutoverseInventory)
        {
            IItemHandler itemHandler = ((TileEntityAutoverseInventory)te).getBaseItemHandler();

            for (int i = 0; itemHandler != null && i < itemHandler.getSlots(); ++i)
            {
                EntityUtils.dropItemStacksInWorld(worldIn, pos, itemHandler.getStackInSlot(i), -1, false);
            }

            worldIn.updateComparatorOutputLevel(pos, this);
        }

        worldIn.removeTileEntity(pos);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (worldIn.isRemote == false)
        {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityAutoverse == false)
            {
                return false;
            }

            if (this.isTileEntityValid(te) == true)
            {
                playerIn.openGui(Autoverse.instance, ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC, worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
        }

        return true;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state)
    {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te == null || te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN) == false)
        {
            return 0;
        }

        IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);

        return inv != null ? InventoryUtils.calcRedstoneFromInventory(inv) : 0;
    }
}
