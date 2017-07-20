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
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.block.BlockPipe;
import fi.dy.masa.autoverse.block.BlockPipe.Connection;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.PositionUtils;

public class ModelPipeBaked implements IBakedModel
{
    private static final String TEX_BASIC_BASE          = Reference.MOD_ID + ":blocks/pipe_basic_base";

    private static final String TEX_EXTRACTION_BASE     = Reference.MOD_ID + ":blocks/pipe_extraction_base";
    private static final String TEX_EXTRACTION_CONN     = Reference.MOD_ID + ":blocks/pipe_extraction_connection";

    private static final String TEX_DIRECTIONAL_BASE    = Reference.MOD_ID + ":blocks/pipe_directional_base";
    private static final String TEX_DIRECTIONAL_CONN    = Reference.MOD_ID + ":blocks/pipe_directional_connection";

    private static final String TEX_ROUNDROBIN_BASE     = Reference.MOD_ID + ":blocks/pipe_roundrobin_base";
    private static final String TEX_ROUNDROBIN_CONN     = Reference.MOD_ID + ":blocks/pipe_roundrobin_connection";

    private static final String TEX_SIDE_WINDOW         = Reference.MOD_ID + ":blocks/pipe_side_window";

    private static final EnumFacing[] MODEL_FACES = new EnumFacing[] {
            EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, null };

    // These are the transforms to get the vertical pipe edge strip model to the correct orientation for each of the pipe edges
    // From NORTH to NORTH, SOUTH, WEST, EAST
    private static final TRSRTransformation[] TRANSFORMS_Y = new TRSRTransformation[] {
            new TRSRTransformation(ModelRotation.X0_Y0), new TRSRTransformation(ModelRotation.X0_Y180),
            new TRSRTransformation(ModelRotation.X0_Y270), new TRSRTransformation(ModelRotation.X0_Y90) };

    // From NORTH to DOWN, UP, NORTH, SOUTH
    private static final TRSRTransformation[] TRANSFORMS_X = new TRSRTransformation[] {
            new TRSRTransformation(ModelRotation.X90_Y90), new TRSRTransformation(ModelRotation.X270_Y270),
            new TRSRTransformation(ModelRotation.X90_Y270), new TRSRTransformation(ModelRotation.X270_Y90) };

    // From NORTH to DOWN, UP, WEST, EAST
    private static final TRSRTransformation[] TRANSFORMS_Z = new TRSRTransformation[] {
            new TRSRTransformation(ModelRotation.X90_Y180), new TRSRTransformation(ModelRotation.X270_Y0),
            new TRSRTransformation(ModelRotation.X270_Y180), new TRSRTransformation(ModelRotation.X90_Y0) };

    private static final Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> QUAD_CACHE = new HashMap<>();
    private static final Map <IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> QUAD_CACHE_ITEMS = new HashMap<>();
    private final IModel edgeModel;
    private final IModel sideModel;
    private final IModel connectionModelBase;
    private final IModel connectionModelFat;
    private final IModel connectionModelSlim;
    private final IModel[] cornerModels;
    private final IModelState modelState;
    private final VertexFormat format;
    private final IBakedModel bakedBaseModel;
    private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
    private final TextureAtlasSprite particle;
    private final BlockPipe.PipeType type;

