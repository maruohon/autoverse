package fi.dy.masa.autoverse.inventory.slot;

import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.IItemHandler;

public class SlotItemHandlerOffset extends SlotItemHandlerGeneric
{
    protected final ISlotOffset callback;

    public SlotItemHandlerOffset(IItemHandler itemHandler, int index, int xPosition, int yPosition, ISlotOffset callback)
    {
        super(itemHandler, index, xPosition, yPosition);

        this.callback = callback;
    }

    @Override
    public int getSlotIndex()
    {
        int index = super.getSlotIndex() + this.callback.getSlotOffset();
        return MathHelper.clamp(index, 0, this.getItemHandler().getSlots() - 1);
    }
}
