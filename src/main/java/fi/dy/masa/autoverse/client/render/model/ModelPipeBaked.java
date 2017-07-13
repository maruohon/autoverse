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
import fi.dy.masa.autoverse.block.BlockPipe;
import fi.dy.masa.autoverse.block.BlockPipe.Connection;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.PositionUtils;

public class ModelPipeBaked implements IBakedModel
{
    private static final String TEX_BASIC         = Reference.MOD_ID + ":blocks/pipe_basic";
    private static final String TEX_EXTRACTION    = Reference.MOD_ID + ":blocks/pipe_extraction";
    private static final String TEX_DIRECTIONAL   = Reference.MOD_ID + ":blocks/pipe_directional";

    private static final EnumFacing[] MODEL_FACES = new EnumFacing[] {
            EnumFacing.DOWN, EnumFacing.UP,
            EnumFacing.NORTH, EnumFacing.SOUTH,
            EnumFacing.WEST, EnumFacing.EAST,
            null
    };
    private static final Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> QUAD_CACHE = new HashMap<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>>();
    private final IModel baseModel;
    private final IModel sideModelBasic;
    private final IModel sideModelExtraction;
    private final IModel sideModelDirectional;
    private final IModel[] cornerModels;
    private final VertexFormat format;
    private final IBakedModel bakedBaseModel;
    private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
    private final TextureAtlasSprite particle;

    private ModelPipeBaked(ModelPipe pipeModel, IModel baseModel, IModel sideModel, IModel[] cornerModels,
            IModelState modelState, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        ImmutableMap<String, String> textures = pipeModel.getTextureMapping();
        this.baseModel            = baseModel.retexture(textures);
        this.sideModelBasic       = sideModel.retexture(ImmutableMap.of("texture", textures.get("texture")));
        this.sideModelExtraction  = sideModel.retexture(ImmutableMap.of("texture", textures.get("texture")));
        this.sideModelDirectional = sideModel.retexture(ImmutableMap.of("texture", textures.get("texture")));
        this.cornerModels = new IModel[cornerModels.length];

        for (int i = 0; i < this.cornerModels.length; i++)
        {
            this.cornerModels[i] = cornerModels[i].retexture(ImmutableMap.of("texture", textures.get("texture")));
        }

        this.format = format;
        this.bakedTextureGetter = bakedTextureGetter;
        this.bakedBaseModel = this.baseModel.bake(modelState, format, bakedTextureGetter);
        this.particle = bakedTextureGetter.apply(new ResourceLocation(textures.get("particle")));
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
            return this.bakedBaseModel.getQuads(state, side, rand);
        }

        ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> quads = QUAD_CACHE.get(state);

        //if (quads == null)
        {
            //IModelState modelState = new ModelStateComposition(new TRSRTransformation(state.getValue(BlockInserter.FACING)), this.modelState);
            IModelState modelState = new TRSRTransformation(ModelRotation.X0_Y0);
            IBakedModel bakedBaseModel = this.baseModel.bake(modelState, this.format, this.bakedTextureGetter);

            quads = this.bakeFullModel(bakedBaseModel, state, side);
            QUAD_CACHE.put(state, quads);

            return quads.get(Optional.ofNullable(side));
        }

