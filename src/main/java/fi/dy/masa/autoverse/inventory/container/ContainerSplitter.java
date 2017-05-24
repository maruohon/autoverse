package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class ContainerSplitter extends ContainerTile
{
    private final TileEntitySplitter tesp;

    public ContainerSplitter(EntityPlayer player, TileEntitySplitter te)
    {
        super(player, te);

        this.tesp = te;
        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 125);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInputInventory(), 0, 8, 20));

        int posX = 98;
        int posY = 20;
        IItemHandler inv = this.tesp.getResetInventory();

        // Add the Reset Sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 38;
        inv = this.tesp.getResetSequenceBuffer();

        // Add the Reset Sequence matcher slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posX = 8;
        posY = 60;
        inv = this.tesp.getSequenceInventory();

        // Add the reference sequence slots
        for (int slot = 0, col = 0, row = 0; slot < inv.getSlots(); slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(inv, slot, posX + col * 18, posY + row * 18));

            if (++col >= 9)
            {
                col = 0;
                row++;
            }
        }

        // Add the output buffer 1 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getOut1Inventory(), 0, 8, 94));

        // Add the output buffer 2 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getOut2Inventory(), 0, 98, 94));
    }
}
