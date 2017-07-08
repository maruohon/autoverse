package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.TileEntitySequenceDetector;

public class ItemHandlerWrapperSequenceDetector extends ItemHandlerWrapperSequenceBase
{
    private final TileEntitySequenceDetector te;
    private final SequenceMatcherVariable sequenceDetection;

    public ItemHandlerWrapperSequenceDetector(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            TileEntitySequenceDetector te)
    {
        super(4, inventoryInput, inventoryOutput);

        this.te = te;
        this.sequenceDetection  = new SequenceMatcherVariable(45, "SequenceDetection");

        this.getSequenceManager().add(this.sequenceDetection);
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        boolean ret = this.moveInputItemToOutput();

        if (ret && this.sequenceDetection.checkInputItem(stack))
        {
            this.te.onSequenceMatch();
        }

        return ret;
    }

    public SequenceMatcherVariable getDetectionSequence()
    {
        return this.sequenceDetection;
    }
}
