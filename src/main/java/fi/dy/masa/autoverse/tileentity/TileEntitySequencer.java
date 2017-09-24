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
import fi.dy.masa.autoverse.gui.client.GuiSequencer;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerSequencer;
import fi.dy.masa.autoverse.inventory.wrapper.machines.ItemHandlerWrapperSequencer;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;

public class TileEntitySequencer extends TileEntityAutoverseInventory
{
    public static final int MAX_LENGTH = 18;
    private ItemHandlerWrapperSequencer inventorySequencer;

    public TileEntitySequencer()
    {
        super(ReferenceNames.NAME_BLOCK_SEQUENCER);

        this.initInventories();
    }

    @Override
    protected void initInventories()
    {
        this.itemHandlerBase = new ItemStackHandlerTileEntity(0, MAX_LENGTH, 64, false, "Items", this);
        this.inventorySequencer = new ItemHandlerWrapperSequencer(this.itemHandlerBase);
        this.itemHandlerExternal = this.inventorySequencer;
    }

    public ItemHandlerWrapperSequencer getSequencer()
    {
        return this.inventorySequencer;
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        if (propId == 1)
        {
            this.setInventorySize(value);
            return true;
        }
        else
        {
            return super.applyProperty(propId, value);
        }
    }

    @Override
    public int[] getProperties()
    {
        int[] values = super.getProperties();

        values[1] = this.itemHandlerBase.getSlots();

        return values;
    }

    @Override
    public void setPlacementProperties(World world, BlockPos pos, ItemStack stack, NBTTagCompound tag)
    {
        super.setPlacementProperties(world, pos, stack, tag);

        if (tag.hasKey("sequencer.length", Constants.NBT.TAG_BYTE))
        {
            this.setInventorySize(tag.getByte("sequencer.length"));
        }
    }

    @Override
    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
        this.pushItemsToAdjacentInventory(this.inventorySequencer, 0, this.getFrontPosition(), this.getOppositeFacing(), true);
    }

    public int getOutputSlot()
    {
        return this.inventorySequencer.getOutputSlot();
    }

    private void setInventorySize(int size)
    {
        this.itemHandlerBase.setInventorySize(MathHelper.clamp(size, 1, MAX_LENGTH));
        this.markDirty();
    }

    private void changeInventorySize(int changeAmount)
    {
        final int oldSize = this.itemHandlerBase.getSlots();
        int newSize = MathHelper.clamp(oldSize + changeAmount, 1, MAX_LENGTH);

        // Shrinking the inventory, only allowed if there are no items in the slots-to-be-removed
        if (changeAmount < 0)
        {
            int changeFinal = 0;

            for (int slot = oldSize - 1; slot >= newSize && slot >= 1; slot--)
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

            newSize = MathHelper.clamp(oldSize + changeFinal, 1, MAX_LENGTH);
        }

        if (newSize >= 1 && newSize <= MAX_LENGTH)
        {
            this.setInventorySize(newSize);
            this.inventorySequencer.wrapPositions();
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

        tag.setByte("len", (byte) this.itemHandlerBase.getSlots());

        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        this.setInventorySize(tag.getByte("len"));

        super.handleUpdateTag(tag);
    }

    @Override
    public ContainerSequencer getContainer(EntityPlayer player)
    {
        return new ContainerSequencer(player, this);
    }

    @Override
    public Object getGui(EntityPlayer player)
    {
        return new GuiSequencer(this.getContainer(player), this);
    }
}
