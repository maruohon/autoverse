package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.BlockSequencer;
import fi.dy.masa.autoverse.gui.client.GuiSequencer;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerSequencer;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequencer;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class TileEntitySequencer extends TileEntityAutoverseInventory
{
    private ItemHandlerWrapperSequencer inventorySequencer;
    protected int tier;

    public TileEntitySequencer()
    {
        this(ReferenceNames.NAME_BLOCK_SEQUENCER);

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
        this.itemHandlerExternal = this.inventorySequencer;
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
            case 4: return 18;
            default: return 2;
        }
    }

    public int getOutputSlot()
    {
        return this.inventorySequencer.getOutputSlot();
    }

    public int getTier()
    {
        return this.tier;
    }

    protected int getMaxTier()
    {
        return BlockSequencer.NUM_TIERS - 1;
    }

    public void setTier(int tier)
    {
        this.tier = MathHelper.clamp(tier, 0, this.getMaxTier());

        this.initInventories();
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        this.pushItemsToAdjacentInventory(this.inventorySequencer, 0, this.posFront, this.facingOpposite, true);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound tag)
    {
        // Setting the tier and thus initializing the inventories needs to
        // happen before reading the inventories!
        this.setTier(tag.getByte("Tier"));

        super.readFromNBTCustom(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);

        nbt.setByte("Tier", (byte) this.getTier());

        return nbt;
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        this.inventorySequencer.deserializeNBT(nbt);
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        nbt.merge(this.inventorySequencer.serializeNBT());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag = super.getUpdatePacketTag(tag);

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
