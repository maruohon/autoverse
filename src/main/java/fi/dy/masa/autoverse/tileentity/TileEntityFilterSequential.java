package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiFilterSequential;
import fi.dy.masa.autoverse.inventory.container.ContainerFilterSequential;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityFilterSequential extends TileEntityFilter
{
    //protected ItemHandlerWrapperFilterSequential inventoryInputSequential;

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
        super.initFilterInventory();
    }

    @Override
    protected int getMaxFilterTier()
    {
        return 2;
    }

    @Override
    protected int getOutBufferFilteredMaxStackSize()
    {
        return 1;
    }

    @Override
    public int getResetSlotCount()
    {
        int tier = this.getFilterTier();

        switch (tier)
        {
            case 0: return 2;
            case 1: return 3;
            case 2: return 4;
            default: return 2;
        }
    }

    @Override
    public int getFilterSlotCount()
    {
        int tier = this.getFilterTier();

        switch (tier)
        {
            case 0: return 4;
            case 1: return 9;
            case 2: return 18;
            default: return 4;
        }
    }

    @Override
    protected int getOutBufferFilteredSlotCount()
    {
        return this.getFilterSlotCount();
    }

    public int getFilterPosition()
    {
        return 0; // TODO this.inventoryInputSequential.getFilterPosition();
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
        return new GuiFilterSequential(this.getContainer(player), this, false);
    }
}
