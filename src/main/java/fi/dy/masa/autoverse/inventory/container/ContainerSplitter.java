package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ICustomSlotSync;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerBasic;
import fi.dy.masa.autoverse.inventory.container.base.ContainerTile;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSplitter;
import fi.dy.masa.autoverse.tileentity.TileEntitySplitter;

public class ContainerSplitter extends ContainerTile implements ICustomSlotSync
{
    private final TileEntitySplitter tesp;
    public boolean secondaryOutput;

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

        ItemHandlerWrapperSplitter splitter = this.tesp.getSplitter();

        if (this.isClient)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInventoryIn(), 0, 8, 30));
        }
        else
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, 0, 8, 30));
        }
        //this.addSlotToContainer(new SlotItemHandlerGeneric(this.inventory, 0, 8, 30));
        //this.addSlotToContainer(new SlotItemHandlerGeneric(splitter, 0, 8, 30));

        IItemHandler inv = splitter.getResetSequence().getSequenceInventory(false);

        int posX = 98;
        int posY = 30;

        // Add the reset sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 48;
        inv = splitter.getResetSequence().getSequenceInventory(true);

        if (this.isClient)
        {
            inv = new ItemStackHandlerBasic(inv.getSlots());
        }

        // Add the reset sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 83;
        inv = splitter.getSwitchSequence().getSequenceInventory(false);

        // Add the switching sequence slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        posY = 101;
        inv = splitter.getSwitchSequence().getSequenceInventory(true);

        if (this.isClient)
        {
            inv = new ItemStackHandlerBasic(inv.getSlots());
        }

        // Add the switching sequence matched slots
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            this.addSpecialSlot(new SlotItemHandlerGeneric(inv, slot, posX + slot * 18, posY));
        }

        // Add the output buffer 1 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInventoryOut1(), 0, 8, 83));

        // Add the output buffer 2 slot
        this.addSlotToContainer(new SlotItemHandlerGeneric(this.tesp.getInventoryOut2(), 0, 44, 83));
    }

    @Override
    public void putCustomStack(int typeId, int slotNum, ItemStack stack)
    {
        this.getSpecialSlots().get(slotNum).putStack(stack);
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            boolean isSecondary = this.tesp.outputIsSecondary();

            for (int i = 0; i < this.listeners.size(); i++)
            {
                if (this.secondaryOutput != isSecondary)
                {
                    this.listeners.get(i).sendProgressBarUpdate(this, 0, isSecondary ? 0x1 : 0x0);
                }
            }

            this.secondaryOutput = isSecondary;
        }

        super.detectAndSendChanges();
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        switch (id)
        {
            case 0:
                this.secondaryOutput = data != 0;
                break;
        }

        super.updateProgressBar(id, data);
    }
}
