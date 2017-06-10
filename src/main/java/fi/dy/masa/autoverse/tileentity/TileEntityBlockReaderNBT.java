package fi.dy.masa.autoverse.tileentity;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiBlockReaderNBT;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.inventory.container.ContainerBlockReaderNBT;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.inventory.wrapper.ItemHandlerWrapperExtractOnly;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverseInventory;
import fi.dy.masa.autoverse.util.BlockUtils;
import fi.dy.masa.autoverse.util.NBTUtils;
import fi.dy.masa.autoverse.util.TileUtils;

public class TileEntityBlockReaderNBT extends TileEntityAutoverseInventory
{
    private final ItemStackHandlerBlockReader itemHandlerBlockReader;
    public static final int MAX_LENGTH = 32;
    private int maxLength = 1;

    public TileEntityBlockReaderNBT()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_BLOCK_READER_NBT);

        this.itemHandlerBlockReader = new ItemStackHandlerBlockReader(0, MAX_LENGTH, 1, false, "Items", this);
        this.itemHandlerBase = this.itemHandlerBlockReader;
        this.itemHandlerExternal = new ItemHandlerWrapperExtractOnly(this.itemHandlerBlockReader);
    }

    @Override
    public IItemHandler getWrappedInventoryForContainer(EntityPlayer player)
    {
        return this.itemHandlerBlockReader;
    }

    @Override
    public boolean applyProperty(int propId, int value)
    {
        switch (propId)
        {
            case 1:
                this.setMaxLength(value);
                return true;

            default:
                return super.applyProperty(propId, value);
        }
    }

    public int getMaxLength()
    {
        return this.maxLength;
    }

    public void setMaxLength(int length)
    {
        this.maxLength = MathHelper.clamp(length, 1, MAX_LENGTH);
    }

    private boolean takeOneBlock(int position, FakePlayer player)
    {
        World world = this.getWorld();
        BlockPos pos = this.getPos().offset(this.getFacing(), position + 1);

        if (world.isBlockLoaded(pos, world.isRemote == false) == false)
        {
            return false;
        }

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block.isAir(state, world, pos) == false &&
            state.getBlockHardness(world, pos) >= 0f)
        {
            ItemStack stack = new ItemStack(block, 1, block.damageDropped(state));

            if (stack.isEmpty() == false)
            {
                this.writeBlockStateDataToItem(stack, state);

                if (state.getBlock().hasTileEntity(state))
                {
                    TileEntity te = world.getTileEntity(pos);

                    if (te != null)
                    {
                        TileUtils.storeTileEntityInStack(stack, te, false);
                    }
                }

                if (this.itemHandlerBase.insertItem(position, stack, false).isEmpty())
                {
                    world.restoringBlockSnapshots = true;

                    BlockUtils.setBlockToAirWithBreakSound(world, pos);

                    world.restoringBlockSnapshots = false;

                    return true;
                }
            }
        }

        return false;
    }

    private void writeBlockStateDataToItem(ItemStack stack, IBlockState state)
    {
        if (stack.isEmpty() == false)
        {
            ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());

            if (rl != null)
            {
                NBTTagCompound nbt = NBTUtils.getCompoundTag(stack, "PlacementState", true);
                nbt.setString("name", rl.toString());
                nbt.setByte("meta", (byte) state.getBlock().getMetaFromState(state));
                nbt.setByte("ReadFacing", (byte) this.getFacing().getIndex());
            }
        }
    }

    private void takeAllBlocksFromWorld()
    {
        for (int offset = 0; offset < this.maxLength; offset++)
        {
            this.takeOneBlock(offset, this.getPlayer());
        }
    }

    @Override
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound nbt)
    {
        nbt = super.getUpdatePacketTag(nbt);
        nbt.setByte("len", (byte) this.maxLength);
        return nbt;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        super.handleUpdateTag(tag);

        this.setMaxLength(tag.getByte("len"));
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        this.setMaxLength(nbt.getByte("MaxLength"));

        super.readFromNBTCustom(nbt);
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt = super.writeToNBTCustom(nbt);

        nbt.setByte("MaxLength", (byte) this.maxLength);

        return nbt;
    }

    private void changeInventorySize(int changeAmount)
    {
        int newSize = MathHelper.clamp(this.getMaxLength() + changeAmount, 1, MAX_LENGTH);

        // Shrinking the inventory, only allowed if there are no items in the slots-to-be-removed
        if (changeAmount < 0)
        {
            int changeFinal = 0;

            for (int slot = this.getMaxLength() - 1; slot >= newSize && slot >= 1; slot--)
            {
                if (this.itemHandlerBlockReader.getStackInSlot(slot).isEmpty())
                {
                    changeFinal--;
                }
                else
                {
                    break;
                }
            }

            newSize = MathHelper.clamp(this.getMaxLength() + changeFinal, 1, MAX_LENGTH);
        }

        if (newSize >= 1 && newSize <= MAX_LENGTH)
        {
            this.setMaxLength(newSize);
        }
    }

    @Override
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        // Take block states from world
        if (action == 0)
        {
            this.takeAllBlocksFromWorld();
        }
        // Change max length
        else if (action == 1)
        {
            this.changeInventorySize(element);
        }

        this.markDirty();
    }

    private class ItemStackHandlerBlockReader extends ItemStackHandlerTileEntity
    {
        public ItemStackHandlerBlockReader(int inventoryId, int invSize, int stackLimit, boolean allowCustomStackSizes,
                String tagName, TileEntityAutoverseInventory te)
        {
            super(inventoryId, invSize, stackLimit, allowCustomStackSizes, tagName, te);
        }

        @Override
        public int getSlots()
        {
            return TileEntityBlockReaderNBT.this.getMaxLength();
        }
    }

    @Override
    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return new ContainerBlockReaderNBT(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return new GuiBlockReaderNBT(this.getContainer(player), this);
    }
}
