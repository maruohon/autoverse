package fi.dy.masa.autoverse.inventory.wrapper;

import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandlerModifiable;

public class InventoryCraftingWrapper extends InventoryCrafting
{
    private final int inventoryWidth;
    private final int inventoryHeight;
    private final IItemHandlerModifiable craftMatrix;
    private final IItemHandlerModifiable resultInventory;
    private World world;

    public InventoryCraftingWrapper(int width, int height,
            IItemHandlerModifiable craftMatrix, IItemHandlerModifiable resultInventory, World world)
    {
        super(null, 0, 0); // dummy

        this.inventoryWidth = width;
        this.inventoryHeight = height;
        this.craftMatrix = craftMatrix;
        this.resultInventory = resultInventory;
        this.world = world;
    }

    public void setWorld(World world)
    {
        this.world = world;
    }

    public World getWorld()
    {
        return this.world;
    }

    @Override
    public int getHeight()
    {
        return this.inventoryHeight;
    }

    @Override
    public int getWidth()
    {
        return this.inventoryWidth;
    }

    @Override
    public int getSizeInventory()
    {
        return this.craftMatrix.getSlots();
    }

    @Override
    @Nullable
    public ItemStack getStackInSlot(int slot)
    {
        return slot >= this.getSizeInventory() ? ItemStack.EMPTY : this.craftMatrix.getStackInSlot(slot);
    }

    @Override
    public boolean isEmpty()
    {
        final int invSize = this.craftMatrix.getSlots();

        for (int slot = 0; slot < invSize; ++slot)
        {
            if (this.craftMatrix.getStackInSlot(slot).isEmpty() == false)
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStackInRowAndColumn(int row, int column)
    {
        if (row >= 0 && row < this.inventoryWidth && column >= 0 && column <= this.inventoryHeight)
        {
            return this.getStackInSlot(row + column * this.inventoryWidth);
        }

        return  ItemStack.EMPTY;
    }

    @Override
    @Nullable
    public ItemStack removeStackFromSlot(int slot)
    {
        return this.craftMatrix.extractItem(slot, Integer.MAX_VALUE, false);
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount)
    {
        ItemStack stack = this.craftMatrix.extractItem(slot, amount, false);

        if (stack.isEmpty() == false)
        {
            this.markDirty();
        }

        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack)
    {
        this.craftMatrix.setStackInSlot(slot, stack);
        this.markDirty();
    }

    @Override
    public int getInventoryStackLimit()
    {
        return this.craftMatrix.getSlotLimit(0);
    }

    @Override
    public void clear()
    {
        for (int slot = 0; slot < this.craftMatrix.getSlots(); slot++)
        {
            this.craftMatrix.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public void markDirty()
    {
        super.markDirty();

        this.resultInventory.setStackInSlot(0, CraftingManager.getInstance().findMatchingRecipe(this, this.world));
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player)
    {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        return true;
    }

    public void openInventory(EntityPlayer player)
    {
    }

    public void closeInventory(EntityPlayer player)
    {
    }

    public int getField(int id)
    {
        return 0;
    }

    public void setField(int id, int value)
    {
    }

    public int getFieldCount()
    {
        return 0;
    }
}
