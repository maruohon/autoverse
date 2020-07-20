package fi.dy.masa.autoverse.client.render.model;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;

public class ModelBase implements IModel
{
    protected final ResourceLocation modelLocation;
    protected final IBakedModelFactory modelFactory;
    protected final ImmutableList<ResourceLocation> modelDependencies;
    protected final ImmutableList<ResourceLocation> textureDependencies;
    protected final ImmutableMap<String, String> textures;

    public ModelBase(ResourceLocation modelLocation,
                     IBakedModelFactory modelFactory,
                     ImmutableList<ResourceLocation> modelDependencies,
                     ImmutableList<ResourceLocation> textureDependencies,
                     ImmutableMap<String, String> textures)
    {
        this.modelLocation = modelLocation;
        this.modelFactory = modelFactory;
        this.modelDependencies = modelDependencies;
        this.textureDependencies = textureDependencies;
        this.textures = textures;
    }

    @Override
    public List<ResourceLocation> getDependencies()
    {
        return this.modelDependencies;
    }

    @Override
    public Collection<ResourceLocation> getTextures()
    {
        return this.textureDependencies;
    }

    @Override
    public IModel retexture(ImmutableMap<String, String> textures)
    {
        return new ModelBase(this.modelLocation, this.modelFactory, this.modelDependencies, this.textureDependencies, textures);
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        return this.modelFactory.createBakedModel(this.modelLocation, this.textures, state, format, bakedTextureGetter);
    }

    public interface IBakedModelFactory
    {
        IBakedModel createBakedModel(ResourceLocation modelLocation,
                                     ImmutableMap<String, String> textures,
                                     IModelState state,
                                     VertexFormat format,
                                     Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter);
    }
}
