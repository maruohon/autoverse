package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;

public class ContainerSequencer extends ContainerTile
{
    private final TileEntitySequencer teseq;
    private int invSize = -1;
    private int outputSlot = -1;

    public ContainerSequencer(EntityPlayer player, TileEntitySequencer te)
    {
        super(player, te);

        this.teseq = te;
        this.reAddSlots();
    }

    private void reAddSlots()
    {
        this.reAddSlots(8, 86);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        final IItemHandler inv = this.teseq.getBaseItemHandler();
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), inv.getSlots());

        SlotPlacer.create(8, 32, inv, this).place();
    }

    @Override
    public void detectAndSendChanges()
    {
        final int invSize = this.teseq.getBaseItemHandler().getSlots();
        final int outputSlot = this.teseq.getOutputSlot();

        if (this.invSize != invSize)
        {
            this.syncProperty(0, (byte) invSize);
            this.invSize = invSize;
            this.reAddSlots();
        }

        if (this.outputSlot != outputSlot)
        {
            this.syncProperty(1, (byte) outputSlot);
            this.outputSlot = outputSlot;
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        if (id == 0)
        {
            this.teseq.getBaseItemHandler().setInventorySize(value);
            this.reAddSlots();
        }
        else if (id == 1)
        {
            this.outputSlot = value;
        }
        else
        {
            super.receiveProperty(id, value);
        }
    }

    public int getExtractSlot()
    {
        return this.outputSlot;
    }
}
