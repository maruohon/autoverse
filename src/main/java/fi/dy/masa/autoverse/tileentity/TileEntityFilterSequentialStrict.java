package fi.dy.masa.autoverse.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.gui.client.GuiFilterSequentialStrict;
import fi.dy.masa.autoverse.inventory.container.ContainerFilterSequentialStrict;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFilterSequentialStrict;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityFilterSequentialStrict extends TileEntityFilter
{
    private ItemHandlerWrapperFilterSequentialStrict filterStrict;
    private static final int MAX_FILTER_LENGTH = 45;

    public TileEntityFilterSequentialStrict()
    {
        this(ReferenceNames.NAME_BLOCK_FILTER_SEQUENTIAL_STRICT);
    }

    public TileEntityFilterSequentialStrict(String name)
    {
        super(name);
    }

    @Override
    protected void initFilterInventory()
    {
        this.filterStrict = new ItemHandlerWrapperFilterSequentialStrict(
                MAX_FILTER_LENGTH,
                this.inventoryInput,
                this.inventoryOutFiltered,
                this.inventoryOutNormal);

        this.filter = this.filterStrict;
    }

    @Override
    public ItemHandlerWrapperFilterSequentialStrict getInventoryFilter()
    {
        return this.filterStrict;
    }

    @Override
    public ContainerTile getContainer(EntityPlayer player)
    {
        return new ContainerFilterSequentialStrict(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiFilterSequentialStrict(new ContainerFilterSequentialStrict(player, this), this);
    }
}
