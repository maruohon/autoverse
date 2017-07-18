package fi.dy.masa.autoverse.tileentity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.autoverse.reference.ReferenceNames;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;
import fi.dy.masa.autoverse.util.NBTUtils;

public class TileEntityPipeRoundRobin extends TileEntityPipeDirectional
{
    protected final EnumFacing[][] roundRobinOutputSidesPerSide;
    private final byte outputSideIndices[] = new byte[6];

    public TileEntityPipeRoundRobin()
    {
        super(ReferenceNames.NAME_TILE_ENTITY_PIPE_ROUNDROBIN);

        this.roundRobinOutputSidesPerSide = new EnumFacing[6][];

        for (int i = 0; i < 6; i++)
        {
            this.roundRobinOutputSidesPerSide[i] = new EnumFacing[0];
        }
    }

    @Override
    public void rotate(Rotation rotation)
    {
        super.rotate(rotation);

        if (rotation != Rotation.NONE)
        {
            this.rotateSidesPerSideArray(this.roundRobinOutputSidesPerSide, rotation);
        }
    }

    @Override
    protected void updateAllValidOutputSidesForInputSide(EnumFacing inputSide)
    {
        // This is used for creating the validOutputSidesperSide array.
        // That array will contain the non-round-robin sides for this type of pipe
        super.updateAllValidOutputSidesForInputSide(inputSide);

        final List<EnumFacing> sides = new ArrayList<EnumFacing>();

        for (EnumFacing side : EnumFacing.values())
        {
            if (side != inputSide &&
                (this.outputSidesMask & (1 << side.getIndex())) != 0 &&
                super.checkHasValidOutputOnSide(side))
            {
                sides.add(side);
            }
        }

        this.roundRobinOutputSidesPerSide[inputSide.getIndex()] = sides.toArray(new EnumFacing[sides.size()]);
    }

    @Override
    protected boolean checkCanOutputOnSide(EnumFacing side)
    {
        // This is used for creating the validOutputSidesperSide array.
        // That array will contain the non-round-robin sides for this type of pipe
        return (this.outputSidesMask & (1 << side.getIndex())) == 0 && super.checkHasValidOutputOnSide(side);
    }

    @Override
    protected boolean hasWorkOnSide(int slot)
    {
        return (this.roundRobinOutputSidesPerSide[slot].length > 0 || this.validOutputSidesPerSide[slot].length > 0) &&
                this.itemHandlerBase.getStackInSlot(slot).isEmpty() == false;
    }

    @Override
    protected InvResult tryPushOutItem(World world, BlockPos pos, int slot)
    {
        //System.out.printf("BASIC tryPushOutItem(): pos: %s, slot: %d, valid sides: %d\n", pos, slot, this.validOutputSidesPerSide[slot].length);
        for (int i = 0; i < this.roundRobinOutputSidesPerSide[slot].length; i++)
        {
            if (this.outputSideIndices[slot] >= this.roundRobinOutputSidesPerSide[slot].length)
            {
                this.outputSideIndices[slot] = 0;
            }

            EnumFacing outputSide = this.roundRobinOutputSidesPerSide[slot][this.outputSideIndices[slot]];
            this.outputSideIndices[slot]++;
            InvResult result = this.tryPushOutItemsToSide(world, pos, outputSide, slot);

            if (result != InvResult.MOVED_NOTHING)
            {
                //System.out.printf("BASIC tryPushOutItem(): pos: %s, side: %s - SUCCESS\n", pos, outputSide);
                return result;
            }
            else
            {
                //System.out.printf("BASIC tryPushOutItem(): pos: %s, side: %s - FAIL\n", pos, outputSide);
            }
        }

        //System.out.printf("tryPushOutItem(): CLOGGED @ %s, item: %s\n", pos, this.itemHandlerBase.getStackInSlot(slot));

        return super.tryPushOutItem(world, pos, slot);
    }

    @Override
    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        super.readFromNBTCustom(nbt);

        NBTUtils.readByteArray(this.outputSideIndices, nbt, "Ind");
        this.readSideArraysFromNBT(this.roundRobinOutputSidesPerSide, nbt, "RRS");
    }

    @Override
    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt = super.writeToNBTCustom(nbt);

        nbt.setByteArray("Ind", this.outputSideIndices);
        this.writeSideArraysToNBT(this.roundRobinOutputSidesPerSide, nbt, "RRS");

        return nbt;
    }
}
