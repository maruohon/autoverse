package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
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
        // Add the input slot
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tefi.getInputInventory(), 0, 8, 16));

        int posX = 98;
        int posY = 16;
        IItemHandler inv = this.tefi.getResetInventory();

        // Add the Reset Sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 36;
        inv = this.tefi.getResetSequenceBuffer();

        // Add the Reset Sequence matcher slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posX = 8;
        posY = 66;
        inv = this.tefi.getFilterItemsInventory();

        // Add the Filter slots
        for (int slot = 0, col = 0, row = 0; slot < inv.getSlots(); slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(inv, slot, posX + col * 18, posY + row * 18));

            if (++col >= 9)
            {
                col = 0;
                row++;
            }
        }

        posY = 114;
        inv = this.tefi.getFilteredItemsInventory();

        // Add the filter buffer slots
        for (int slot = 0; slot < 9; slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 144;
        inv = this.tefi.getOutputInventory();

        // Add the output buffer slots
        for (int slot = 0; slot < 9; slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        this.customInventorySlots = new MergeSlotRange(0, this.inventorySlots.size());
    }
}
