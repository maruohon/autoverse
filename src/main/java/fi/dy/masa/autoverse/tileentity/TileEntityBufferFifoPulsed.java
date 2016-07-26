package fi.dy.masa.autoverse.tileentity;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperFifoPulsed;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntityBufferFifoPulsed extends TileEntityBufferFifo
{
    private ItemHandlerWrapperFifoPulsed itemHandlerFifo;

    public TileEntityBufferFifoPulsed()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO_PULSED);

        this.initInventories();
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, NUM_SLOTS, 1, false, "Items", this);
        this.itemHandlerFifo = new ItemHandlerWrapperFifoPulsed(this.itemHandlerBase);
        this.itemHandlerExternal = this.getFifoInventory();
    }

    @Override
    public ItemHandlerWrapperFifoPulsed getFifoInventory()
    {
        return this.itemHandlerFifo;
    }

    @Override
    protected boolean pushItemsToAdjacentInventory(IItemHandler invSrc, int slot, BlockPos pos, EnumFacing side, boolean spawnInWorld)
    {
        boolean ret = super.pushItemsToAdjacentInventory(invSrc, slot, pos, side, spawnInWorld);

        if (ret == false)
        {
            return false;
        }

        this.getFifoInventory().advancePositions();

        return true;
    }
}
