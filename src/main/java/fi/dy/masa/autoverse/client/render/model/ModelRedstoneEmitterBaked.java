package fi.dy.masa.autoverse.client.render.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
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
import fi.dy.masa.autoverse.block.BlockRedstoneEmitter;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.reference.Reference;

public class ModelRedstoneEmitterBaked implements IBakedModel
{
    private static final String TEX_BASIC_DISABLED      = Reference.MOD_ID + ":blocks/redstone_emitter_basic_side_disabled";
    private static final String TEX_BASIC_FRONT         = Reference.MOD_ID + ":blocks/redstone_emitter_basic_front";
    private static final String TEX_BASIC_POWERED       = Reference.MOD_ID + ":blocks/redstone_emitter_basic_side_powered";
    private static final String TEX_BASIC_UNPOWERED     = Reference.MOD_ID + ":blocks/redstone_emitter_basic_side_unpowered";

    private static final String TEX_ADVANCED_FRONT      = Reference.MOD_ID + ":blocks/redstone_emitter_advanced_front";
    private static final String TEX_ADVANCED_POWERED    = Reference.MOD_ID + ":blocks/redstone_emitter_advanced_side_powered";
    private static final String TEX_ADVANCED_UNPOWERED  = Reference.MOD_ID + ":blocks/redstone_emitter_advanced_side_unpowered";

    private static final Map<IBlockState, IBakedModel> MODEL_CACHE = new HashMap<>();

    private final IModel baseModel;
    private final VertexFormat format;
    private final IBakedModel bakedBaseModel;
    private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
    private final TextureAtlasSprite particle;
    private final ImmutableMap<String, String> textures;
    private final BlockRedstoneEmitter.EmitterType type;

    private ModelRedstoneEmitterBaked(
            BlockRedstoneEmitter.EmitterType type,
            IModel baseModel,
            ImmutableMap<String, String> textures,
            IModelState modelState,
            VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        this.type = type;
        this.textures = textures;
        this.baseModel = baseModel;
        this.format = format;
        this.bakedTextureGetter = bakedTextureGetter;
        this.bakedBaseModel = baseModel.bake(modelState, format, bakedTextureGetter);
        this.particle = bakedTextureGetter.apply(new ResourceLocation(textures.get("powered")));
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
            state = AutoverseBlocks.REDSTONE_EMITTER.getDefaultState().withProperty(BlockRedstoneEmitter.TYPE, this.type);

            for (EnumFacing sideTmp : EnumFacing.values())
            {
                state = state.withProperty(BlockRedstoneEmitter.SIDES.get(sideTmp.getIndex()), BlockRedstoneEmitter.SideStatus.UNPOWERED);
            }
        }

        IBakedModel model = MODEL_CACHE.get(state);

        if (model == null)
        {
            IModel iModel = this.baseModel.retexture(this.getTextures(state));
            model = iModel.bake(new TRSRTransformation(state.getValue(BlockAutoverse.FACING)), this.format, this.bakedTextureGetter);

            MODEL_CACHE.put(state, model);
        }

