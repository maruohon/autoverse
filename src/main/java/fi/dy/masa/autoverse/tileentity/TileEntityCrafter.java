package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiCrafter;
import fi.dy.masa.autoverse.inventory.ItemHandlerCraftResult;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerCrafter;
import fi.dy.masa.autoverse.inventory.wrapper.InventoryCraftingWrapper;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperContainer;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperCrafter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class TileEntityCrafter extends TileEntityAutoverseInventory
{
    private ItemStackHandlerTileEntity inventoryInput;
    private ItemStackHandlerTileEntity inventoryOutput;
    private ItemStackHandlerTileEntity inventoryCraftingBase;
    private ItemHandlerCraftResult inventoryCraftResult;
    private InventoryCraftingWrapper inventoryCrafting;
    private ItemHandlerWrapperCrafter crafter;
    private int delay = 1;

    public TileEntityCrafter()
    {
        super(ReferenceNames.NAME_BLOCK_CRAFTER);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput         = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutput        = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOut", this);
        this.inventoryCraftingBase  = new ItemStackHandlerTileEntity(2, 9, 64, false, "ItemsCrafting", this);
        this.inventoryCraftResult   = new ItemHandlerCraftResult();
        this.itemHandlerBase        = this.inventoryInput;

        this.inventoryCrafting = new InventoryCraftingWrapper(3, 3, this.inventoryCraftingBase, this.inventoryCraftResult);

        this.crafter = new ItemHandlerWrapperCrafter(
                this.inventoryInput,
                this.inventoryOutput,
                this.inventoryCraftingBase,
                this.inventoryCraftResult,
                this.inventoryCrafting);

        this.itemHandlerExternal = this.crafter;
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer(EntityPlayer player)
    {
        return new ItemHandlerWrapperContainer(this.itemHandlerBase, this.crafter, false);
    }

    @Override
    public void onLoad()
    {
        if (this.getWorld().isRemote == false)
        {
            FakePlayer player = this.getPlayer();
            this.inventoryCrafting.setPlayer(player);
            this.inventoryCraftResult.init(this.inventoryCrafting, this.getWorld(), player, this.posFront);
            this.crafter.onLoad();
        }
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = this.pushItemsToAdjacentInventory(this.inventoryOutput, 0, this.posFront, this.facingOpposite, false);
        boolean movedIn = this.crafter.moveItems();

        if (movedIn || movedOut)
        {
            this.scheduleUpdateIfNeeded(movedIn);
        }
    }

    public ItemHandlerWrapperCrafter getInventoryCrafter()
    {
        return this.crafter;
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
        return this.inventoryCraftResult;
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded(false);
    }

    protected void scheduleUpdateIfNeeded(boolean force)
    {
        if (force ||
            this.inventoryInput.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOutput.getStackInSlot(0).isEmpty() == false)
        {
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    @Override
    public void inventoryChanged(int inventoryId, int slot)
    {
        this.scheduleUpdateIfNeeded(true);
    }

    @Override
    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryCraftingBase);
        this.crafter.dropAllItems(this.getWorld(), this.getPos());
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

        this.crafter.deserializeNBT(nbt);
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

        nbt.merge(this.crafter.serializeNBT());
    }

    @Override
    public ContainerCrafter getContainer(EntityPlayer player)
    {
        return new ContainerCrafter(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiCrafter(this.getContainer(player), this);
    }
}
