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

        // Add the end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.detector.getEndMarkerInventory(), 0, 26, 16));

        // Add the reset sequence slots
        this.addSequenceSlots(98, 16, this.detector.getResetSequence()).place();

        // Add the detection sequence slots
        this.addSequenceSlots(8, 56, this.detector.getDetectionSequence()).setAddMatchedSlots(false).place();

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesd.getInventoryOut(), 0, 152, 151));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            int matchedLength = this.detector.getDetectionSequence().getCurrentPosition();

            if (matchedLength != this.matchedLength)
            {
                this.syncProperty(0, (byte) matchedLength);
                this.matchedLength = matchedLength;
            }
        }

        super.detectAndSendChanges();
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

    public ItemHandlerWrapperSequenceDetector getDetector()
    {
        return this.detector;
    }

    public int getMatchedLength()
    {
        return this.matchedLength;
    }
}
