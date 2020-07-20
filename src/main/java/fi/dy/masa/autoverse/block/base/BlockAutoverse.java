package fi.dy.masa.autoverse.block.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.autoverse.gui.client.base.CreativeTab;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.tileentity.base.TileEntityAutoverse;
import fi.dy.masa.autoverse.util.EntityUtils;

public class BlockAutoverse extends Block
{
    public static final int BOX_ID_MAIN = 123456;
    public static final EnumFacing DEFAULT_FACING = EnumFacing.NORTH;
    public static final PropertyDirection FACING = BlockDirectional.FACING;
    public static final PropertyDirection FACING_OUT = PropertyDirection.create("facing_out");

    public static final PropertyBool POWERED = PropertyBool.create("powered");

    protected String blockName;
    protected String[] unlocalizedNames;
    protected String[] tooltipNames;
    protected boolean enabled = true;
    protected boolean hasFacing;
    private Map<Integer, AxisAlignedBB> hilightBoxMap;

    public BlockAutoverse(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(material);
        this.setHardness(hardness);
        this.setResistance(resistance);
        this.setHarvestLevel("pickaxe", harvestLevel);
        this.setCreativeTab(CreativeTab.AUTOVERSE_TAB);
        this.setSoundType(SoundType.STONE);
        this.blockName = name;
        this.unlocalizedNames = this.generateUnlocalizedNames();
        this.tooltipNames = this.generateTooltipNames();
    }

    public String getBlockName()
    {
        return this.blockName;
    }

    public boolean getHasFacing()
    {
        return this.hasFacing;
    }

    public boolean hasSpecialHitbox()
    {
        return false;
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return this.getMetaFromState(state);
    }

    protected String[] generateUnlocalizedNames()
    {
        return new String[] { this.blockName };
    }

    /**
     * Generate the names used to look up tooltips for the ItemBlocks.
     * To use a common tooltip for all variants of the block, return an array with exactly one entry in it.
     * @return
     */
    protected String[] generateTooltipNames()
    {
        return this.generateUnlocalizedNames();
    }

    public String[] getUnlocalizedNames()
    {
        return this.unlocalizedNames;
    }

    public String[] getTooltipNames()
    {
        return this.tooltipNames;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public BlockAutoverse setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        return this;
    }

    public ItemBlock createItemBlock()
    {
        return new ItemBlockAutoverse(this);
    }

    public void setPlacementProperties(World world, BlockPos pos, @Nonnull ItemStack stack, @Nonnull NBTTagCompound tag)
    {
        // Only apply the properties if the stack doesn't have NBT data
        // (in case of a BlockEntityTag from an already placed and configured block)
        if (stack.getTagCompound() == null)
        {
            TileEntityAutoverse te = getTileEntitySafely(world, pos, TileEntityAutoverse.class);

            if (te != null)
            {
                te.setPlacementProperties(world, pos, stack, tag);
            }
        }
    }

    public void scheduleBlockUpdate(World world, BlockPos pos, IBlockState state, int delay, boolean force)
    {
        if (force || world.isUpdateScheduled(pos, state.getBlock()) == false)
        {
            world.scheduleUpdate(pos, state.getBlock(), delay);
        }
    }

    public Map<Integer, AxisAlignedBB> getHilightBoxMap()
    {
        return this.hilightBoxMap;
    }

    /**
     * Returns the "id" or "key" of the pointed element's bounding box the player is currently looking at.
     * Invalid hits (ie. misses) return null.
     */
    @Nullable
    private Integer getPointedElementId(World world, BlockPos pos, BlockAutoverse block, @Nonnull Entity entity)
    {
        block.updateBlockHilightBoxes(world.getBlockState(pos).getActualState(world, pos), world, pos);
        return EntityUtils.getPointedBox(EntityUtils.getEyesVec(entity), entity.getLookVec(), 6d, block.getHilightBoxMap());
    }

    protected EnumFacing getTargetedSide(World world, BlockPos pos, IBlockState state, @Nonnull EnumFacing side, @Nonnull EntityPlayer player)
    {
        if (this.getHilightBoxMap() != null)
        {
            Integer key = this.getPointedElementId(world, pos, (BlockAutoverse) state.getBlock(), player);
            return key != null ? this.getSideFromBoxId(key, side) : side;
        }
        else
        {
            return side;
        }
    }

    protected EnumFacing getSideFromBoxId(Integer id, EnumFacing sideIn)
    {
        return id >= BOX_ID_MAIN ? sideIn : EnumFacing.byIndex(id & 0xF);
    }

    protected void createHilightBoxMap()
    {
        this.hilightBoxMap = new ConcurrentHashMap<>();
    }

    public void updateBlockHilightBoxes(IBlockState actualState, World world, BlockPos pos)
    {
    }

    @Nullable
    protected static RayTraceResult collisionRayTraceToBoxes(IBlockState state, BlockAutoverse block,
            World world, BlockPos pos, Vec3d start, Vec3d end)
    {
        block.updateBlockHilightBoxes(state.getActualState(world, pos), world, pos);

        List<RayTraceResult> list = new ArrayList<>();

        for (AxisAlignedBB bb : block.getHilightBoxMap().values())
        {
            RayTraceResult trace = bb.calculateIntercept(start, end);

            if (trace != null)
            {
                list.add(new RayTraceResult(trace.hitVec, trace.sideHit, pos));
            }
        }

        RayTraceResult trace = null;
        // Closest to start, by being furthest from the end point
        double closest = 0.0D;

        for (RayTraceResult traceTmp : list)
        {
            if (traceTmp != null)
            {
                double dist = traceTmp.hitVec.squareDistanceTo(end);

                if (dist > closest)
                {
                    trace = traceTmp;
                    closest = dist;
                }
            }
        }

        return trace;
    }

    /**
     * Returns the tile of the specified class, returns null if it is the wrong type or does not exist.
     * Avoids creating new tile entities when using a ChunkCache (off the main thread).
     * see {@link net.minecraft.block.BlockFlowerPot#getActualState(IBlockState, IBlockAccess, BlockPos)}
     */
    @Nullable
    public static <T extends TileEntity> T getTileEntitySafely(IBlockAccess world, BlockPos pos, Class<T> tileClass)
    {
        TileEntity te;

        if (world instanceof ChunkCache)
        {
            ChunkCache chunkCache = (ChunkCache) world;
            te = chunkCache.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
        }
        else
        {
            te = world.getTileEntity(pos);
        }

        if (tileClass.isInstance(te))
        {
            return tileClass.cast(te);
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis)
    {
        if (this.hasFacing)
        {
            IBlockState state = world.getBlockState(pos).withRotation(Rotation.CLOCKWISE_90);
            world.setBlockState(pos, state, 3);
            return true;
        }

        return false;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation)
    {
        return this.hasFacing ? state.withProperty(FACING, rotation.rotate(state.getValue(FACING))) : state;
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror)
    {
        return this.hasFacing ? state.withRotation(mirror.toRotation(state.getValue(FACING))) : state;
    }
}