    private ModelPipeBaked(
            BlockPipe.PipeType type,
            IModel edgeModel,
            IModel sideModel,
            IModel sideModelBase,
            IModel sideModelFat,
            IModel sideModelSlim,
            IModel[] cornerModels,
            ImmutableMap<String, String> textures,
            IModelState modelState,
            VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        this.type = type;
        this.edgeModel             = edgeModel      .retexture(ImmutableMap.of("edge", textures.get("base")));
        this.sideModel             = sideModel      .retexture(ImmutableMap.of("side", textures.get("side")));
        this.connectionModelBase   = sideModelBase  .retexture(ImmutableMap.of("edge", textures.get("base"), "side", textures.get("side")));
        this.connectionModelFat    = sideModelFat   .retexture(ImmutableMap.of("edge", textures.get("type"), "side", textures.get("side")));
        this.connectionModelSlim   = sideModelSlim  .retexture(ImmutableMap.of("edge", textures.get("type"), "side", textures.get("side")));
        this.cornerModels = new IModel[cornerModels.length];
        this.modelState = modelState;

        for (int i = 0; i < this.cornerModels.length; i++)
        {
            this.cornerModels[i] = cornerModels[i].retexture(ImmutableMap.of("texture", textures.get("base")));
        }

        this.format = format;
        this.bakedTextureGetter = bakedTextureGetter;
        this.bakedBaseModel = this.edgeModel.bake(modelState, format, bakedTextureGetter);
        this.particle = bakedTextureGetter.apply(new ResourceLocation(textures.get("base")));
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
        ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> quads;

        // Item model
        if (state == null)
        {
            state = AutoverseBlocks.PIPE.getDefaultState().withProperty(BlockPipe.TYPE, this.type);

            //quads = QUAD_CACHE_ITEMS.get(state);

            //if (quads == null)
            {
                TRSRTransformation trsca = new TRSRTransformation(null, null, new javax.vecmath.Vector3f(1.5f, 1.5f, 1.5f), null);
                IModelState modelState = new ModelStateComposition(this.modelState, TRSRTransformation.blockCenterToCorner(trsca));

                quads = this.bakeFullModel(state, side, modelState);
                //QUAD_CACHE_ITEMS.put(state, quads);
            }

            return quads.get(Optional.ofNullable(side));
        }
        else
        {
            //quads = QUAD_CACHE.get(state);

            //if (quads == null)
            {
                TRSRTransformation modelState = new TRSRTransformation(ModelRotation.X0_Y0);

                quads = this.bakeFullModel(state, side, modelState);
                //QUAD_CACHE.put(state, quads);
            }

            return quads.get(Optional.ofNullable(side));
        }
    }

