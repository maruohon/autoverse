package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import fi.dy.masa.autoverse.gui.client.CreativeTab;

public class BlockAutoverse extends Block
{
    public static final PropertyDirection FACING = BlockProperties.FACING;

    public final String blockName;
    protected String[] unlocalizedNames;

    public BlockAutoverse(String name, float hardness, int harvestLevel, Material material)
    {
        super(material);
        this.setHardness(hardness);
        this.setHarvestLevel("pickaxe", harvestLevel);
        this.setCreativeTab(CreativeTab.AUTOVERSE_TAB);
        this.setSoundType(SoundType.METAL);
        this.blockName = name;
        this.unlocalizedNames = this.createUnlocalizedNames();
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return this.getMetaFromState(state);
    }

    protected String[] createUnlocalizedNames()
    {
        return new String[] { this.blockName };
    }

    public String[] getUnlocalizedNames()
    {
        return this.unlocalizedNames;
    }

    public String[] getItemBlockVariantStrings()
    {
        return this.getUnlocalizedNames();
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot)
    {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
    {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }
}
