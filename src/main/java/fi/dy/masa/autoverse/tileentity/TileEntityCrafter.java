package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiCrafter;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerCrafter;
import fi.dy.masa.autoverse.inventory.wrapper.InventoryCraftingWrapper;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperContainer;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperCraftingOutput;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperCrafter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityCrafter extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemStackHandlerTileEntity inventoryCraftingBase;
    private ItemHandlerWrapperCraftingOutput inventoryCraftingOutput;
    private InventoryCraftingWrapper inventoryCrafting;
    private ItemHandlerWrapperCrafter inventoryWrapperCrafter;
    private int delay = 1;

    public TileEntityCrafter()
    {
        super(ReferenceNames.NAME_BLOCK_CRAFTER);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput             = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutput            = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut", this);
        this.inventoryCraftingBase      = new ItemStackHandlerTileEntity(2, 9, 64, false, "ItemsCrafting", this);
        this.inventoryCraftingOutput    = new ItemHandlerWrapperCraftingOutput();
        this.itemHandlerBase            = this.inventoryInput;

        this.inventoryCrafting = new InventoryCraftingWrapper(3, 3,
                this.inventoryCraftingBase, this.inventoryCraftingOutput, null);

        // Set the callback, which is used to consume the ingredient items
        this.inventoryCraftingOutput.setCraftingInventory(this.inventoryCrafting);

        this.inventoryWrapperCrafter = new ItemHandlerWrapperCrafter(
                this.inventoryInput,
                this.inventoryCrafting,
                this.inventoryCraftingOutput,
                this.inventoryOutput);

        this.itemHandlerExternal = new ItemHandlerWrapperCrafterExternal(this.inventoryWrapperCrafter, this.inventoryOutput);
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer(EntityPlayer player)
    {
        return new ItemHandlerWrapperContainer(this.itemHandlerBase, this.inventoryWrapperCrafter, false);
    }

    @Override
    public void onLoad()
    {
        this.inventoryCrafting.setWorld(this.getWorld());
        this.inventoryWrapperCrafter.onLoad(this.getWorld());
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = false;
        boolean movedIn = false;
        movedOut |= this.pushItemsToAdjacentInventory(this.inventoryOutput, 0, this.posFront, this.facingOpposite, false);
        movedIn = this.inventoryWrapperCrafter.moveItems();

        if (movedIn || movedOut)
        {
            this.scheduleUpdateIfNeeded();
        }
    }

    public ItemHandlerWrapperCrafter getInventoryCrafter()
    {
        return this.inventoryWrapperCrafter;
    }

    public IItemHandler getInventoryInput()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOutput()
    {
        return this.inventoryOutput;
    }

    public InventoryCraftingWrapper getCraftingInventory()
    {
        return this.inventoryCrafting;
    }

    public IItemHandler getInventoryCraftingOutput()
    {
        return this.inventoryCraftingOutput;
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded();
    }

    protected void scheduleUpdateIfNeeded()
    {
        if (this.inventoryInput.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOutput.getStackInSlot(0).isEmpty() == false)
        {
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        // Input inventory
        if (inventoryId == 0)
        {
            this.scheduleBlockUpdate(this.delay, true);
        }
    }

    @Override
    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryCraftingBase);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        this.inventoryInput.deserializeNBT(nbt);
        this.inventoryOutput.deserializeNBT(nbt);
        this.inventoryCraftingBase.deserializeNBT(nbt);

        this.inventoryWrapperCrafter.deserializeNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);

        return nbt;
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        super.writeItemsToNBT(nbt);

        nbt.merge(this.inventoryInput.serializeNBT());
        nbt.merge(this.inventoryOutput.serializeNBT());
        nbt.merge(this.inventoryCraftingBase.serializeNBT());

        nbt.merge(this.inventoryWrapperCrafter.serializeNBT());
    }

    private class ItemHandlerWrapperCrafterExternal implements IItemHandler
    {
        private final IItemHandler inventoryInput;
        private final IItemHandler inventoryOutput;

        private ItemHandlerWrapperCrafterExternal(IItemHandler inventoryInput, IItemHandler inventoryOutput)
        {
            this.inventoryInput = inventoryInput;
            this.inventoryOutput = inventoryOutput;
        }

        @Override
        public int getSlots()
        {
            return 2;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return slot == 1 ? this.inventoryOutput.getStackInSlot(0) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            return slot == 1 ? ItemStack.EMPTY : this.inventoryInput.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return slot == 1 ? this.inventoryOutput.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }
    }

    @Override
    public ContainerCrafter getContainer(EntityPlayer player)
    {
        return new ContainerCrafter(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiCrafter(this.getContainer(player), this);
    }
}
