package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.autoverse.inventory.slot.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntityFilter;

public class ContainerFilter extends ContainerCustomSlotClick
{
    private final TileEntityFilter tefi;

    public ContainerFilter(EntityPlayer player, TileEntityFilter te)
    {
        super(player, te);

        this.tefi = te;
        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int posX = 8;
        int posY = 29;
        int len = this.tefi.getNumResetSlots();

        // Add the Reset Sequence slots
        for (int slot = 0; slot < len; slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, slot, posX + slot * 18, posY));
        }

        posY = 62;
        len = this.tefi.getNumFilterSlots();

        // Add the Filter slots
        for (int i = 0, slot = this.tefi.getNumResetSlots(), col = 0, row = 0; i < len; i++, slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, slot, posX + col * 18, posY + row * 18));

            if (++col >= 9)
            {
                col = 0;
                row++;
            }
        }

        posY = 113;

        // Add the filter buffer slots
        for (int slot = 0; slot < 9; slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.tefi.getFilteredItemsInventory(), slot, posX + slot * 18, posY));
        }

        posY = 146;

        // Add the output buffer slots
        for (int slot = 0; slot < 9; slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.tefi.getOutputInventory(), slot, posX + slot * 18, posY));
        }

        this.customInventorySlots = new MergeSlotRange(0, this.inventorySlots.size());
    }
}
