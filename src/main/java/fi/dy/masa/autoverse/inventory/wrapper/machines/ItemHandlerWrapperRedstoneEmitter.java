package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitter;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class ItemHandlerWrapperRedstoneEmitter extends ItemHandlerWrapperSequenceBase implements ISequenceCallback
{
    private final TileEntityRedstoneEmitter te;
    private final SequenceMatcher sequenceMarkerItem;
    private final SequenceMatcher sequenceSideConfig;
    private final SequenceMatcherVariable sequenceSwitchOn;
    private final SequenceMatcherVariable sequenceSwitchOff;
    private boolean isOn;

    public ItemHandlerWrapperRedstoneEmitter(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            TileEntityRedstoneEmitter te)
    {
        super(4, inventoryInput, inventoryOutput);
        this.te = te;

        this.sequenceMarkerItem = new SequenceMatcher(1, "SequenceMarker");
        this.sequenceSideConfig = new SequenceMatcher(6, "SequenceSides");
        this.sequenceSwitchOn   = new SequenceMatcherVariable(4, "SequenceOn");
        this.sequenceSwitchOff  = new SequenceMatcherVariable(4, "SequenceOff");

        this.sequenceSideConfig.setCallback(this, 0);

        this.getSequenceManager().add(this.sequenceMarkerItem, 1);
        this.getSequenceManager().add(this.sequenceSideConfig);
        this.getSequenceManager().add(this.sequenceSwitchOn);
        this.getSequenceManager().add(this.sequenceSwitchOff);
    }

    @Override
    public void onConfigureSequenceSlot(int callbackId, int slot, boolean finished)
    {
        ItemStack stackHighMarker = this.sequenceMarkerItem.getStackInSlot(0);
        boolean enabled = InventoryUtils.areItemStacksEqual(this.sequenceSideConfig.getStackInSlot(slot), stackHighMarker);
        this.te.setSideEnabled(slot, enabled);
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.setIsOn(false);
        this.te.setSideMask(0);
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        if (this.moveInputItemToOutput())
        {
            if (this.isOn == false && this.sequenceSwitchOn.checkInputItem(stack))
            {
                this.setIsOn(true);
            }
            else if (this.isOn && this.sequenceSwitchOff.checkInputItem(stack))
            {
                this.setIsOn(false);
            }

            return true;
        }

        return false;
    }

    public IItemHandler getMarkerInventory()
    {
        return this.sequenceMarkerItem.getSequenceInventory(false);
    }

    public SequenceMatcher getSwitchOnSequence()
    {
        return this.sequenceSwitchOn;
    }

    public SequenceMatcher getSwitchOffSequence()
    {
        return this.sequenceSwitchOff;
    }

    private void setIsOn(boolean isOn)
    {
        this.isOn = isOn;
        this.te.setIsPowered(isOn);
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setBoolean("On", this.isOn);

        return tag;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.isOn = tag.getBoolean("On");
    }
}
