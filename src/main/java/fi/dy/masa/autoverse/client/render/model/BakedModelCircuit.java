package fi.dy.masa.autoverse.client.render.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.block.BlockCircuit;
import fi.dy.masa.autoverse.block.BlockCircuit.CircuitType;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.reference.Reference;

public class BakedModelCircuit implements IBakedModel
{
    private static final ResourceLocation BASE_MODEL_LATCH = new ResourceLocation(Reference.MOD_ID, "block/latch_base");
    private static final ResourceLocation SIDE_MODEL_LATCH = new ResourceLocation(Reference.MOD_ID, "block/latch_side");

    private static final Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> QUAD_CACHE = new HashMap<>();

    private final IModel baseModel;
    private final IModel sideModel;
    private final IBakedModel bakedBaseModel;
    private final ImmutableMap<String, String> textures;
    private final VertexFormat format;
    private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
    private final BlockCircuit.CircuitType type;
    private final TextureAtlasSprite particle;

    private BakedModelCircuit(
            BlockCircuit.CircuitType type,
            IModel baseModel, IModel sideModel,
            ImmutableMap<String, String> textures, IModelState modelState, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        this.type = type;
        this.baseModel = baseModel;
        this.sideModel = sideModel;
        this.bakedBaseModel = this.baseModel.bake(modelState, format, bakedTextureGetter);
        this.textures = textures;
        this.format = format;
        this.bakedTextureGetter = bakedTextureGetter;
        this.particle = bakedTextureGetter.apply(new ResourceLocation("autoverse:blocks/latch_rs_base_side"));
    }

    @Override
    public boolean isAmbientOcclusion()
    {
        return this.bakedBaseModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return this.bakedBaseModel.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer()
    {
        return this.bakedBaseModel.isBuiltInRenderer();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemCameraTransforms getItemCameraTransforms()
    {
        return this.bakedBaseModel.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides()
    {
        return this.bakedBaseModel.getOverrides();
    }

    @Override
    public TextureAtlasSprite getParticleTexture()
    {
        return this.particle;
    }

    @Override
    synchronized public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand)
    {
        // Item model
        if (state == null)
        {
            state = AutoverseBlocks.CIRCUIT.getDefaultState().withProperty(BlockCircuit.TYPE, this.type);
        }

        ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> quads = QUAD_CACHE.get(state);

        if (quads == null)
        {
            quads = this.bakeFullModel(state);
            QUAD_CACHE.put(state, quads);
        }

        return quads.get(Optional.ofNullable(side));
    }

    private ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> bakeFullModel(IBlockState state)
    {
        ImmutableMap.Builder<Optional<EnumFacing>, ImmutableList<BakedQuad>> builder = ImmutableMap.builder();
        List<IBakedModel> models = this.getModelParts(state);

        for (EnumFacing face : BakedModelBase.MODEL_FACES)
        {
            ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();

            for (IBakedModel bakedPart : models)
            {
                quads.addAll(bakedPart.getQuads(state, face, 0));
            }

            if (face != null)
            {
                builder.put(Optional.of(face), quads.build());
            }
            else
            {
                builder.put(Optional.empty(), quads.build());
            }
        }

        return builder.build();
    }

    private List<IBakedModel> getModelParts(IBlockState state)
    {
        EnumFacing side;
        IModel model;
        List<IBakedModel> models = new ArrayList<>();
        boolean powered = state.getValue(BlockCircuit.POWERED);
        ImmutableMap<String, String> baseTextures = this.getBaseModelTextures(state.getValue(BlockCircuit.TYPE), powered);

        model = this.baseModel.retexture(baseTextures);
        models.add(model.bake(TRSRTransformation.from(ModelRotation.X0_Y0), this.format, this.bakedTextureGetter));

        side = state.getValue(BlockCircuit.FACING);
        model = this.sideModel.retexture(baseTextures);
        models.add(model.bake(TRSRTransformation.from(side), this.format, this.bakedTextureGetter));

        if (state.getValue(BlockCircuit.TYPE) == CircuitType.LATCH_RS)
        {
            side = state.getValue(BlockCircuit.FACING2);
            model = this.sideModel.retexture(this.getSideModelTexturesLatchRS(state, false));
            models.add(model.bake(TRSRTransformation.from(side), this.format, this.bakedTextureGetter));

            side = state.getValue(BlockCircuit.FACING3);
            model = this.sideModel.retexture(this.getSideModelTexturesLatchRS(state, true));
            models.add(model.bake(TRSRTransformation.from(side), this.format, this.bakedTextureGetter));
        }

        return models;
    }

    private ImmutableMap<String, String> getBaseModelTextures(CircuitType type, boolean powered)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        try
        {
            builder.put("latch_base",       this.textures.get("base"));
            builder.put("latch_side_side",  this.textures.get("out_" + (powered ? "on" : "off") + "_side"));
            builder.put("latch_side_end",   this.textures.get("out_" + (powered ? "on" : "off") + "_end"));
        }
        catch (Exception e)
        {
            Autoverse.logger.warn("Failed to get base model texture name for a Latch of type {}", type, e);
        }

        return builder.build();
    }

    private ImmutableMap<String, String> getSideModelTexturesLatchRS(IBlockState state, boolean isReset)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        try
        {
            builder.put("latch_side_side",  this.textures.get("in_" + (isReset ? "reset" : "set") + "_side"));
            builder.put("latch_side_end",   this.textures.get("in_" + (isReset ? "reset" : "set") + "_end"));
        }
        catch (Exception e)
        {
            Autoverse.logger.warn("Failed to get side model texture name for block state {}", state, e);
        }

        return builder.build();
    }