        return model.getQuads(state, side, rand);
    }

    private ImmutableMap<String, String> getTextures(IBlockState state)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        EnumFacing front = state.getValue(BlockAutoverse.FACING);

        for (EnumFacing side : EnumFacing.values())
        {
            if (side == front)
            {
                builder.put(side.toString().toLowerCase(), this.textures.get("front"));
            }
            else
            {
                BlockRedstoneEmitter.SideStatus status = state.getValue(BlockRedstoneEmitter.SIDES.get(side.getIndex()));
                builder.put(side.toString().toLowerCase(), this.textures.get(status.toString().toLowerCase()));
            }
        }

        return builder.build();
    }

    private static abstract class ModelRedstoneEmitterBase implements IModel
    {
        private static final ResourceLocation MODEL = new ResourceLocation(Reference.MOD_ID, "block/cube_individual");

        protected final Map<String, String> textures = new HashMap<String, String>();
        private final BlockRedstoneEmitter.EmitterType type;

        protected ModelRedstoneEmitterBase(BlockRedstoneEmitter.EmitterType type)
        {
            this.type = type;
        }

        @Override
        public IModelState getDefaultState()
        {
            return ModelRotation.X0_Y0;
        }

        @Override
        public List<ResourceLocation> getDependencies()
        {
            return Lists.newArrayList(MODEL);
        }

        @Override
        public Collection<ResourceLocation> getTextures()
        {
            Collection<ResourceLocation> textures = Lists.newArrayList();

            for (String name : this.getTextureMapping().values())
            {
                textures.add(new ResourceLocation(name));
            }

            return textures;
        }

        @Override
        public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
        {
            IModel model = null;

            try
            {
                model = ModelLoaderRegistry.getModel(MODEL);
            }
            catch (Exception e)
            {
                Autoverse.logger.warn("Failed to load a model for the Redstone Emitter!", e);
            }

            return new ModelRedstoneEmitterBaked(this.type, model, this.getTextureMapping(), state, format, bakedTextureGetter);
        }

        protected ImmutableMap<String, String> getTextureMapping()
        {
            return ImmutableMap.copyOf(this.textures);
        }
    }

    private static class ModelRedstoneEmitterBasic extends ModelRedstoneEmitterBase
    {
        private ModelRedstoneEmitterBasic()
        {
            super(BlockRedstoneEmitter.EmitterType.BASIC);

            this.textures.put("disabled",   TEX_BASIC_DISABLED);
            this.textures.put("front",      TEX_BASIC_FRONT);
            this.textures.put("powered",    TEX_BASIC_POWERED);
            this.textures.put("unpowered",  TEX_BASIC_UNPOWERED);
        }
    }

    private static class ModelRedstoneEmitterAdvanced extends ModelRedstoneEmitterBase
    {
        private ModelRedstoneEmitterAdvanced()
        {
            super(BlockRedstoneEmitter.EmitterType.ADVANCED);

            this.textures.put("disabled",   TEX_BASIC_DISABLED); // unused, but just in case to avoid potential crashes
            this.textures.put("front",      TEX_ADVANCED_FRONT);
            this.textures.put("powered",    TEX_ADVANCED_POWERED);
            this.textures.put("unpowered",  TEX_ADVANCED_UNPOWERED);
        }
    }

    public static class ModelLoaderRedstoneEmitter implements ICustomModelLoader
    {
        private static final ResourceLocation FAKE_LOCATION_BASIC       = new ResourceLocation(Reference.MOD_ID, "models/block/custom/redstone_emitter_basic");
        private static final ResourceLocation FAKE_LOCATION_ADVANCED    = new ResourceLocation(Reference.MOD_ID, "models/block/custom/redstone_emitter_advanced");

        @Override
        public boolean accepts(ResourceLocation modelLocation)
        {
            return modelLocation.equals(FAKE_LOCATION_BASIC) || modelLocation.equals(FAKE_LOCATION_ADVANCED);
        }

        @Override
        public IModel loadModel(ResourceLocation modelLocation) throws Exception
        {
            if (modelLocation.equals(FAKE_LOCATION_ADVANCED))
            {
                return new ModelRedstoneEmitterAdvanced();
            }
            else
            {
                return new ModelRedstoneEmitterBasic();
            }
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager)
        {
            MODEL_CACHE.clear();
        }
    }

    public static class StateMapper extends StateMapperBase
    {
        private static final ModelResourceLocation LOCATION_BASIC       = new ModelResourceLocation(Reference.MOD_ID + ":redstone_emitter", "type=basic");
        private static final ModelResourceLocation LOCATION_ADVANCED    = new ModelResourceLocation(Reference.MOD_ID + ":redstone_emitter", "type=advanced");

        @Override
        protected ModelResourceLocation getModelResourceLocation(IBlockState state)
        {
            switch (state.getValue(BlockRedstoneEmitter.TYPE))
            {
                case BASIC:     return LOCATION_BASIC;
                case ADVANCED:  return LOCATION_ADVANCED;
                default:        return LOCATION_BASIC;
            }
        }
    }
}
