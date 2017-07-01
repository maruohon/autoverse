package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
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

        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 156);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.customInventorySlots = new MergeSlotRange(this.inventorySlots.size(), 1);

        if (this.isClient)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.tere.getInventoryIn(), 0, 8, 30));
        }
        else
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, 0, 8, 30));
        }

        ItemHandlerWrapperRedstoneEmitter emitter = this.tere.getEmitterHandler();
        IItemHandler inv = emitter.getResetSequence().getSequenceInventory(false);

        int posX = 62;
        int posY = 30;

        // Add the reset sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 48;
        // Use a basic inventory to hold the items on the client side
        inv = this.isClient ? new ItemStackHandlerBasic(inv.getSlots()) : emitter.getResetSequence().getSequenceInventory(true);

        // Add the reset sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        // Add the side config marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(emitter.getMarkerInventory(), 0, 8, 66));

        posX = 8;
        posY = 101;
        inv = emitter.getSwitchOnSequence().getSequenceInventory(false);

        // Add the ON sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 119;
        // Use a basic inventory to hold the items on the client side
        inv = this.isClient ? new ItemStackHandlerBasic(inv.getSlots()) : emitter.getSwitchOnSequence().getSequenceInventory(true);

        // Add the ON sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posX = 80;
        posY = 101;
        inv = emitter.getSwitchOffSequence().getSequenceInventory(false);

        // Add the OFF sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 119;
        // Use a basic inventory to hold the items on the client side
        inv = this.isClient ? new ItemStackHandlerBasic(inv.getSlots()) : emitter.getSwitchOffSequence().getSequenceInventory(true);

        // Add the OFF sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tere.getInventoryOut(), 0, 152, 119));
    }
}
