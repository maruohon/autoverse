package fi.dy.masa.autoverse.client.render.model;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;

public class ModelLoaderBase implements ICustomModelLoader
{
    protected final ImmutableMap<ResourceLocation, IModelFactory> modelFactories;
    protected final IQuadCacheClearer quadCacheClearer;

    public ModelLoaderBase(ImmutableMap<ResourceLocation, IModelFactory> modelFactories, IQuadCacheClearer quadCacheClearer)
    {
        this.modelFactories = modelFactories;
        this.quadCacheClearer = quadCacheClearer;
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation)
    {
        return this.modelFactories.containsKey(modelLocation);
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation)
    {
        IModelFactory factory = this.modelFactories.get(modelLocation);

        if (factory == null)
        {
            throw new IllegalArgumentException("No model factory present for model location '" + modelLocation + "'");
        }

        return factory.createModel(modelLocation);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager)
    {
        this.quadCacheClearer.clearCache();
    }

    public interface IModelFactory
    {
        IModel createModel(ResourceLocation modelLocation);
    }

    public interface IQuadCacheClearer
    {
        void clearCache();
    }
}
