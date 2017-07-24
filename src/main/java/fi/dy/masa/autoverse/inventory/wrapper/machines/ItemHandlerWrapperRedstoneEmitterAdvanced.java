package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.tileentity.TileEntityRedstoneEmitter;

public class ItemHandlerWrapperRedstoneEmitterAdvanced extends ItemHandlerWrapperSequenceBase
{
    private final TileEntityRedstoneEmitter te;
    private final SequenceMatcherVariable[] switchOnSequences;
    private final SequenceMatcherVariable[] switchOffSequences;

    public ItemHandlerWrapperRedstoneEmitterAdvanced(
            IItemHandler inventoryInput,
            IItemHandler inventoryOutput,
            TileEntityRedstoneEmitter te)
    {
        super(4, inventoryInput, inventoryOutput);
        this.te = te;

        this.switchOnSequences  = new SequenceMatcherVariable[6];
        this.switchOffSequences = new SequenceMatcherVariable[6];

        for (int i = 0; i < 6; i++)
        {
            this.switchOnSequences[i]  = new SequenceMatcherVariable(4, "SequenceOn" + i);
            this.switchOffSequences[i] = new SequenceMatcherVariable(4, "SequenceOff" + i);

            this.getSequenceManager().add(this.switchOnSequences[i]);
            this.getSequenceManager().add(this.switchOffSequences[i]);
        }
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        this.te.setPoweredMask(0);
    }

    @Override
    protected boolean moveInputItemNormal(ItemStack stack)
    {
        if (this.moveInputItemToOutput())
        {
            int powerMask = this.te.getPoweredMask();

            for (int side = 0; side < 6; side++)
            {
                boolean powered = (powerMask & (1 << side)) != 0;

                if (powered && this.switchOffSequences[side].checkInputItem(stack))
                {
                    this.te.setSidePowered(side, false);
                }
                else if (powered == false && this.switchOnSequences[side].checkInputItem(stack))
                {
                    this.te.setSidePowered(side, true);
                }
            }

            return true;
        }

        return false;
    }

    public SequenceMatcher getSwitchOnSequence(int side)
    {
        return this.switchOnSequences[side];
    }

    public SequenceMatcher getSwitchOffSequence(int side)
    {
        return this.switchOffSequences[side];
    }
}
