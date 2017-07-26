package fi.dy.masa.autoverse.inventory.wrapper;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerWrapperRedstoneLockable extends ItemHandlerWrapperSize
{
    private final TileEntity te;

    public ItemHandlerWrapperRedstoneLockable(IItemHandler baseHandler, TileEntity te)
    {
        super(baseHandler);

        this.te = te;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        if (this.te.getWorld().isBlockPowered(this.te.getPos()))
        {
            return stack;
        }

        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (this.te.getWorld().isBlockPowered(this.te.getPos()))
        {
            return ItemStack.EMPTY;
        }

        return super.extractItem(slot, amount, simulate);
    }
}
