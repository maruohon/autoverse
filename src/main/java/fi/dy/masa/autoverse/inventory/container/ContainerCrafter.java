package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.wrapper.InvWrapper;
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
        this.reAddSlots(8, 174);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        // Add the input slot as a merge slot range, but no other slots
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 1, false);

        // Add the input slot. On the client use the basic underlying inventory, not the wrapper handler.
        this.addSideDependentSlot(0, 8, 30, this.inventory, this.tec.getInventoryInput());

        ItemHandlerWrapperCrafter crafter = this.tec.getInventoryCrafter();
        InventoryCraftingWrapper matrix = this.tec.getCraftingInventory();

        // Add the end marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(crafter.getEndMarkerInventory(), 0, 8, 48));

        // Add the empty item marker slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(crafter.getEmptyMarkerInventory(), 0, 8, 66));

        // Add the reset sequence slots
        this.addSequenceSlots(98, 30, crafter.getResetSequence()).place();

        // Add the crafting pattern slots
        SlotPlacer.create( 8, 97, crafter.getRecipeSequenceInventory(), this).setMaxSlotsPerRow(3).setSlotType(SlotType.SPECIAL).place();

        // Add the crafting grid slots
        SlotPlacer.create(80, 97, new InvWrapper(matrix), this).setMaxSlotsPerRow(3).setSlotType(SlotType.SPECIAL).place();

        // Add the crafting output slot
        this.addSpecialSlot(new SlotItemHandlerGeneric(this.tec.getInventoryCraftingOutput(), 0, 152, 115));

        // Add the output buffer slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tec.getInventoryOutput(), 0, 152, 151));
    }
}
