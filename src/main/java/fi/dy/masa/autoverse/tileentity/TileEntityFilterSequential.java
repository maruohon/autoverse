package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiFilterSequential;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperFilterSequential;
import fi.dy.masa.autoverse.inventory.container.ContainerFilterSequential;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityFilterSequential extends TileEntityFilter
{
    protected ItemHandlerWrapperFilterSequential inventoryInputSequential;

    public TileEntityFilterSequential()
    {
        this(ReferenceNames.NAME_TILE_ENTITY_FILTER_SEQUENTIAL);
    }

    public TileEntityFilterSequential(String name)
    {
        super(name);
    }

    @Override
    protected void initFilterInventory()
    {
        this.inventoryInputSequential = new ItemHandlerWrapperFilterSequential(this.inventoryReset, this.inventoryFilterItems,
                                             this.inventoryFilterered, this.inventoryOtherOut, this);
        this.inventoryInput = this.inventoryInputSequential;
    }

    public int getFilterPosition()
    {
        return this.inventoryInputSequential.getFilterPosition();
    }

    @Override
    protected int getNumFilterSlots()
    {
        int tier = this.getFilterTier();
        if (tier == 2)
        {
            return 18;
        }

        return tier == 1 ? 9 : 4;
    }

    @Override
    public ContainerFilterSequential getContainer(EntityPlayer player)
    {
        return new ContainerFilterSequential(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiFilterSequential(this.getContainer(player), this);
    }
}
