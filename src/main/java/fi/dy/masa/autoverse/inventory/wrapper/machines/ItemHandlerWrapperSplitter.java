package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.IItemHandlerSize;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class ItemHandlerWrapperSplitter implements IItemHandler, IItemHandlerSize, INBTSerializable<NBTTagCompound>
{
    //private final TileEntityAutoverseInventory te;
    private final ItemStackHandlerTileEntity inventoryInput;
    private final SequenceMatcher sequenceReset;
    private final SequenceMatcher sequenceSwitch;
    private Mode mode = Mode.CONFIGURE_RESET;
    private boolean outputIsSecondary;

    public ItemHandlerWrapperSplitter(int sequenceLength, ItemStackHandlerTileEntity inventoryInput, TileEntityAutoverseInventory te)
    {
        //this.te = te;
        this.inventoryInput   = inventoryInput;
        this.sequenceReset  = new SequenceMatcher(this.inventoryInput, sequenceLength, "ItemsReset");
        this.sequenceSwitch = new SequenceMatcher(this.inventoryInput, sequenceLength, "ItemsSwitch");
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 1;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 1;
    }

    @Override
    public int getItemStackLimit(int slot, ItemStack stack)
    {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return this.inventoryInput.getStackInSlot(0);
    }

    /*
    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        this.inventoryInput.setStackInSlot(slot, stack);
    }
    */

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        stack = this.inventoryInput.insertItem(0, stack, simulate);

        if (simulate == false && stack.isEmpty())
        {
            switch (this.mode)
            {
                case CONFIGURE_RESET:
                    if (this.sequenceReset.configureSequence())
                    {
                        this.mode = Mode.CONFIGURE_SWITCH;
                    }
                    break;

                case CONFIGURE_SWITCH:
                    if (this.sequenceSwitch.configureSequence())
                    {
                        this.mode = Mode.NORMAL_OPERATION;
                    }
                    break;

                case NORMAL_OPERATION:
                    if (this.sequenceReset.checkInputItem())
                    {
                        this.sequenceReset.reset();
                        this.sequenceSwitch.reset();
                        this.outputIsSecondary = false;
                        this.mode = Mode.CONFIGURE_RESET;
                    }
                    else if (this.sequenceSwitch.checkInputItem())
                    {
                        this.outputIsSecondary = ! this.outputIsSecondary;
                    }
                    break;
            }
        }

        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return ItemStack.EMPTY;
    }

    public boolean secondaryOutputActive()
    {
        return this.outputIsSecondary;
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("State", (byte) ((this.outputIsSecondary ? 0x80 : 0x00) | (this.mode.getId() & 0x7)));

        tag.setTag("SeqReset", this.sequenceReset.serializeNBT());
        tag.setTag("SeqSwitch", this.sequenceSwitch.serializeNBT());

        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Splitter", tag);

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        nbt = nbt.getCompoundTag("Splitter");

        int state = nbt.getByte("State");
        this.mode = Mode.fromid(state & 0x7);
        this.outputIsSecondary = (state & 0x80) != 0;

        this.sequenceReset.deserializeNBT(nbt.getCompoundTag("SeqReset"));
        this.sequenceSwitch.deserializeNBT(nbt.getCompoundTag("SeqSwitch"));
    }

    public SequenceMatcher getResetSequence()
    {
        return this.sequenceReset;
    }

    public SequenceMatcher getSwitchSequence()
    {
        return this.sequenceSwitch;
    }

    public enum Mode
    {
        CONFIGURE_RESET     (0),
        CONFIGURE_SWITCH    (1),
        NORMAL_OPERATION    (2);

        private final int id;

        private Mode (int id)
        {
            this.id = id;
        }

        public int getId()
        {
            return this.id;
        }

        public static Mode fromid(int id)
        {
            return values()[id % values().length];
        }
    }
}
