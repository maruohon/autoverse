package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;

public class ContainerSequencer extends ContainerTile
{
    private final TileEntitySequencer teseq;
    private int outputSlot = -1;

    public ContainerSequencer(EntityPlayer player, TileEntitySequencer te)
    {
        super(player, te);

        this.teseq = te;
        this.reAddSlots(8, 74);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        final IItemHandler inv = this.teseq.getBaseItemHandler();
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), inv.getSlots());

        SlotPlacer.create(8, 22, inv, this).place();
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        int outputSlot = this.teseq.getOutputSlot();

        if (this.outputSlot != outputSlot)
        {
            this.syncProperty(0, (byte) outputSlot);
            this.outputSlot = outputSlot;
        }
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        if (id == 0)
        {
            this.outputSlot = value;
        }
    }

    public int getExtractSlot()
    {
        return this.outputSlot;
    }
}
