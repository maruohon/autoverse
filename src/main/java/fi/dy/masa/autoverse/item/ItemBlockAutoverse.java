package fi.dy.masa.autoverse.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.block.base.BlockAutoverseTileEntity;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class ItemBlockAutoverse extends ItemBlock
{
    public static final String PRE_BLUE = TextFormatting.BLUE.toString();
    public static final String PRE_GREEN = TextFormatting.GREEN.toString();
    public static final String RST_GRAY = TextFormatting.RESET.toString() + TextFormatting.GRAY.toString();
    public static final String RST_WHITE = TextFormatting.RESET.toString() + TextFormatting.WHITE.toString();

    protected String[] blockNames;

    public ItemBlockAutoverse(Block block)
    {
        super(block);
        this.setHasSubtypes(true);
        this.setMaxDamage(0);

        if (block instanceof BlockAutoverse)
        {
            this.setNames(((BlockAutoverse)block).getUnlocalizedNames());
        }
    }

    public void setNames(String[] names)
    {
        this.blockNames = names;
    }

    @Override
    public int getMetadata(int meta)
    {
        return meta;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState)
    {
        if (! world.setBlockState(pos, newState, 3)) return false;

        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == this.block)
        {
            if (this.block instanceof BlockAutoverseTileEntity)
            {
                ((BlockAutoverseTileEntity) this.block).onBlockPlacedBy(world, pos, side, state, player, stack);
            }
            else
            {
                this.block.onBlockPlacedBy(world, pos, state, player, stack);
            }

            setTileEntityNBT(world, player, pos, stack);
        }

        return true;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        if (this.blockNames != null && stack.getMetadata() < this.blockNames.length)
        {
            return ReferenceNames.getDotPrefixedName("tile." + this.blockNames[stack.getMetadata()]);
        }

        return super.getUnlocalizedName(stack);
    }

    /**
     * Custom addInformation() method, which allows selecting a subset of the tooltip strings.
     */
    @SideOnly(Side.CLIENT)
    public void addInformationSelective(ItemStack stack, EntityPlayer player, List<String> list, boolean advancedTooltips, boolean verbose)
    {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean advancedTooltips)
    {
        ArrayList<String> tmpList = new ArrayList<String>();
        boolean verbose = Autoverse.proxy.isShiftKeyDown();

        // "Fresh" items without NBT data: display the tips before the usual tooltip data
        if (stack.getTagCompound() == null)
        {
            this.addTooltips(stack, tmpList, verbose);

            if (verbose == false && tmpList.size() > 1)
            {
                list.add(I18n.format("autoverse.tooltip.item.holdshiftfordescription"));
            }
            else
            {
                list.addAll(tmpList);
            }
        }

        tmpList.clear();
        this.addInformationSelective(stack, player, tmpList, advancedTooltips, true);

        // If we want the compact version of the tooltip, and the compact list has more than 2 lines, only show the first line
        // plus the "Hold Shift for more" tooltip.
        if (verbose == false && tmpList.size() > 2)
        {
            tmpList.clear();
            this.addInformationSelective(stack, player, tmpList, advancedTooltips, false);
            if (tmpList.size() > 0)
            {
                list.add(tmpList.get(0));
            }
            list.add(I18n.format("autoverse.tooltip.item.holdshift"));
        }
        else
        {
            list.addAll(tmpList);
        }
    }

    @SideOnly(Side.CLIENT)
    public void addTooltips(ItemStack stack, List<String> list, boolean verbose)
    {
        ItemAutoverse.addTooltips(this.getUnlocalizedName(stack) + ".tooltips", list, verbose);
    }

    @SideOnly(Side.CLIENT)
    public boolean isFull3D()
    {
        return true;
    }
}
