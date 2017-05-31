package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.InventoryCraftingWrapper;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperCrafter;
import fi.dy.masa.autoverse.tileentity.TileEntityCrafter;

public class ContainerCrafter extends ContainerTile
{
    private final TileEntityCrafter tec;

    public ContainerCrafter(EntityPlayer player, TileEntityCrafter te)
    {
        super(player, te);

        this.tec = te;
        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        // Add the input slot
        if (this.isClient)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.tec.getInventoryInput(), 0, 8, 34));
        }
        else
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, 0, 8, 34));
        }

        int posX = 98;
        int posY = 16;
        ItemHandlerWrapperCrafter crafter = this.tec.getInventoryCrafter();
        IItemHandler inv = crafter.getResetSequence().getSequenceInventory(false);

        // Add the Reset Sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 34;
        // Use a basic inventory to hold the items on the client side
        inv = this.isClient ? new ItemStackHandlerBasic(inv.getSlots()) : crafter.getResetSequence().getSequenceInventory(true);

        // Add the Reset Sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        // Add the empty item marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(crafter.getEmptyMarkerInventory(), 0, 62, 34));

        posX = 116;
        posY = 72;
        inv = crafter.getRecipeSequenceInventory();

        // Add the crafting pattern slots
        for (int row = 0; row < 3; ++row)
        {
            for (int column = 0; column < 3; ++column)
            {
                this.addSpecialSlot(new SlotItemHandlerGeneric(inv, row * 3 + column, posX + column * 18, posY + row * 18));
            }
        }

        posX = 8;
        InventoryCraftingWrapper matrix = this.tec.getCraftingInventory();
        inv = new InvWrapper(matrix);

        // Add the crafting grid slots
        for (int row = 0; row < 3; ++row)
        {
            for (int column = 0; column < 3; ++column)
            {
                this.addSlotToContainer(new SlotItemHandlerGeneric(inv, row * 3 + column, posX + column * 18, posY + row * 18));
            }
        }

        // Add the crafting output slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tec.getInventoryCraftingOutput(), 0, 80, 90));

        // Update the output slot contents
        matrix.markDirty();

        inv = tec.getInventoryOutput();

        // Add the output buffer slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(inv, slot, 8 + slot * 18, 141));
        }
    }
}
