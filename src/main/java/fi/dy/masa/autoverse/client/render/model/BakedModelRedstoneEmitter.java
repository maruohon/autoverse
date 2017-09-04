package fi.dy.masa.autoverse.client.render.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
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
import fi.dy.masa.autoverse.block.BlockRedstoneEmitter.EmitterType;
import fi.dy.masa.autoverse.block.BlockRedstoneEmitter.SideStatus;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.reference.Reference;

public class BakedModelRedstoneEmitter extends BakedModelMachineSlim
{
    protected static final ResourceLocation FULL_BLOCK_MODEL  = new ResourceLocation(Reference.MOD_ID, "block/cube_individual");
    protected static final ResourceLocation BASE_MODEL_10     = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_main_10_tex_16");
    protected static final ResourceLocation SIDE_MODEL_06x03  = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_side_06x03_tex_06x03");
    protected static final ResourceLocation SIDE_MODEL_08x03  = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_side_08x03_tex_10x03");
    private static final Map<IBlockState, IBakedModel> MODEL_CACHE = new HashMap<>();

    protected final IModel fullBlockModel;
    private final BlockRedstoneEmitter.EmitterType type;
    protected final TextureAtlasSprite particle;

    private BakedModelRedstoneEmitter(
            BlockRedstoneEmitter.EmitterType type,
            IModel fullBlock, IModel baseModel, IModel outModel, IModel sideModel,
            ImmutableMap<String, String> textures, IModelState modelState, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        super(baseModel, outModel, sideModel, textures, modelState, format, bakedTextureGetter);

        this.type = type;
        this.fullBlockModel = fullBlock;
        String typeName = type == BlockRedstoneEmitter.EmitterType.ADVANCED ? "advanced" : "basic";
        this.particle = bakedTextureGetter.apply(new ResourceLocation("autoverse:blocks/redstone_emitter_" + typeName + "_front"));
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
        }

