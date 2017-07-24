package fi.dy.masa.autoverse.inventory.container;

import java.util.Arrays;
import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperRedstoneEmitterAdvanced;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitterAdvanced;

public class ContainerRedstoneEmitterAdvanced extends ContainerTile
{
    private final TileEntityRedstoneEmitterAdvanced tere;
    private final ItemHandlerWrapperRedstoneEmitterAdvanced emitter;
    private final int[] matchedLengths = new int[12];

    public ContainerRedstoneEmitterAdvanced(EntityPlayer player, TileEntityRedstoneEmitterAdvanced te)
    {
        super(player, te);
        this.tere = te;
        this.emitter = this.tere.getEmitterHandlerAdvanced();

        Arrays.fill(this.matchedLengths, -1);

        this.reAddSlots(44, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 1);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 16, this.inventory, this.tere.getInventoryIn());

        // Add the end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.emitter.getEndMarkerInventory(), 0, 26, 16));

        // Add the reset sequence slots
        this.addSequenceSlots(170, 16, this.emitter.getResetSequence()).place();

        for (int side = 0, x = 8, y = 63; side < 6; side++)
        {
            // Add the ON sequence slots
            this.addSequenceSlots(x, y, this.emitter.getSwitchOnSequence(side)).setAddMatchedSlots(false).place();

            // Add the OFF sequence slots
            this.addSequenceSlots(x, y + 18, this.emitter.getSwitchOffSequence(side)).setAddMatchedSlots(false).place();

            if (side == 2)
            {
                x = 8;
                y = 110;
            }
            else
            {
                x += 81;
            }
        }

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tere.getInventoryOut(), 0, 188, 151));
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            for (int i = 0; i < 6; i++)
            {
                int matchedLength = this.emitter.getSwitchOnSequence(i).getCurrentPosition();
                int index = i * 2;

                if (matchedLength != this.matchedLengths[index])
                {
                    this.syncProperty(index, (byte) matchedLength);
                    this.matchedLengths[index] = matchedLength;
                }

                matchedLength = this.emitter.getSwitchOffSequence(i).getCurrentPosition();
                index = i * 2 + 1;

                if (matchedLength != this.matchedLengths[index])
                {
                    this.syncProperty(index, (byte) matchedLength);
                    this.matchedLengths[index] = matchedLength;
                }
            }
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveProperty(int id, int value)
    {
        super.receiveProperty(id, value);

        if (id >= 0 && id < 12)
        {
            this.matchedLengths[id] = value;
        }
    }

    public int getMatchedLength(int id)
    {
        return this.matchedLengths[id];
    }
}
