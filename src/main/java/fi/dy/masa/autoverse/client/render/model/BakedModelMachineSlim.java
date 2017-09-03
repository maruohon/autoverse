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
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
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
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.block.base.BlockMachineSlimBase;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.PositionUtils;

public class BakedModelMachineSlim implements IBakedModel
{
    public static final ResourceLocation BASE_MODEL_12  = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_main_12");
    public static final ResourceLocation SIDE_MODEL_08  = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_side_08_tex_10");

    private static final Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> QUAD_CACHE = new HashMap<>();

    private final IModel baseModel;
    private final IModel outModel;
    private final IModel sideModel;
    private final ImmutableMap<String, String> textures;
    private final VertexFormat format;
    private final IBakedModel bakedBaseModel;
    private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
    private final TextureAtlasSprite particle;

    private BakedModelMachineSlim(
            IModel baseModel, IModel outModel, IModel sideModel,
            ImmutableMap<String, String> textures, IModelState modelState, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        this.baseModel = baseModel;
        this.outModel = outModel;
        this.sideModel = sideModel;
        this.textures = textures;

        this.format = format;
        this.bakedTextureGetter = bakedTextureGetter;
        this.bakedBaseModel = this.baseModel.bake(modelState, format, bakedTextureGetter);
        this.particle = this.bakedBaseModel.getParticleTexture();
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
        ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> quads = null;

        // Item model - this should never be used, as the slim model isn't used for any items
        if (state == null)
        {
            return ImmutableList.of();
        }
        else
        {
            quads = QUAD_CACHE.get(state);

            if (quads == null)
            {
                quads = this.bakeFullModel(state, state.getValue(BlockAutoverse.FACING));
                QUAD_CACHE.put(state, quads);
            }

            return quads.get(Optional.ofNullable(side));
        }
    }

    private ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> bakeFullModel(IBlockState state, EnumFacing mainFacing)
    {
        ImmutableMap.Builder<Optional<EnumFacing>, ImmutableList<BakedQuad>> builder = ImmutableMap.builder();
        IBakedModel bakedBaseModel = this.baseModel.bake(new TRSRTransformation(mainFacing), this.format, this.bakedTextureGetter);
        IBakedModel bakedOutModel = this.getOutputModel(state, mainFacing);
        List<IBakedModel> sideModels = this.getSideModels(state, mainFacing);

        for (EnumFacing face : ModelPipeBaked.MODEL_FACES)
        {
            ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();

            quads.addAll(bakedBaseModel.getQuads(state, face, 0));

            // This will be null for machines without output sides
            if (bakedOutModel != null)
            {
                quads.addAll(bakedOutModel.getQuads(state, face, 0));
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

    @Nullable
    private IBakedModel getOutputModel(IBlockState state, EnumFacing mainFacing)
    {
        BlockMachineSlimBase block = (BlockMachineSlimBase) state.getBlock();

        // Machine without an output side
        if (block.hasMainOutput() == false)
        {
            return null;
        }

        if (block.isMainOutputOppositeToFacing())
        {
            mainFacing = mainFacing.getOpposite();
        }

        return this.outModel.bake(new TRSRTransformation(mainFacing), this.format, this.bakedTextureGetter);
    }

    private List<IBakedModel> getSideModels(IBlockState state, EnumFacing mainFacing)
    {
        BlockMachineSlimBase block = (BlockMachineSlimBase) state.getBlock();
        List<IBakedModel> models = new ArrayList<IBakedModel>();

        for (int side = 0; side < block.getNumModelSideFacings(); side++)
        {
            EnumFacing relativeFacing = state.getValue(block.getPropertyFacing(side));
            EnumFacing absoluteFacing = PositionUtils.getAbsoluteFacingFromNorth(mainFacing, relativeFacing);
            IModel sideModel = this.sideModel.retexture(this.getSideModelTextures(state, side));
            models.add(sideModel.bake(new TRSRTransformation(absoluteFacing), this.format, this.bakedTextureGetter));
        }

        return models;
    }

    private ImmutableMap<String, String> getSideModelTextures(IBlockState state, int sideId)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        try
        {
            builder.put("slim_side_end", this.textures.get("slim_side_end_" + (sideId + 2))); // start numbering from 2, as "the second side"
            builder.put("slim_side_side", this.textures.get("slim_side_side_" + (sideId + 2)));
        }
        catch (Exception e)
        {
            Autoverse.logger.warn("Failed to get side model texture name for block state {}", state, e);
        }

        return builder.build();
    }

    private static class ModelMachine implements IModel
    {
        private final IModel baseModel;
        private final IModel outModel;
        private final IModel sideModel;
        private final ImmutableMap<String, String> textures;

        protected ModelMachine(IModel baseModel, IModel outModel, IModel sideModel, ImmutableMap<String, String> textures)
        {
            this.baseModel = baseModel;
            this.outModel = outModel;
            this.sideModel = sideModel;
            this.textures = textures;
        }

        @Override
        public IModelState getDefaultState()
        {
            return ModelRotation.X0_Y0;
        }

        @Override
        public List<ResourceLocation> getDependencies()
        {
            return ImmutableList.of(BASE_MODEL_12, SIDE_MODEL_08);
        }

        @Override
        public Collection<ResourceLocation> getTextures()
        {
            // retexture() runs after this, so we don't know them yet...
            // But luckily them being in the blockstate json seems to get the textures stitched...
            return ImmutableList.of();
        }

        @Override
        public IModel retexture(ImmutableMap<String, String> textures)
        {
            IModel baseModel = this.baseModel.retexture(textures);
            IModel outModel = this.outModel.retexture(textures);
            IModel sideModel = this.sideModel.retexture(textures);

            return new ModelMachine(baseModel, outModel, sideModel, textures);
        }

        @Override
        public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
        {
            return new BakedModelMachineSlim(this.baseModel, this.outModel, this.sideModel, this.textures, state, format, bakedTextureGetter);
        }
    }

    public static class ModelLoaderMachineSlim implements ICustomModelLoader
    {
        private static final ResourceLocation FAKE_LOCATION = new ResourceLocation(Reference.MOD_ID, "models/block/custom/machine_slim");

        @Override
        public boolean accepts(ResourceLocation modelLocation)
        {
            return modelLocation.equals(FAKE_LOCATION);
        }

        @Override
        public IModel loadModel(ResourceLocation modelLocation) throws Exception
        {
            IModel baseModel = ModelLoaderRegistry.getMissingModel();
            IModel outModel  = ModelLoaderRegistry.getMissingModel();
            IModel sideModel = ModelLoaderRegistry.getMissingModel();

            try
            {
                baseModel  = ModelLoaderRegistry.getModel(BASE_MODEL_12);
                outModel   = ModelLoaderRegistry.getModel(SIDE_MODEL_08);
                sideModel  = ModelLoaderRegistry.getModel(SIDE_MODEL_08);
            }
            catch (Exception e)
            {
                Autoverse.logger.warn("Failed to load a model for a machine!", e);
            }

            return new ModelMachine(baseModel, outModel, sideModel, ImmutableMap.of());
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager)
        {
            QUAD_CACHE.clear();
        }
    }
}
