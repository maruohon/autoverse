package fi.dy.masa.autoverse.tileentity;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.gui.client.GuiBufferFifo;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBufferFifo;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperFifo;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class TileEntityBufferFifo extends TileEntityAutoverseInventory
{
    public static final int MAX_LENGTH = 117;
    protected ItemHandlerWrapperFifo itemHandlerFifo;
    protected boolean spawnItemsInWorld;
    private int fifoLength = 26;

    public TileEntityBufferFifo()
    {
        this(ReferenceNames.NAME_TILE_ENTITY_BUFFER_FIFO);
    }

    public TileEntityBufferFifo(String name)
    {
        super(name);

        this.spawnItemsInWorld = true;
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemHandlerFifoBase(0, MAX_LENGTH, 1, false, "Items", this);
        this.itemHandlerFifo = new ItemHandlerWrapperFifo(this.itemHandlerBase);
        this.itemHandlerExternal = this.itemHandlerFifo;
    }

    public ItemHandlerWrapperFifo getFifoInventory()
    {
        return this.itemHandlerFifo;
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 1:
                this.setFifoLength(value);
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    @Override
    public void setPlacementProperties(World world, BlockPos pos, ItemStack stack, NBTTagCompound tag)
    {
        if (tag.hasKey("fifo.size", Constants.NBT.TAG_BYTE))
        {
            this.setFifoLength(tag.getByte("fifo.size"));
            this.markDirty();
        }
    }

    public void setFifoLength(int length)
    {
        this.fifoLength = MathHelper.clamp(length, 1, MAX_LENGTH);
        this.markDirty();
    }

    public int getFifoLength()
    {
        return this.fifoLength;
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        this.pushItemsToAdjacentInventory(this.itemHandlerExternal, 0, this.posFront, this.facingOpposite, this.spawnItemsInWorld);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        this.fifoLength = nbt.getByte("Length");
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt.setByte("Length", (byte) this.fifoLength);

        return super.writeToNBTCustom(nbt);
    }

    @Override
    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        this.getFifoInventory().deserializeNBT(nbt);
    }

    @Override
    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        nbt.merge(this.getFifoInventory().serializeNBT());
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound nbt)
    {
        nbt = super.getUpdatePacketTag(nbt);

        nbt.setByte("flen", (byte) this.getFifoLength());

        return nbt;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.setFifoLength(tag.getByte("flen"));

        super.handleUpdateTag(tag);
    }

    private void changeInventorySize(int changeAmount)
    {
        int newSize = MathHelper.clamp(this.getFifoLength() + changeAmount, 1, MAX_LENGTH);

        // Shrinking the inventory, only allowed if there are no items in the slots-to-be-removed
        if (changeAmount < 0)
        {
            int changeFinal = 0;

            for (int slot = this.getFifoLength() - 1; slot >= newSize && slot >= 1; slot--)
            {
                if (this.itemHandlerBase.getStackInSlot(slot).isEmpty())
                {
                    changeFinal--;
                }
                else
                {
                    break;
                }
            }

            newSize = MathHelper.clamp(this.getFifoLength() + changeFinal, 1, MAX_LENGTH);
        }

        if (newSize >= 1 && newSize <= MAX_LENGTH)
        {
            this.setFifoLength(newSize);
            this.getFifoInventory().wrapPositions();
            this.notifyBlockUpdate(this.getPos());
        }
    }

    @Override
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        if (action == 0)
        {
            this.changeInventorySize(element);
        }
    }

    protected class ItemHandlerFifoBase extends ItemStackHandlerTileEntity
    {
        public ItemHandlerFifoBase(int inventoryId, int invSize, int stackLimit,
                boolean allowCustomStackSizes, String tagName, TileEntityAutoverseInventory te)
        {
            super(inventoryId, invSize, stackLimit, allowCustomStackSizes, tagName, te);
        }

        @Override
        public int getSlots()
        {
            return TileEntityBufferFifo.this.getFifoLength();
        }
    }

    @Override
    public ContainerBufferFifo getContainer(EntityPlayer player)
    {
        return new ContainerBufferFifo(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiBufferFifo(this.getContainer(player), this);
    }
}
