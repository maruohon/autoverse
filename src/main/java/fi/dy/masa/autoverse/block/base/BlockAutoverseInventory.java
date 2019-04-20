package fi.dy.masa.autoverse.block.base;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public abstract class BlockAutoverseInventory extends BlockAutoverseTileEntity
{
    public BlockAutoverseInventory(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        TileEntityAutoverseInventory te = getTileEntitySafely(world, pos, TileEntityAutoverseInventory.class);

        if (te != null)
        {
            te.dropInventories();
        }

        world.updateComparatorOutputLevel(pos, this);
        world.removeTileEntity(pos);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand)
    {
        if (world.isRemote == false)
        {
            TileEntityAutoverseInventory te = getTileEntitySafely(world, pos, TileEntityAutoverseInventory.class);

            if (te != null)
            {
                te.onScheduledBlockUpdate(world, pos, state, rand);
            }
        }
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state)
    {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos)
    {
        TileEntityAutoverseInventory te = getTileEntitySafely(world, pos, TileEntityAutoverseInventory.class);

        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH))
        {
            IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH);

            return inv != null ? InventoryUtils.calcRedstoneFromInventory(inv) : 0;
        }

        return 0;
    }
}