    private ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> bakeFullModel(
            IBlockState state, @Nullable EnumFacing side, @Nullable IModelState modelState)
    {
        ImmutableMap.Builder<Optional<EnumFacing>, ImmutableList<BakedQuad>> builder = ImmutableMap.builder();
        List<IBakedModel> connectionModels = this.getConnectionModels(state, modelState);
        List<IBakedModel> middleModels = this.getMainModelPieces(state, modelState);

        for (EnumFacing face : MODEL_FACES)
        {
            ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();

            for (IBakedModel bakedPart : middleModels)
            {
                quads.addAll(bakedPart.getQuads(state, face, 0));
            }

            for (IBakedModel bakedPart : connectionModels)
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

    private List<IBakedModel> getMainModelPieces(IBlockState state, @Nullable IModelState modelStateIn)
    {
        List<IBakedModel> models = new ArrayList<IBakedModel>();

        this.addEdges(state, PositionUtils.SIDES_Y, EnumFacing.UP, models, modelStateIn);
        this.addEdges(state, PositionUtils.SIDES_X, EnumFacing.EAST, models, modelStateIn);
        this.addEdges(state, PositionUtils.SIDES_Z, EnumFacing.SOUTH, models, modelStateIn);

        this.addSides(state, PositionUtils.SIDES_Y, EnumFacing.UP, models, modelStateIn);
        this.addSides(state, PositionUtils.SIDES_X, EnumFacing.EAST, models, modelStateIn);

        this.addCorners(state, models, EnumFacing.UP, modelStateIn);

        TRSRTransformation tr = new TRSRTransformation(ModelRotation.X180_Y180);
        IModelState modelState = modelStateIn != null ? new ModelStateComposition(tr, modelStateIn) : tr;
        this.addCorners(state, models, EnumFacing.DOWN, modelState);

        return models;
    }

    /**
     * Transforms a single vertical pipe model edge (strip) to all four sides around the given <b>rotationAxis</b>.
     * This method is then called for each of the three axes separately.
     * @param state
     * @param sides
     * @param rotationAxis
     * @param models
     */
    private void addEdges(IBlockState state, EnumFacing[] sides, EnumFacing rotationAxis,
            List<IBakedModel> models, @Nullable IModelState modelStateIn)
    {
        //for (EnumFacing side : sides)
        for (int i = 0; i < 4; i++)
        {
            //EnumFacing other = PositionUtils.rotateAround(side, rotationAxis);
            //boolean conn1 = state.getValue(BlockPipe.CONNECTIONS.get(side.getIndex())) != Connection.NONE;
            //boolean conn2 = state.getValue(BlockPipe.CONNECTIONS.get(other.getIndex())) != Connection.NONE;

            TRSRTransformation tr;
            if (sides == PositionUtils.SIDES_Y)         { tr = TRANSFORMS_Y[i]; }
            else if (sides == PositionUtils.SIDES_X)    { tr = TRANSFORMS_X[i]; }
            else                                        { tr = TRANSFORMS_Z[i]; }

            IModelState modelState = modelStateIn != null ? new ModelStateComposition(tr, modelStateIn) : tr;

            /*
            boolean conn3 = state.getValue(BlockPipe.CONNECTIONS.get(rotationAxis.getIndex())) != Connection.NONE;
            boolean conn4 = state.getValue(BlockPipe.CONNECTIONS.get(rotationAxis.getOpposite().getIndex())) != Connection.NONE;
            // When there is no connection on either of the sides around an edge,
            // or when there is a connection on both sides,
            // or in a three-way intersection along the rotation axis,
            // then a connecting model strip is added to that edge.
            if ((conn1 == conn2) || (conn1 && conn3 && conn4))
            */

            // When there is no connection on either of the sides around an edge,
            // or when there is a connection on both sides,
            // then a connecting model strip is added to that edge.
            //if (conn1 == conn2)

            // No connection on either side
            //if (conn1 == false && conn2 == false)
            {
                models.add(this.edgeModel.bake(modelState, this.format, this.bakedTextureGetter));
            }
        }
    }

    /**
     * Transforms a single side model piece ("window") to all four sides around the given <b>rotationAxis</b>.
     * This method is then called for each of the three axes separately.
     * @param state
     * @param sides
     * @param rotationAxis
     * @param models
     */
    private void addSides(IBlockState state, EnumFacing[] sides, EnumFacing rotationAxis,
            List<IBakedModel> models, @Nullable IModelState modelStateIn)
    {
        int i = 0;

        for (EnumFacing side : sides)
        {
            // No connection on this side, add the side "window" model
            if (state.getValue(BlockPipe.CONNECTIONS.get(side.getIndex())) == Connection.NONE)
            {
                TRSRTransformation tr;
                if (sides == PositionUtils.SIDES_Y)         { tr = TRANSFORMS_Y[i]; }
                else if (sides == PositionUtils.SIDES_X)    { tr = TRANSFORMS_X[i]; }
                else                                        { tr = TRANSFORMS_Z[i]; }

                IModelState modelState = modelStateIn != null ? new ModelStateComposition(tr, modelStateIn) : tr;

                models.add(this.sideModel.bake(modelState, this.format, this.bakedTextureGetter));
            }

            i++;
        }
    }

    /**
     * Select the correct pipe corner model based on which of the three adjacent sides have pipe connections on them.
     * The selected model is then transformed for all four corners on that "plane" (either the top of the pipe, or the bottom).
     * @param state
     * @param models
     * @param upAxis
     * @param modelState
     */
    private void addCorners(IBlockState state, List<IBakedModel> models, EnumFacing upAxis, @Nullable IModelState modelStateIn)
    {
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

            // No visible faces if there is a connection on every side
            if (index != 7)
            {
                IModelState modelState = modelStateIn != null ? new ModelStateComposition(TRANSFORMS_Y[i], modelStateIn) : TRANSFORMS_Y[i];

                models.add(this.cornerModels[index].bake(modelState, this.format, this.bakedTextureGetter));
            }

            i++;
        }
    }

    private List<IBakedModel> getConnectionModels(IBlockState state, @Nullable IModelState modelStateIn)
    {
        List<IBakedModel> models = new ArrayList<IBakedModel>();

        for (EnumFacing side : EnumFacing.values())
        {
            BlockPipe.Connection conn = state.getValue(BlockPipe.CONNECTIONS.get(side.getIndex()));
            TRSRTransformation tr = new TRSRTransformation(side);
            IModelState modelState = modelStateIn != null ? new ModelStateComposition(tr, modelStateIn) : tr;

            switch (conn)
            {
                case BASIC:
                    models.add(this.connectionModelBase.bake(modelState, this.format, this.bakedTextureGetter));
                    break;

                case TYPE:
                    if (state.getValue(BlockPipe.TYPE) == BlockPipe.PipeType.EXTRACTION)
                    {
                        models.add(this.connectionModelFat.bake(modelState, this.format, this.bakedTextureGetter));
                    }
                    else
                    {
                        models.add(this.connectionModelSlim.bake(modelState, this.format, this.bakedTextureGetter));
                    }
                    break;

                default:
            }
        }

        return models;
    }

    private static abstract class ModelPipe implements IModel
    {
        private static final ResourceLocation EDGE_MODEL            = new ResourceLocation(Reference.MOD_ID, "block/pipe_edge_y");
        private static final ResourceLocation SIDE_MODEL            = new ResourceLocation(Reference.MOD_ID, "block/pipe_side_window");
        private static final ResourceLocation CONNECTION_MODEL_BASE = new ResourceLocation(Reference.MOD_ID, "block/pipe_connection_normal");
        private static final ResourceLocation CONNECTION_MODEL_FAT  = new ResourceLocation(Reference.MOD_ID, "block/pipe_connection_fat");
        private static final ResourceLocation CONNECTION_MODEL_SLIM = new ResourceLocation(Reference.MOD_ID, "block/pipe_connection_slim");

        protected final BlockPipe.PipeType type;
        protected final Map<String, String> textures = new HashMap<String, String>();

        protected ModelPipe(BlockPipe.PipeType type)
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
            List<ResourceLocation> models = Lists.newArrayList(EDGE_MODEL, SIDE_MODEL,
                    CONNECTION_MODEL_BASE, CONNECTION_MODEL_FAT, CONNECTION_MODEL_SLIM);

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
            IModel edgeModel = null;
            IModel sideModel = null;
            IModel connectionModelBase = null;
            IModel connectionModelFat = null;
            IModel connectionModelSlim = null;
            IModel[] cornerModels = new IModel[8];

            try
            {
                edgeModel           = ModelLoaderRegistry.getModel(EDGE_MODEL);
                sideModel           = ModelLoaderRegistry.getModel(SIDE_MODEL);
                connectionModelBase = ModelLoaderRegistry.getModel(CONNECTION_MODEL_BASE);
                connectionModelFat  = ModelLoaderRegistry.getModel(CONNECTION_MODEL_FAT);
                connectionModelSlim = ModelLoaderRegistry.getModel(CONNECTION_MODEL_SLIM);
                List<ResourceLocation> models = this.getDependencies();

                for (int i = 0; i < cornerModels.length; i++)
                {
                    cornerModels[i] = ModelLoaderRegistry.getModel(models.get(i + (models.size() - 8)));
                }
            }
            catch (Exception e)
            {
                Autoverse.logger.warn("Failed to load a model for the Pipe!", e);
            }

            return new ModelPipeBaked(this.type, edgeModel, sideModel, connectionModelBase, connectionModelFat,
                    connectionModelSlim, cornerModels, this.getTextureMapping(), state, format, bakedTextureGetter);
        }

        protected ImmutableMap<String, String> getTextureMapping()
        {
            return ImmutableMap.copyOf(this.textures);
        }
    }