        //return QUAD_CACHE.get(state).get(Optional.ofNullable(side));
    }

    private ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> bakeFullModel(IBakedModel baseModel, IBlockState state, @Nullable EnumFacing side)
    {
        ImmutableMap.Builder<Optional<EnumFacing>, ImmutableList<BakedQuad>> builder = ImmutableMap.builder();
        List<IBakedModel> sideModels = this.getSideModels(state);
        List<IBakedModel> middleModels = this.getMiddleModelPieces(state);

        for (EnumFacing face : MODEL_FACES)
        {
            ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();

            for (IBakedModel bakedPart : middleModels)
            {
                quads.addAll(bakedPart.getQuads(state, face, 0));
            }

            for (IBakedModel bakedPart : sideModels)
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

    private List<IBakedModel> getMiddleModelPieces(IBlockState state)
    {
        List<IBakedModel> models = new ArrayList<IBakedModel>();

        this.addMiddleModelPieces(state, PositionUtils.SIDES_Y, EnumFacing.UP, models);
        this.addMiddleModelPieces(state, PositionUtils.SIDES_X, EnumFacing.EAST, models);
        this.addMiddleModelPieces(state, PositionUtils.SIDES_Z, EnumFacing.SOUTH, models);

        this.addCornerPieces(state, models, EnumFacing.UP, null);
        this.addCornerPieces(state, models, EnumFacing.DOWN, new TRSRTransformation(ModelRotation.X180_Y180));

        return models;
    }

    private void addMiddleModelPieces(IBlockState state, EnumFacing[] sides, EnumFacing rotationAxis, List<IBakedModel> models)
    {
        // NORTH, SOUTH, WEST, EAST
        TRSRTransformation[] trY = new TRSRTransformation[] {
                new TRSRTransformation(ModelRotation.X0_Y0),
                new TRSRTransformation(ModelRotation.X0_Y180),
                new TRSRTransformation(ModelRotation.X0_Y270),
                new TRSRTransformation(ModelRotation.X0_Y90)
        };

        // DOWN, UP, NORTH, SOUTH
        TRSRTransformation[] trX = new TRSRTransformation[] {
                new TRSRTransformation(ModelRotation.X90_Y90),
                new TRSRTransformation(ModelRotation.X270_Y270),
                new TRSRTransformation(ModelRotation.X90_Y270),
                new TRSRTransformation(ModelRotation.X270_Y90)
        };

        // DOWN, UP, WEST, EAST
        TRSRTransformation[] trZ = new TRSRTransformation[] {
                new TRSRTransformation(ModelRotation.X90_Y180),
                new TRSRTransformation(ModelRotation.X270_Y0),
                new TRSRTransformation(ModelRotation.X270_Y180),
                new TRSRTransformation(ModelRotation.X90_Y0)
        };
        int i = 0;

        for (EnumFacing side : sides)
        {
            EnumFacing other = PositionUtils.rotateAround(side, rotationAxis);
            boolean conn1 = state.getValue(BlockPipe.CONNECTIONS.get(side.getIndex())) != Connection.NONE;
            boolean conn2 = state.getValue(BlockPipe.CONNECTIONS.get(other.getIndex())) != Connection.NONE;
            TRSRTransformation tr;
            if (sides == PositionUtils.SIDES_Y)         { tr = trY[i]; }
            else if (sides == PositionUtils.SIDES_X)    { tr = trX[i]; }
            else                                        { tr = trZ[i]; }

            // When there is no connection on either of the sides around an edge,
            // or when there is a connection on both sides, then a vertical strip is added to that edge.
            //if (conn1 == conn2)

            // No connection on either side
            if (conn1 == false && conn2 == false)
            {
                models.add(this.baseModel.bake(tr, this.format, this.bakedTextureGetter));
            }

            i++;
        }
    }

    private void addCornerPieces(IBlockState state, List<IBakedModel> models, EnumFacing upAxis, @Nullable TRSRTransformation initialRot)
    {
        // NORTH, SOUTH, WEST, EAST
        TRSRTransformation[] trY = new TRSRTransformation[] {
                new TRSRTransformation(ModelRotation.X0_Y0),
                new TRSRTransformation(ModelRotation.X0_Y180),
                new TRSRTransformation(ModelRotation.X0_Y270),
                new TRSRTransformation(ModelRotation.X0_Y90)
        };

        int i = 0;

        for (EnumFacing side : PositionUtils.SIDES_Y)
        {
            int index = 0;
            EnumFacing other = PositionUtils.rotateAround(side, upAxis);

            if (state.getValue(BlockPipe.CONNECTIONS.get(upAxis.getIndex())) != Connection.NONE)
            {
                index |= 0x1;
            }

            if (state.getValue(BlockPipe.CONNECTIONS.get(other.getIndex())) != Connection.NONE)
            {
                index |= 0x2;
            }

            if (state.getValue(BlockPipe.CONNECTIONS.get(side.getIndex())) != Connection.NONE)
            {
                index |= 0x4;
            }

            TRSRTransformation tr = initialRot != null ? trY[i].compose(initialRot) : trY[i];
            models.add(this.cornerModels[index].bake(tr, this.format, this.bakedTextureGetter));

            i++;
        }
    }

    private List<IBakedModel> getSideModels(IBlockState state)
    {
        List<IBakedModel> models = new ArrayList<IBakedModel>();

        for (EnumFacing side : EnumFacing.values())
        {
            BlockPipe.Connection conn = state.getValue(BlockPipe.CONNECTIONS.get(side.getIndex()));

            switch (conn)
            {
                case BASIC:
                    models.add(this.sideModelBasic.bake(new TRSRTransformation(side), this.format, this.bakedTextureGetter));
                    break;

                case EXTRACT:
                    models.add(this.sideModelExtraction.bake(new TRSRTransformation(side), this.format, this.bakedTextureGetter));
                    break;

                case OUTPUT:
                    models.add(this.sideModelDirectional.bake(new TRSRTransformation(side), this.format, this.bakedTextureGetter));
                    break;

                default:
            }
        }

        return models;
    }

    private static abstract class ModelPipe implements IModel
    {
        private static final ResourceLocation BASE_MODEL     = new ResourceLocation(Reference.MOD_ID, "block/pipe_strip_y");
        private static final ResourceLocation SIDE_MODEL     = new ResourceLocation(Reference.MOD_ID, "block/pipe_side_strips");

        protected final Map<String, String> textures = new HashMap<String, String>();

        @Override
        public IModelState getDefaultState()
        {
            return ModelRotation.X0_Y0;
        }

        @Override
        public List<ResourceLocation> getDependencies()
        {
            List<ResourceLocation> models = Lists.newArrayList(BASE_MODEL, SIDE_MODEL);

            for (int i = 0; i < 8; i++)
            {
                models.add(new ResourceLocation(Reference.MOD_ID, "block/pipe_corner_notch_" + i));
            }

            return models;
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
            IModel baseModel = null;
            IModel sideModel = null;
            IModel[] cornerModels = new IModel[8];

            try
            {
                baseModel = ModelLoaderRegistry.getModel(BASE_MODEL);
                sideModel = ModelLoaderRegistry.getModel(SIDE_MODEL);
                List<ResourceLocation> models = this.getDependencies();

                for (int i = 0; i < cornerModels.length; i++)
                {
                    cornerModels[i] = ModelLoaderRegistry.getModel(models.get(i + 2));
                }
            }
            catch (Exception e)
            {
                Autoverse.logger.warn("Failed to load a model for the Pipe!", e);
            }

            return new ModelPipeBaked(this, baseModel, sideModel, cornerModels, state, format, bakedTextureGetter);
        }

        public ImmutableMap<String, String> getTextureMapping()
        {
            return ImmutableMap.copyOf(this.textures);
        }
    }

    private static class ModelPipeBasic extends ModelPipe
    {
        private ModelPipeBasic()
        {
            this.textures.put("particle",   TEX_BASIC);
            this.textures.put("texture",    TEX_BASIC);
        }
    }

    private static class ModelPipeExtraction extends ModelPipe
    {
        private ModelPipeExtraction()
        {
            this.textures.put("particle",   TEX_EXTRACTION);
            this.textures.put("texture",    TEX_EXTRACTION);
        }
    }

    private static class ModelPipeDirectional extends ModelPipe
    {
        private ModelPipeDirectional()
        {
            this.textures.put("particle",   TEX_DIRECTIONAL);
            this.textures.put("texture",    TEX_DIRECTIONAL);
        }
    }

    public static class ModelLoaderPipe implements ICustomModelLoader
    {
        private static final ResourceLocation FAKE_LOCATION_BASIC       = new ResourceLocation(Reference.MOD_ID, "models/block/custom/pipe_basic");
        private static final ResourceLocation FAKE_LOCATION_EXTRACTION  = new ResourceLocation(Reference.MOD_ID, "models/block/custom/pipe_extraction");
        private static final ResourceLocation FAKE_LOCATION_DIRECTIONAL = new ResourceLocation(Reference.MOD_ID, "models/block/custom/pipe_directional");

        @Override
        public boolean accepts(ResourceLocation modelLocation)
        {
            return modelLocation.equals(FAKE_LOCATION_BASIC) ||
                   modelLocation.equals(FAKE_LOCATION_EXTRACTION) ||
                   modelLocation.equals(FAKE_LOCATION_DIRECTIONAL);
        }

        @Override
        public IModel loadModel(ResourceLocation modelLocation) throws Exception
        {
            if (modelLocation.equals(FAKE_LOCATION_EXTRACTION))
            {
                return new ModelPipeExtraction();
            }
            else if (modelLocation.equals(FAKE_LOCATION_DIRECTIONAL))
            {
                return new ModelPipeDirectional();
            }
            else
            {
                return new ModelPipeBasic();
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
        private static final ModelResourceLocation LOCATION_BASIC       = new ModelResourceLocation(Reference.MOD_ID + ":pipe", "type=basic");
        private static final ModelResourceLocation LOCATION_EXTRACTION  = new ModelResourceLocation(Reference.MOD_ID + ":pipe", "type=extraction");
        private static final ModelResourceLocation LOCATION_DIRECTIONAL = new ModelResourceLocation(Reference.MOD_ID + ":pipe", "type=directional");

        @Override
        protected ModelResourceLocation getModelResourceLocation(IBlockState state)
        {
            switch (state.getValue(BlockPipe.TYPE))
            {
                case EXTRACTION:    return LOCATION_EXTRACTION;
                case DIRECTIONAL:   return LOCATION_DIRECTIONAL;
                default:            return LOCATION_BASIC;
            }
        }
    }
}
