package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
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

        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 1);

        if (this.isClient)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesd.getInventoryIn(), 0, 8, 16));
        }
        else
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, 0, 8, 16));
        }

        // Add the sequence end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.detector.getMarkerInventory(), 0, 8, 34));

        IItemHandler inv = this.detector.getResetSequence().getSequenceInventory(false);

        int posX = 98;
        int posY = 16;

        // Add the reset sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 34;
        // Use a basic inventory to hold the items on the client side
        inv = this.isClient ? new ItemStackHandlerBasic(inv.getSlots()) : this.detector.getResetSequence().getSequenceInventory(true);

        // Add the reset sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posX = 8;
        posY = 56;
        inv = this.detector.getDetectionSequence().getSequenceInventory(false);

        // Add the detection sequence slots
        for (int slot = 0, x = posX; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, x, posY));

            x += 18;

            if (slot % 9 == 8)
            {
                x = posX;
                posY += 18;
            }
        }

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
                this.listeners.get(i).sendProgressBarUpdate(this, 0, sequenceLength);
            }

            if (matchedLength != this.matchedLength)
            {
                this.listeners.get(i).sendProgressBarUpdate(this, 1, matchedLength);
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