    private static class ModelPipeBasic extends ModelPipe
    {
        private ModelPipeBasic()
        {
            super(BlockPipe.PipeType.BASIC);

            this.textures.put("base", TEX_BASIC_BASE);
            this.textures.put("type", TEX_BASIC_BASE); // Dummy, not used, but required
            this.textures.put("side", TEX_SIDE_WINDOW);
        }
    }

    private static class ModelPipeExtraction extends ModelPipe
    {
        private ModelPipeExtraction()
        {
            super(BlockPipe.PipeType.EXTRACTION);

            this.textures.put("base", TEX_EXTRACTION_BASE);
            this.textures.put("type", TEX_EXTRACTION_CONN);
            this.textures.put("side", TEX_SIDE_WINDOW);
        }
    }

    private static class ModelPipeDirectional extends ModelPipe
    {
        private ModelPipeDirectional()
        {
            super(BlockPipe.PipeType.DIRECTIONAL);

            this.textures.put("base", TEX_DIRECTIONAL_BASE);
            this.textures.put("type", TEX_DIRECTIONAL_CONN);
            this.textures.put("side", TEX_SIDE_WINDOW);
        }
    }

    private static class ModelPipeRoundRobin extends ModelPipe
    {
        private ModelPipeRoundRobin()
        {
            super(BlockPipe.PipeType.ROUNDROBIN);

            this.textures.put("base", TEX_ROUNDROBIN_BASE);
            this.textures.put("type", TEX_ROUNDROBIN_CONN);
            this.textures.put("side", TEX_SIDE_WINDOW);
        }
    }

