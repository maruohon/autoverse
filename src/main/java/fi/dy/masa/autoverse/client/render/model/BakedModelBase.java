package fi.dy.masa.autoverse.client.render.model;

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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;

public abstract class BakedModelBase implements IBakedModel
{
    public static final EnumFacing[] MODEL_FACES = new EnumFacing[] {
            EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, null };

    protected final Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> quadCache;
    protected final IModel baseModel;
    protected final IBakedModel bakedBaseModel;
    protected final ImmutableMap<String, String> textures;
    protected final VertexFormat format;
    protected final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
    protected final TextureAtlasSprite particle;

    protected BakedModelBase(ResourceLocation baseModelLocation,
                             ResourceLocation particleTexture,
                             ImmutableMap<String, String> textures,
                             Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> quadCache,
                             IModelState modelState,
                             VertexFormat format,
                             Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        this.baseModel = getModelOrMissing(baseModelLocation);
        this.bakedBaseModel = this.baseModel.bake(modelState, format, bakedTextureGetter);
        this.textures = textures;
        this.quadCache = quadCache;
        this.format = format;
        this.bakedTextureGetter = bakedTextureGetter;
        this.particle = bakedTextureGetter.apply(particleTexture);
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
            state = this.getBaseStateForItemModel();
        }

        ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> quads = this.quadCache.get(state);

        if (quads == null)
        {
            quads = this.bakeFullModel(state);
            this.quadCache.put(state, quads);
        }

        return quads.get(Optional.ofNullable(side));
    }

    protected ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>> bakeFullModel(IBlockState state)
    {
        ImmutableMap.Builder<Optional<EnumFacing>, ImmutableList<BakedQuad>> builder = ImmutableMap.builder();
        List<IBakedModel> modelParts = this.getModelParts(state);

        for (EnumFacing face : MODEL_FACES)
        {
            ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();

            for (IBakedModel bakedPart : modelParts)
            {
                quads.addAll(bakedPart.getQuads(state, face, 0));
            }

            builder.put(Optional.ofNullable(face), quads.build());
        }

        return builder.build();
    }

    protected abstract List<IBakedModel> getModelParts(IBlockState state);

    protected abstract IBlockState getBaseStateForItemModel();

    public static IModel getModelOrMissing(ResourceLocation modelLocation)
    {
        try
        {
            return ModelLoaderRegistry.getModel(modelLocation);
        }
        catch (Exception e)
        {
            return ModelLoaderRegistry.getMissingModel();
        }
    }
}
