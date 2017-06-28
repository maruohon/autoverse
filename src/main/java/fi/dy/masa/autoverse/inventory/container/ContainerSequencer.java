package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IContainerListener;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;

public class ContainerSequencer extends ContainerTile
{
    private final TileEntitySequencer teseq;
    private int outputSlot = -1;

    public ContainerSequencer(EntityPlayer player, TileEntitySequencer te)
    {
        super(player, te);

        this.teseq = te;
        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 74);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int posX = 8;
        int posY = 22;
        IItemHandler inv = this.teseq.getBaseItemHandler();

        // Add the Sequencer slots
        for (int slot = 0, col = 0, row = 0; slot < inv.getSlots(); slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(inv, slot, posX + col * 18, posY + row * 18));

            if (++col >= 9)
            {
                col = 0;
                row++;
            }
        }

        this.customInventorySlots = new MergeSlotRange(0, this.inventorySlots.size());
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        int outputSlot = this.teseq.getOutputSlot();

        if (this.outputSlot != outputSlot)
        {
            for (int i = 0; i < this.listeners.size(); i++)
            {
                IContainerListener listener = this.listeners.get(i);
                listener.sendWindowProperty(this, 0, outputSlot);
            }
        }

        this.outputSlot = outputSlot;
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        if (id == 0)
        {
            this.outputSlot = data;
        }
    }

    public int getExtractSlot()
    {
        return this.outputSlot;
    }
}
