package fi.dy.masa.autoverse.client.render.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import fi.dy.masa.autoverse.block.BlockRedstoneEmitter;
import fi.dy.masa.autoverse.block.BlockRedstoneEmitter.EmitterType;
import fi.dy.masa.autoverse.block.BlockRedstoneEmitter.SideStatus;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.reference.Reference;

public class BakedModelRedstoneEmitter extends BakedModelMachineSlim
{
    private static final ResourceLocation FAKE_LOCATION_BASIC    = new ResourceLocation(Reference.MOD_ID, "models/block/custom/redstone_emitter_basic");
    private static final ResourceLocation FAKE_LOCATION_ADVANCED = new ResourceLocation(Reference.MOD_ID, "models/block/custom/redstone_emitter_advanced");

    private static final ResourceLocation FULL_BLOCK_MODEL  = new ResourceLocation(Reference.MOD_ID, "block/cube_individual");
    private static final ResourceLocation BASE_MODEL_10     = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_main_10_tex_16");
    private static final ResourceLocation SIDE_MODEL_06x03  = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_side_06x03_tex_06x03");
    private static final ResourceLocation SIDE_MODEL_08x03  = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_side_08x03_tex_10x03");

    private static final Map<IBlockState, IBakedModel> MODEL_CACHE = new HashMap<>();

    protected final IModel fullBlockModel;
    private final BlockRedstoneEmitter.EmitterType type;

    private BakedModelRedstoneEmitter(ResourceLocation modelLocation,
                                      ImmutableMap<String, String> textures,
                                      IModelState modelState,
                                      VertexFormat format,
                                      Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        super(modelLocation, textures, modelState, format, bakedTextureGetter);

        this.type = modelLocation.equals(FAKE_LOCATION_ADVANCED) ? BlockRedstoneEmitter.EmitterType.ADVANCED : BlockRedstoneEmitter.EmitterType.BASIC;
        this.fullBlockModel = getModelOrMissing(FULL_BLOCK_MODEL);
    }

    @Override
    synchronized public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand)
    {
        // Item model
        if (state == null)
        {
            state = AutoverseBlocks.REDSTONE_EMITTER.getDefaultState().withProperty(BlockRedstoneEmitter.TYPE, this.type);
        }

        if (state.getValue(BlockRedstoneEmitter.SLIM))
        {
            return super.getQuads(state, side, rand);
        }
        else
        {
            IBakedModel bakedModel = MODEL_CACHE.get(state);

            if (bakedModel == null)
            {
                IModel model = this.fullBlockModel.retexture(this.getFullBlockModelTextures(state));
                bakedModel = model.bake(TRSRTransformation.from(ModelRotation.X0_Y0), this.format, this.bakedTextureGetter);

                MODEL_CACHE.put(state, bakedModel);
            }

            return bakedModel.getQuads(state, side, rand);
        }
    }

    @Override
    protected void getBaseModel(List<IBakedModel> models, IBlockState state, EnumFacing mainFacing)
    {
        IModel baseModel = this.baseModel.retexture(this.getFullBlockModelTextures(state));
        models.add(baseModel.bake(TRSRTransformation.from(ModelRotation.X0_Y0), this.format, this.bakedTextureGetter));
    }

    @Override
    protected void getSideModels(List<IBakedModel> models, IBlockState state, EnumFacing mainFacing)
    {
        boolean advanced = this.type == EmitterType.ADVANCED;

        for (EnumFacing side : EnumFacing.VALUES)
        {
            BlockRedstoneEmitter.SideStatus sideStatus = state.getValue(BlockRedstoneEmitter.SIDES.get(side.getIndex()));

            if (side != mainFacing && (advanced || sideStatus != SideStatus.DISABLED))
            {
                boolean powered = sideStatus == BlockRedstoneEmitter.SideStatus.POWERED;
                IModel sideModel = this.sideModel.retexture(this.getSideModelTextures(powered));
                models.add(sideModel.bake(TRSRTransformation.from(side), this.format, this.bakedTextureGetter));
            }
        }
    }

    private ImmutableMap<String, String> getSideModelTextures(boolean powered)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put("slim_side_end", this.textures.get("rs_" + (powered ? "on" : "off") + "_slim_end"));
        builder.put("slim_side_side", this.textures.get("rs_slim_side"));

        return builder.build();
    }

    private ImmutableMap<String, String> getFullBlockModelTextures(IBlockState state)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        EnumFacing front = state.getValue(BlockAutoverse.FACING);
        boolean slim = state.getValue(BlockRedstoneEmitter.SLIM);

        for (EnumFacing side : EnumFacing.VALUES)
        {
            String sideName = side.toString().toLowerCase();
            String key = slim ? "slim_" + sideName : sideName;

            if (side == front)
            {
                builder.put(key, this.textures.get("front"));
            }
            else
            {
                BlockRedstoneEmitter.SideStatus status = state.getValue(BlockRedstoneEmitter.SIDES.get(side.getIndex()));
                builder.put(key, this.textures.get(status.toString().toLowerCase()));
            }
        }

        return builder.build();
    }

    private static void clearModelCache()
    {
        MODEL_CACHE.clear();
    }

    private static ImmutableMap<ResourceLocation, ModelLoaderBase.IModelFactory> getModelFactories()
    {
        ImmutableMap.Builder<ResourceLocation, ModelLoaderBase.IModelFactory> builder = ImmutableMap.builder();

        final ImmutableList<ResourceLocation> modelDeps = ImmutableList.of(FULL_BLOCK_MODEL, BASE_MODEL_10, SIDE_MODEL_06x03, SIDE_MODEL_08x03);
        final ImmutableList<ResourceLocation> textureDeps = ImmutableList.of(
                new ResourceLocation("autoverse:blocks/redstone_emitter_basic_front"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_basic_side_disabled"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_basic_side_unpowered"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_basic_side_powered"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_basic_out_slim_end_10"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_basic_out_slim_side_10"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_basic_rs_unpowered_slim_end_06"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_basic_rs_powered_slim_end_06"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_basic_rs_slim_side_06"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_front"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_side_unpowered"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_side_powered"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_out_slim_end_10"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_out_slim_side_10"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_rs_unpowered_slim_end_06"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_rs_powered_slim_end_06"),
                new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_rs_slim_side_06")
        );

        builder.put(FAKE_LOCATION_BASIC,    (ml) -> new ModelBase(ml, BakedModelRedstoneEmitter::new, modelDeps, textureDeps, ImmutableMap.of()));
        builder.put(FAKE_LOCATION_ADVANCED, (ml) -> new ModelBase(ml, BakedModelRedstoneEmitter::new, modelDeps, textureDeps, ImmutableMap.of()));

        return builder.build();
    }

    public static ICustomModelLoader getModelLoader()
    {
        return new ModelLoaderBase(getModelFactories(), BakedModelRedstoneEmitter::clearModelCache);
    }

    public static class StateMapper extends StateMapperBase
    {
        private static final ModelResourceLocation LOCATION_BASIC    = new ModelResourceLocation(Reference.MOD_ID + ":redstone_emitter", "type=basic");
        private static final ModelResourceLocation LOCATION_ADVANCED = new ModelResourceLocation(Reference.MOD_ID + ":redstone_emitter", "type=advanced");

        @Override
        protected ModelResourceLocation getModelResourceLocation(IBlockState state)
        {
            switch (state.getValue(BlockRedstoneEmitter.TYPE))
            {
                case ADVANCED:  return LOCATION_ADVANCED;
                case BASIC:
                default:        return LOCATION_BASIC;
            }
        }
    }
}
