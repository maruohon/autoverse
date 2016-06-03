package fi.dy.masa.autoverse.block.base;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import fi.dy.masa.autoverse.gui.client.CreativeTab;

public class BlockAutoverse extends Block
{
    protected final String blockName;
    protected String[] unlocalizedNames;

    public BlockAutoverse(String name, float hardness, int harvestLevel, Material material)
    {
        super(material);
        this.setHardness(hardness);
        this.setHarvestLevel("pickaxe", harvestLevel);
        this.setCreativeTab(CreativeTab.AUTOVERSE_TAB);
        this.setSoundType(SoundType.METAL);
        this.blockName = name;
        this.createUnlocalizedNames();
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return this.getMetaFromState(state);
    }

    public void createUnlocalizedNames()
    {
        this.unlocalizedNames = new String[] { this.blockName };
    }

    public String[] getUnlocalizedNames()
    {
        return this.unlocalizedNames;
    }

    public String[] getItemBlockVariantStrings()
    {
        return this.getUnlocalizedNames();
    }
}