    private static class ModelCircuit implements IModel
    {
        private final BlockCircuit.CircuitType type;
        private final IModel baseModel;
        private final IModel sideModel;
        private final ImmutableMap<String, String> textures;
        private static final ImmutableList<ResourceLocation> TEXTURE_DEPS;

        static
        {
            ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();

            builder.add(new ResourceLocation("autoverse:blocks/latch_t_base_side"));
            builder.add(new ResourceLocation("autoverse:blocks/latch_rs_base_side"));
            builder.add(new ResourceLocation("autoverse:blocks/latch_rs_out_off_end"));
            builder.add(new ResourceLocation("autoverse:blocks/latch_rs_out_off_side"));
            builder.add(new ResourceLocation("autoverse:blocks/latch_rs_out_on_end"));
            builder.add(new ResourceLocation("autoverse:blocks/latch_rs_out_on_side"));
            builder.add(new ResourceLocation("autoverse:blocks/latch_rs_in_reset_end"));
            builder.add(new ResourceLocation("autoverse:blocks/latch_rs_in_reset_side"));
            builder.add(new ResourceLocation("autoverse:blocks/latch_rs_in_set_end"));
            builder.add(new ResourceLocation("autoverse:blocks/latch_rs_in_set_side"));

            TEXTURE_DEPS = builder.build();
        }

        protected ModelCircuit(BlockCircuit.CircuitType type, IModel baseModel, IModel sideModel, ImmutableMap<String, String> textures)
        {
            this.type = type;
            this.baseModel = baseModel;
            this.sideModel = sideModel;
            this.textures = textures;
        }

        @Override
        public List<ResourceLocation> getDependencies()
        {
            return Lists.newArrayList(BASE_MODEL_LATCH, SIDE_MODEL_LATCH);
        }

        @Override
        public Collection<ResourceLocation> getTextures()
        {
            return TEXTURE_DEPS;
        }

        @Override
        public IModel retexture(ImmutableMap<String, String> textures)
        {
            return new ModelCircuit(this.type, this.baseModel, this.sideModel, textures);
        }

        @Override
        public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
        {
            return new BakedModelCircuit(this.type, this.baseModel, this.sideModel, this.textures, state, format, bakedTextureGetter);
        }
    }

    public static class ModelLoaderCircuit implements ICustomModelLoader
    {
        private static final ResourceLocation FAKE_LOCATION_LATCH_RS = new ResourceLocation(Reference.MOD_ID, "models/block/custom/latch_rs");
        private static final ResourceLocation FAKE_LOCATION_LATCH_T  = new ResourceLocation(Reference.MOD_ID, "models/block/custom/latch_t");

        @Override
        public boolean accepts(ResourceLocation modelLocation)
        {
            return modelLocation.equals(FAKE_LOCATION_LATCH_RS) || modelLocation.equals(FAKE_LOCATION_LATCH_T);
        }

        @Override
        public IModel loadModel(ResourceLocation modelLocation)
        {
            IModel baseModel = ModelLoaderRegistry.getMissingModel();
            IModel sideModel = ModelLoaderRegistry.getMissingModel();

            try
            {
                baseModel  = ModelLoaderRegistry.getModel(BASE_MODEL_LATCH);
                sideModel  = ModelLoaderRegistry.getModel(SIDE_MODEL_LATCH);
            }
            catch (Exception e)
            {
                Autoverse.logger.warn("Failed to load a model for a Latch", e);
            }

            if (modelLocation.equals(FAKE_LOCATION_LATCH_T))
            {
                return new ModelCircuit(BlockCircuit.CircuitType.LATCH_T, baseModel, sideModel, ImmutableMap.of());
            }
            else
            {
                return new ModelCircuit(BlockCircuit.CircuitType.LATCH_RS, baseModel, sideModel, ImmutableMap.of());
            }
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager)
        {
            QUAD_CACHE.clear();
        }
    }

    public static class StateMapper extends StateMapperBase
    {
        private static final ModelResourceLocation LOCATION_LATCH_RS = new ModelResourceLocation(Reference.MOD_ID + ":circuit", "type=latch_rs");
        private static final ModelResourceLocation LOCATION_LATCH_T  = new ModelResourceLocation(Reference.MOD_ID + ":circuit", "type=latch_t");

        @Override
        protected ModelResourceLocation getModelResourceLocation(IBlockState state)
        {
            switch (state.getValue(BlockCircuit.TYPE))
            {
                case LATCH_RS: return LOCATION_LATCH_RS;
                case LATCH_T:  return LOCATION_LATCH_T;
                default:       return LOCATION_LATCH_RS;
            }
        }
    }
}
