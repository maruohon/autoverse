package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFilterSequentialStrict;
import fi.dy.masa.autoverse.tileentity.TileEntityFilterSequentialStrict;

public class ContainerFilterSequentialStrict extends ContainerTile
{
    private final TileEntityFilterSequentialStrict tef;
    private final ItemHandlerWrapperFilterSequentialStrict filter;
    private int matchedLength = -1;

    public ContainerFilterSequentialStrict(EntityPlayer player, TileEntityFilterSequentialStrict te)
    {
        super(player, te);
        this.tef = te;
        this.filter = te.getInventoryFilter();

        this.reAddSlots();
    }

    private void reAddSlots()
    {
        this.reAddSlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 1);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 16, this.inventory, this.tef.getInventoryInput());

        // Add the sequence end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.filter.getEndMarkerInventory(), 0, 26, 16));

        // Add the reset sequence slots
        this.addSequenceSlots(98, 16, this.filter.getResetSequence()).place();

        // Add the filter sequence slots
        this.addSequenceSlots(8, 63, this.filter.getFilterSequence()).place();

        // Add the normal output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tef.getInventoryOutNormal(), 0, 8, 154));

        // Add the filtered output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tef.getInventoryOutFiltered(), 0, 152, 154));
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        if (this.isClient)
        {
            return;
        }

        int matchedLength = this.filter.getMatchedLength();

        if (matchedLength != this.matchedLength)
        {
            this.syncProperty(0, (byte) matchedLength);
            this.matchedLength = matchedLength;
        }
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        switch (id)
        {
            case 0:
                this.matchedLength = value;
                break;
        }
    }

    public int getMatchedLength()
    {
        return this.matchedLength;
    }
}
