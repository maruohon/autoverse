package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.block.BlockSequencer;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiSequencer;
import fi.dy.masa.autoverse.inventory.ItemHandlerWrapperSequencer;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerSequencer;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class TileEntitySequencer extends TileEntityAutoverseInventory
{
    ItemHandlerWrapperSequencer inventorySequencer;
    protected int tier;

    public TileEntitySequencer()
    {
        this(ReferenceNames.NAME_TILE_ENTITY_SEQUENCER);

        this.initInventories();
    }

    public TileEntitySequencer(String name)
    {
        super(name);
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, this.getNumSlots(), 64, false, "Items", this);
        this.inventorySequencer = new ItemHandlerWrapperSequencer(this.itemHandlerBase);
    }

    public int getNumSlots()
    {
        int tier = this.getTier();

        switch (tier)
        {
            case 0: return 2;
            case 1: return 3;
            case 2: return 6;
            case 3: return 9;
            case 4: return 16;
            default: return 2;
        }
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer()
    {
        return this.getBaseItemHandler();
    }

    public int getTier()
    {
        return this.tier;
    }

    protected int getMaxTier()
    {
        return BlockSequencer.MAX_TIER;
    }

    public void setTier(int tier)
    {
        this.tier = MathHelper.clamp_int(tier, 0, this.getMaxTier());

        this.initInventories();
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        super.readFromNBTCustom(tag);

        // Setting the tier and thus initializing the inventories needs to
        // happen before reading the inventories!
        this.setTier(tag.getByte("Tier"));

        this.inventorySequencer.deserializeNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setByte("Tier", (byte)this.getTier());
        nbt.merge(this.inventorySequencer.serializeNBT());

        return nbt;
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        // Do nothing here, see readFromNBTCustom() above...
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        super.writeItemsToNBT(nbt);

        nbt.merge(this.inventorySequencer.serializeNBT());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag.setByte("t", (byte) this.getTier());

        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.setTier(tag.getByte("t"));

        super.handleUpdateTag(tag);
    }

    @Override
    protected void onRedstoneChange(boolean state)
    {
        if (state == true)
        {
            this.scheduleBlockTick(1, true);
        }
    }

    @Override
    public void onBlockTick(IBlockState state, Random rand)
    {
        super.onBlockTick(state, rand);

        this.pushItemsToAdjacentInventory(this.inventorySequencer, 0, this.posFront, this.facingOpposite, true);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return true;
        }

        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.inventorySequencer);
        }

        return super.getCapability(capability, facing);
    }

    @Override
    public ContainerSequencer getContainer(EntityPlayer player)
    {
        return new ContainerSequencer(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiSequencer(this.getContainer(player), this);
    }
}
