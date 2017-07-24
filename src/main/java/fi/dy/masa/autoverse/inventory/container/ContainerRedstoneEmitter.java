package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.container.base.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperRedstoneEmitter;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitter;

public class ContainerRedstoneEmitter extends ContainerTile
{
    private final TileEntityRedstoneEmitter tere;

    public ContainerRedstoneEmitter(EntityPlayer player, TileEntityRedstoneEmitter te)
    {
        super(player, te);
        this.tere = te;

        this.reAddSlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 1);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 30, this.inventory, this.tere.getInventoryIn());

        ItemHandlerWrapperRedstoneEmitter emitter = this.tere.getEmitterHandlerBasic();

        // Add the end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(emitter.getEndMarkerInventory(), 0, 26, 30));

        // Add the side config enabled marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(emitter.getMarkerInventory(), 0, 26, 48));

        // Add the reset sequence slots
        this.addSequenceSlots(98, 30, emitter.getResetSequence()).place();

        // Add the ON sequence slots
        this.addSequenceSlots(98,  78, emitter.getSwitchOnSequence()).place();

        // Add the OFF sequence slots
        this.addSequenceSlots(98, 126, emitter.getSwitchOffSequence()).place();

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tere.getInventoryOut(), 0, 8, 144));
    }
}
