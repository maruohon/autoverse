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
        super(ReferenceNames.NAME_TILE_ENTITY_FILTER_SEQUENTTIAL);
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

    protected int getNumResetSlots()
    {
        return 3 + this.getFilterTier();
    }

    protected int getNumFilterSlots()
    {
        int tier = this.getFilterTier();
        return tier == 1 ? 18 : 9;
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
