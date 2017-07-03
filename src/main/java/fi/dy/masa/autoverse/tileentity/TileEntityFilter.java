package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiFilter;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerFilter;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFilter;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.PositionUtils;

public class TileEntityFilter extends TileEntityAutoverseInventory
{
    protected ItemStackHandlerTileEntity inventoryInput;
    protected ItemStackHandlerTileEntity inventoryOutFiltered;
    protected ItemStackHandlerTileEntity inventoryOutNormal;
    protected ItemHandlerWrapperFilter inventoryFilter;

    protected EnumFacing facingFilteredOut = EnumFacing.WEST;
    protected BlockPos posFilteredOut;
    protected int filterTier;
    protected int delay = 1;

    public TileEntityFilter()
    {
        this(ReferenceNames.NAME_BLOCK_FILTER);
    }

    public TileEntityFilter(String name)
    {
        super(name);
    }

    @Override
    protected void initInventories()
    {
        this.inventoryInput         = new ItemStackHandlerTileEntity(0, 1,  1, false, "ItemsIn", this);
        this.inventoryOutFiltered   = new ItemStackHandlerTileEntity(1, 1, 64, false, "ItemsOutFiltered", this);
        this.inventoryOutNormal     = new ItemStackHandlerTileEntity(2, 1, 64, false, "ItemsOutNormal", this);
        this.itemHandlerBase        = this.inventoryInput;

        this.initFilterInventory();

        this.itemHandlerExternal = this.inventoryFilter;
    }

    protected void initFilterInventory()
    {
        this.inventoryFilter = new ItemHandlerWrapperFilter(
                                    this.getResetSlotCount(),
                                    this.getFilterSlotCount(),
                                    this.inventoryInput,
                                    this.inventoryOutFiltered,
                                    this.inventoryOutNormal);
    }

    public int getResetSlotCount()
    {
        int tier = this.getFilterTier();

        switch (tier)
        {
            case 0: return 2;
            case 1: return 2;
            case 2: return 3;
            case 3: return 4;
            case 4: return 4;
            default: return 1;
        }
    }

    public int getFilterSlotCount()
    {
        int tier = this.getFilterTier();

        switch (tier)
        {
            case 0: return 1;
            case 1: return 3;
            case 2: return 6;
            case 3: return 9;
            case 4: return 18;
            default: return 1;
        }
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 1:
                this.setFilterOutputSide(EnumFacing.getFront(value));
                return true;

            case 2:
                this.delay = value;
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    public IItemHandler getInventoryInput()
    {
        return this.inventoryInput;
    }

    public IItemHandler getInventoryOutFiltered()
    {
        return this.inventoryOutFiltered;
    }

    public IItemHandler getInventoryOutNormal()
    {
        return this.inventoryOutNormal;
    }

    public ItemHandlerWrapperFilter getInventoryFilter()
    {
        return this.inventoryFilter;
    }

    public int getFilterTier()
    {
        return this.filterTier;
    }

    protected int getMaxFilterTier()
    {
        return 4;
    }

    public void setFilterTier(int tier)
    {
        this.filterTier = MathHelper.clamp(tier, 0, this.getMaxFilterTier());

        this.initInventories();
    }

    public void setFilterOutputSide(EnumFacing side)
    {
        if (side != this.facing)
        {
            this.facingFilteredOut = side;
            this.posFilteredOut = this.getPos().offset(side);
        }
    }

    /**
     * This returns the filter-out side's facing as what it would be if the non-match-out
     * side was North, which is the default rotation for the model.
     * That way the filter-out side's texture will be placed on the correct face
     * of the non-rotated model, before the primary facing's rotation is applied to the entire model.
     */
    public EnumFacing getFilterOutRelativeFacing()
    {
        return PositionUtils.getRelativeFacing(this.getFacing(), this.facingFilteredOut);
    }

    @Override
    public boolean onRightClickBlock(EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        ItemStack stack = player.getHeldItem(hand);

        if (stack.isEmpty() && player.isSneaking())
        {
            this.setFilterOutputSide(side);
            this.notifyBlockUpdate(this.getPos());
            this.markDirty();
            return true;
        }

        return false;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        boolean movedOut = false;
        boolean movedIn = false;
        movedOut |= this.pushItemsToAdjacentInventory(this.inventoryOutNormal, 0, this.posFront, this.facingOpposite, false);
        movedOut |= this.pushItemsToAdjacentInventory(this.inventoryOutFiltered, 0, this.posFilteredOut, this.facingFilteredOut.getOpposite(), false);
        movedIn = this.inventoryFilter.moveItems();

        if (movedIn || movedOut)
        {
            this.scheduleUpdateIfNeeded();
        }
    }

    @Override
    public void onNeighborTileChange(IBlockAccess world, BlockPos pos, BlockPos neighbor)
    {
        this.scheduleUpdateIfNeeded();
    }

    protected void scheduleUpdateIfNeeded()
    {
        if (this.inventoryInput.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOutNormal.getStackInSlot(0).isEmpty() == false ||
            this.inventoryOutFiltered.getStackInSlot(0).isEmpty() == false)
        {
            this.scheduleBlockUpdate(this.delay, false);
        }
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        // Setting the tier and thus initializing the inventories needs to
        // happen before reading the inventories!
        this.setFilterTier(nbt.getByte("Tier"));
        this.setFilterOutputSide(EnumFacing.getFront(nbt.getByte("FilterFacing")));

        this.inventoryInput.deserializeNBT(nbt);
        this.inventoryOutNormal.deserializeNBT(nbt);
        this.inventoryOutFiltered.deserializeNBT(nbt);

        this.inventoryFilter.deserializeNBT(nbt);
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("Tier", (byte)this.getFilterTier());
        nbt.setByte("FilterFacing", (byte)this.facingFilteredOut.getIndex());

        return nbt;
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        super.writeItemsToNBT(nbt);

        nbt.merge(this.inventoryInput.serializeNBT());
        nbt.merge(this.inventoryOutNormal.serializeNBT());
        nbt.merge(this.inventoryOutFiltered.serializeNBT());

        nbt.merge(this.inventoryFilter.serializeNBT());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        //tag.setByte("m", (byte)this.inventoryInput.getMode().getId());
        tag.setByte("f", (byte)((this.facingFilteredOut.getIndex() << 4) | this.facing.getIndex()));
        tag.setByte("t", (byte)this.getFilterTier());
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        //this.inventoryInput.setMode(ItemHandlerWrapperFilter.EnumMode.fromId(tag.getByte("m")));
        int facings = tag.getByte("f");
        this.setFacing(EnumFacing.getFront(facings & 0x7));
        this.setFilterOutputSide(EnumFacing.getFront(facings >> 4));

        this.setFilterTier(tag.getByte("t"));

        super.handleUpdateTag(tag);
    }

    @Override
    public void dropInventories()
    {
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryInput);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutNormal);
        InventoryUtils.dropInventoryContentsInWorld(this.getWorld(), this.getPos(), this.inventoryOutFiltered);
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
    public ContainerFilter getContainer(EntityPlayer player)
    {
        return new ContainerFilter(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiFilter(this.getContainer(player), this);
    }
}