        if (state.getValue(BlockRedstoneEmitter.SLIM))
        {
            return super.getQuads(state, side, rand);
        }
        else
        {
            IBakedModel bakedModel = null;
            //bakedModel = MODEL_CACHE.get(state);

            if (bakedModel == null)
            {
                IModel model = this.fullBlockModel.retexture(this.getFullBlockModelTextures(state));
                bakedModel = model.bake(new TRSRTransformation(ModelRotation.X0_Y0), this.format, this.bakedTextureGetter);

                MODEL_CACHE.put(state, bakedModel);
            }

            return bakedModel.getQuads(state, side, rand);
        }
    }

    @Override
    protected IBakedModel getBaseModel(IBlockState state, EnumFacing mainFacing)
    {
        IModel baseModel = this.baseModel.retexture(this.getFullBlockModelTextures(state));
        return baseModel.bake(new TRSRTransformation(ModelRotation.X0_Y0), this.format, this.bakedTextureGetter);
    }

    @Override
    protected List<IBakedModel> getSideModels(IBlockState state, EnumFacing mainFacing)
    {
        List<IBakedModel> models = new ArrayList<IBakedModel>();
        boolean advanced = this.type == EmitterType.ADVANCED;

        for (EnumFacing side : EnumFacing.VALUES)
        {
            if (side != mainFacing && (advanced || state.getValue(BlockRedstoneEmitter.SIDES.get(side.getIndex())) != SideStatus.DISABLED))
            {
                boolean powered = state.getValue(BlockRedstoneEmitter.SIDES.get(side.getIndex())) == BlockRedstoneEmitter.SideStatus.POWERED;
                IModel sideModel = this.sideModel.retexture(this.getSideModelTextures(state, powered));
                models.add(sideModel.bake(new TRSRTransformation(side), this.format, this.bakedTextureGetter));
            }
        }

        return models;
    }

    private ImmutableMap<String, String> getSideModelTextures(IBlockState state, boolean powered)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        try
        {
            builder.put("slim_side_end", this.textures.get("rs_" + (powered ? "on" : "off") + "_slim_end"));
            builder.put("slim_side_side", this.textures.get("rs_slim_side"));
        }
        catch (Exception e)
        {
            Autoverse.logger.warn("Failed to get side model texture name for block state {}", state, e);
        }

        return builder.build();
    }

    private ImmutableMap<String, String> getFullBlockModelTextures(IBlockState state)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        EnumFacing front = state.getValue(BlockAutoverse.FACING);
        boolean slim = state.getValue(BlockRedstoneEmitter.SLIM);

        try
        {
            for (EnumFacing side : EnumFacing.values())
            {
                String key = slim ? "slim_" + side.toString().toLowerCase() : side.toString().toLowerCase();

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
        }
        catch (Exception e)
        {
            Autoverse.logger.warn("Failed to get side model texture name for block state {}", state, e);
        }

        return builder.build();
    }

    private static class ModelRedstoneEmitter extends ModelMachineSlim
    {
        protected final IModel fullModel;
        private final BlockRedstoneEmitter.EmitterType type;
        private static ImmutableList<ResourceLocation> texture_deps = ImmutableList.of();

        static
        {
            ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_basic_front"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_basic_side_disabled"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_basic_side_unpowered"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_basic_side_powered"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_basic_out_slim_end_10"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_basic_out_slim_side_10"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_basic_rs_unpowered_slim_end_06"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_basic_rs_powered_slim_end_06"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_basic_rs_slim_side_06"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_front"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_side_unpowered"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_side_powered"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_out_slim_end_10"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_out_slim_side_10"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_rs_unpowered_slim_end_06"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_rs_powered_slim_end_06"));
            builder.add(new ResourceLocation("autoverse:blocks/redstone_emitter_advanced_rs_slim_side_06"));
            texture_deps = builder.build();
        }

        protected ModelRedstoneEmitter(BlockRedstoneEmitter.EmitterType type,
                IModel fullBlock, IModel baseModel, IModel outModel, IModel sideModel, ImmutableMap<String, String> textures)
        {
            super(baseModel, outModel, sideModel, textures);

            this.type = type;
            this.fullModel = fullBlock;
        }

        @Override
        public List<ResourceLocation> getDependencies()
        {
            return Lists.newArrayList(FULL_BLOCK_MODEL, BASE_MODEL_10, SIDE_MODEL_08x03, SIDE_MODEL_06x03);
        }

        @Override
        public Collection<ResourceLocation> getTextures()
        {
            return texture_deps;
        }

        @Override
        public IModel retexture(ImmutableMap<String, String> textures)
        {
            IModel outModel = this.outModel.retexture(textures);
            // The full block model, the slim base model and the slim side models will be re-textured later, based on the IBlockState

            return new ModelRedstoneEmitter(this.type, this.fullModel, this.baseModel, outModel, this.sideModel, textures);
        }

        @Override
        public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
        {
            return new BakedModelRedstoneEmitter(this.type, this.fullModel, this.baseModel, this.outModel, this.sideModel,
                    this.textures, state, format, bakedTextureGetter);
        }
    }

    public static class ModelLoaderRedstoneEmitter implements ICustomModelLoader
    {
        private static final ResourceLocation FAKE_LOCATION_BASIC    = new ResourceLocation(Reference.MOD_ID, "models/block/custom/redstone_emitter_basic");
        private static final ResourceLocation FAKE_LOCATION_ADVANCED = new ResourceLocation(Reference.MOD_ID, "models/block/custom/redstone_emitter_advanced");

        @Override
        public boolean accepts(ResourceLocation modelLocation)
        {
            return modelLocation.equals(FAKE_LOCATION_BASIC) || modelLocation.equals(FAKE_LOCATION_ADVANCED);
        }

        @Override
        public IModel loadModel(ResourceLocation modelLocation) throws Exception
        {
            IModel fullModel = ModelLoaderRegistry.getMissingModel();
            IModel baseModel = ModelLoaderRegistry.getMissingModel();
            IModel outModel  = ModelLoaderRegistry.getMissingModel();
            IModel sideModel = ModelLoaderRegistry.getMissingModel();

            try
            {
                fullModel  = ModelLoaderRegistry.getModel(FULL_BLOCK_MODEL);
                baseModel  = ModelLoaderRegistry.getModel(BASE_MODEL_10);
                outModel   = ModelLoaderRegistry.getModel(SIDE_MODEL_08x03);
                sideModel  = ModelLoaderRegistry.getModel(SIDE_MODEL_06x03);
            }
            catch (Exception e)
            {
                Autoverse.logger.warn("Failed to load a model for a Redstone Emitter", e);
            }

            if (modelLocation.equals(FAKE_LOCATION_ADVANCED))
            {
                return new ModelRedstoneEmitter(BlockRedstoneEmitter.EmitterType.ADVANCED, fullModel, baseModel, outModel, sideModel, ImmutableMap.of());
            }
            else
            {
                return new ModelRedstoneEmitter(BlockRedstoneEmitter.EmitterType.BASIC, fullModel, baseModel, outModel, sideModel, ImmutableMap.of());
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
        private static final ModelResourceLocation LOCATION_BASIC    = new ModelResourceLocation(Reference.MOD_ID + ":redstone_emitter", "type=basic");
        private static final ModelResourceLocation LOCATION_ADVANCED = new ModelResourceLocation(Reference.MOD_ID + ":redstone_emitter", "type=advanced");

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
