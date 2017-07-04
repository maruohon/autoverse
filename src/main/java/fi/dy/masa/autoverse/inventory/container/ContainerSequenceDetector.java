package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequenceDetector;
import fi.dy.masa.autoverse.tileentity.TileEntitySequenceDetector;

public class ContainerSequenceDetector extends ContainerTile
{
    private final TileEntitySequenceDetector tesd;
    private final ItemHandlerWrapperSequenceDetector detector;
    private int sequenceLength = -1;
    private int matchedLength = -1;

    public ContainerSequenceDetector(EntityPlayer player, TileEntitySequenceDetector te)
    {
        super(player, te);
        this.tesd = te;
        this.detector = te.getDetectorHandler();

        this.reAddSlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 1);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 16, this.inventory, this.tesd.getInventoryIn());

        // Add the sequence end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.detector.getMarkerInventory(), 0, 8, 34));

        // Add the reset sequence slots
        SlotPlacerSequence.create(98, 16, this.detector.getResetSequence(), this).place();

        // Add the detection sequence slots
        SlotPlacerSequence.create(8, 56, this.detector.getDetectionSequence(), this).setAddMatchedSlots(false).place();

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesd.getInventoryOut(), 0, 152, 151));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient)
        {
            return;
        }

        int sequenceLength = this.detector.getCurrentDetectionSequenceLength();
        int matchedLength = this.detector.getDetectionSequence().getCurrentPosition();

        for (int i = 0; i < this.listeners.size(); i++)
        {
            if (sequenceLength != this.sequenceLength)
            {
                this.listeners.get(i).sendWindowProperty(this, 0, sequenceLength);
            }

            if (matchedLength != this.matchedLength)
            {
                this.listeners.get(i).sendWindowProperty(this, 1, matchedLength);
            }
        }

        this.sequenceLength = sequenceLength;
        this.matchedLength = matchedLength;

        super.detectAndSendChanges();
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        super.updateProgressBar(id, data);

        switch (id)
        {
            case 0:
                this.sequenceLength = data;
                break;

            case 1:
                this.matchedLength = data;
                break;
        }
    }

    public ItemHandlerWrapperSequenceDetector getDetector()
    {
        return this.detector;
    }

    public int getSequenceLength()
    {
        return this.sequenceLength;
    }

    public int getMatchedLength()
    {
        return this.matchedLength;
    }
}
