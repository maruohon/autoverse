package fi.dy.masa.autoverse.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import fi.dy.masa.autoverse.reference.ReferenceGuiIds;
import fi.dy.masa.autoverse.tileentity.TileEntityAutoverseInventory;

public class AutoverseGuiHandler implements IGuiHandler
{
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        if (player == null || world == null)
        {
            return null;
        }

        switch (id)
        {
            case ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC:
                TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
                if (te != null && te instanceof TileEntityAutoverseInventory)
                {
                    return ((TileEntityAutoverseInventory)te).getContainer(player);
                }
                break;

            default:
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        if (player == null || world == null)
        {
            return null;
        }

        switch (id)
        {
            case ReferenceGuiIds.GUI_ID_TILE_ENTITY_GENERIC:
                TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
                if (te != null && te instanceof TileEntityAutoverseInventory)
                {
                    return ((TileEntityAutoverseInventory)te).getGui(player);
                }
                break;

            default:
        }

        return null;
    }

}