    public static class ModelLoaderPipe implements ICustomModelLoader
    {
        private static final ResourceLocation FAKE_LOCATION_BASIC       = new ResourceLocation(Reference.MOD_ID, "models/block/custom/pipe_basic");
        private static final ResourceLocation FAKE_LOCATION_EXTRACTION  = new ResourceLocation(Reference.MOD_ID, "models/block/custom/pipe_extraction");
        private static final ResourceLocation FAKE_LOCATION_DIRECTIONAL = new ResourceLocation(Reference.MOD_ID, "models/block/custom/pipe_directional");
        private static final ResourceLocation FAKE_LOCATION_ROUNDROBIN  = new ResourceLocation(Reference.MOD_ID, "models/block/custom/pipe_roundrobin");

        @Override
        public boolean accepts(ResourceLocation modelLocation)
        {
            return modelLocation.equals(FAKE_LOCATION_BASIC) ||
                   modelLocation.equals(FAKE_LOCATION_EXTRACTION) ||
                   modelLocation.equals(FAKE_LOCATION_DIRECTIONAL) ||
                   modelLocation.equals(FAKE_LOCATION_ROUNDROBIN);
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
            else if (modelLocation.equals(FAKE_LOCATION_ROUNDROBIN))
            {
                return new ModelPipeRoundRobin();
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
            QUAD_CACHE_ITEMS.clear();
        }
    }

    public static class StateMapper extends StateMapperBase
    {
        private static final ModelResourceLocation LOCATION_BASIC       = new ModelResourceLocation(Reference.MOD_ID + ":pipe", "type=basic");
        private static final ModelResourceLocation LOCATION_EXTRACTION  = new ModelResourceLocation(Reference.MOD_ID + ":pipe", "type=extraction");
        private static final ModelResourceLocation LOCATION_DIRECTIONAL = new ModelResourceLocation(Reference.MOD_ID + ":pipe", "type=directional");
        private static final ModelResourceLocation LOCATION_ROUNDROBIN  = new ModelResourceLocation(Reference.MOD_ID + ":pipe", "type=roundrobin");

        @Override
        protected ModelResourceLocation getModelResourceLocation(IBlockState state)
        {
            switch (state.getValue(BlockPipe.TYPE))
            {
                case EXTRACTION:    return LOCATION_EXTRACTION;
                case DIRECTIONAL:   return LOCATION_DIRECTIONAL;
                case ROUNDROBIN:    return LOCATION_ROUNDROBIN;
                default:            return LOCATION_BASIC;
            }
        }
    }
}
