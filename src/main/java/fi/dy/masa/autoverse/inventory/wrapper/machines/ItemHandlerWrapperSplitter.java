package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.IItemHandlerSize;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;

public class ItemHandlerWrapperSplitter implements IItemHandler, IItemHandlerSize, INBTSerializable<NBTTagCompound>
{
    private final ItemStackHandlerTileEntity inventoryInput;
    private final SequenceMatcher sequenceReset;
    private final SequenceMatcher sequenceSwitch1;
    private boolean outputIsSecondary;
    private Mode mode = Mode.CONFIGURE_RESET;

    public ItemHandlerWrapperSplitter(int sequenceLength, ItemStackHandlerTileEntity inventoryInput)
    {
        this.inventoryInput   = inventoryInput;
        this.sequenceReset  = new SequenceMatcher(sequenceLength, "ItemsReset");
        this.sequenceSwitch1 = new SequenceMatcher(sequenceLength, "ItemsSwitch1");
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

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        stack = this.inventoryInput.insertItem(0, stack, simulate);

        if (simulate == false && stack.isEmpty())
        {
            this.handleInputItem(this.mode, this.inventoryInput.getStackInSlot(0));
        }

        return stack;
    }

    protected void handleInputItem(Mode mode, ItemStack inputStack)
    {
        switch (mode)
        {
            case CONFIGURE_RESET:
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    this.setMode(Mode.CONFIGURE_SWITCH_1);
                }
                break;

            case CONFIGURE_SWITCH_1:
                if (this.getSwitchSequence1().configureSequence(inputStack))
                {
                    this.setMode(Mode.NORMAL_OPERATION);
                }
                break;

            case NORMAL_OPERATION:
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    this.getResetSequence().reset();
                    this.getSwitchSequence1().reset();
                    this.setSecondaryOutputActive(false);
                    this.setMode(Mode.CONFIGURE_RESET);
                }
                else if (this.getSwitchSequence1().checkInputItem(inputStack))
                {
                    this.setSecondaryOutputActive(! this.secondaryOutputActive());
                }
                break;

            default:
                break;
        }
    }

    protected void setMode(Mode mode)
    {
        this.mode = mode;
    }

    protected void setSecondaryOutputActive(boolean secondaryActive)
    {
        this.outputIsSecondary = secondaryActive;
    }

    public boolean secondaryOutputActive()
    {
        return this.outputIsSecondary;
    }

    public SequenceMatcher getResetSequence()
    {
        return this.sequenceReset;
    }

    public SequenceMatcher getSwitchSequence1()
    {
        return this.sequenceSwitch1;
    }

    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag.setByte("State", (byte) ((this.outputIsSecondary ? 0x80 : 0x00) | (this.mode.getId() & 0x7)));
        tag.setTag("SeqReset", this.sequenceReset.serializeNBT());
        tag.setTag("SeqSwitch1", this.sequenceSwitch1.serializeNBT());

        return tag;
    }

    protected void readFromNBT(NBTTagCompound tag)
    {
        int state = tag.getByte("State");
        this.setMode(Mode.fromid(state & 0x7));
        this.setSecondaryOutputActive((state & 0x80) != 0);

        this.sequenceReset.deserializeNBT(tag.getCompoundTag("SeqReset"));
        this.sequenceSwitch1.deserializeNBT(tag.getCompoundTag("SeqSwitch1"));
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Splitter", this.writeToNBT(new NBTTagCompound()));

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        this.readFromNBT(nbt.getCompoundTag("Splitter"));
    }

    public enum Mode
    {
        CONFIGURE_RESET     (0),
        CONFIGURE_SWITCH_1  (1),
        CONFIGURE_SWITCH_2  (2),
        NORMAL_OPERATION    (3);

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
