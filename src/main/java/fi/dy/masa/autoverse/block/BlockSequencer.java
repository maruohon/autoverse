package fi.dy.masa.autoverse.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.autoverse.block.base.BlockMachineSlimBase;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencerProgrammable;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;

public class BlockSequencer extends BlockMachineSlimBase
{
    public static final PropertyEnum<SequencerType> TYPE = PropertyEnum.<SequencerType>create("type", SequencerType.class);

    public BlockSequencer(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(SLIM, false)
                .withProperty(FACING, DEFAULT_FACING)
                .withProperty(TYPE, SequencerType.BASIC));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, SLIM, TYPE });
    }

    @Override
    protected TileEntityAutoverse createTileEntityInstance(World world, IBlockState state)
    {
        switch (state.getValue(TYPE))
        {
            case PROGRAMMABLE:
                return new TileEntitySequencerProgrammable();

            default:
                return new TileEntitySequencer();
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, SequencerType.fromMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getMeta();
    }

    @Override
    public ItemBlockAutoverse createItemBlock()
    {
        ItemBlockAutoverse item = super.createItemBlock();
        item.addPlacementProperty(0, "sequencer.length", Constants.NBT.TAG_BYTE, 1, TileEntitySequencer.MAX_LENGTH);

        // These need to be added specifically for meta 0, because of the property above
        // prevents using the wild card property from the super class.
        // (the existence of a specific meta overrides/hides the wild card meta property - this is a design flaw in the pp system currently)
        item.addPlacementProperty(0, "machine.slim_model", Constants.NBT.TAG_BYTE, 0, 1);
        item.addPlacementPropertyValueNames(0, "machine.slim_model", new String[] { "false", "true" });

        return item;
    }

    @Override
    protected String[] generateUnlocalizedNames()
    {
        return new String[] {
            this.blockName + "_basic",
            this.blockName + "_programmable"
        };
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (SequencerType type : SequencerType.values())
        {
            list.add(new ItemStack(this, 1, type.getMeta()));
        }
    }

    public static enum SequencerType implements IStringSerializable
    {
        BASIC           (0, "basic"),
        PROGRAMMABLE    (1, "programmable");

        private final int meta;
        private final String name;

        private SequencerType(int meta, String name)
        {
            this.meta = meta;
            this.name = name;
        }

        public int getMeta()
        {
            return this.meta;
        }

        @Override
        public String toString()
        {
            return this.name;
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        public static SequencerType fromMeta(int meta)
        {
            return meta == PROGRAMMABLE.meta ? PROGRAMMABLE : BASIC;
        }
    }
}
