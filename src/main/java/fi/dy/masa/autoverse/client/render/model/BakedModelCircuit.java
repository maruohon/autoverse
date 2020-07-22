package fi.dy.masa.autoverse.client.render.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
import fi.dy.masa.autoverse.block.BlockCircuit;
import fi.dy.masa.autoverse.block.BlockCircuit.CircuitType;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.reference.Reference;

public class BakedModelCircuit extends BakedModelBase
{
    private static final ResourceLocation FAKE_LOCATION_LATCH_RS = new ResourceLocation(Reference.MOD_ID, "models/block/custom/latch_rs");
    private static final ResourceLocation FAKE_LOCATION_LATCH_T  = new ResourceLocation(Reference.MOD_ID, "models/block/custom/latch_t");

    private static final ResourceLocation LATCH_BASE_MODEL = new ResourceLocation(Reference.MOD_ID, "block/latch_base");
    private static final ResourceLocation LATCH_SIDE_MODEL = new ResourceLocation(Reference.MOD_ID, "block/latch_side");

    private static final Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> QUAD_CACHE = new HashMap<>();

    private final BlockCircuit.CircuitType type;
    private final IModel sideModel;

    private BakedModelCircuit(ResourceLocation modelLocation,
                              ImmutableMap<String, String> textures,
                              IModelState modelState,
                              VertexFormat format,
                              Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        super(LATCH_BASE_MODEL, textures, QUAD_CACHE, modelState, format, bakedTextureGetter);

        this.type = modelLocation.equals(FAKE_LOCATION_LATCH_RS) ? CircuitType.LATCH_RS : CircuitType.LATCH_T;
        this.sideModel = getModelOrMissing(LATCH_SIDE_MODEL);
    }

    @Override
    protected IBlockState getBaseStateForItemModel()
    {
        return AutoverseBlocks.CIRCUIT.getDefaultState().withProperty(BlockCircuit.TYPE, this.type);
    }

    @Override
    protected List<IBakedModel> getModelParts(IBlockState state)
    {
        List<IBakedModel> models = new ArrayList<>();
        ImmutableMap<String, String> baseTextures = this.getBaseModelTextures(state.getValue(BlockCircuit.POWERED));

        models.add(this.baseModel.retexture(baseTextures).bake(TRSRTransformation.from(ModelRotation.X0_Y0), this.format, this.bakedTextureGetter));
        models.add(this.sideModel.retexture(baseTextures).bake(TRSRTransformation.from(state.getValue(BlockCircuit.FACING)), this.format, this.bakedTextureGetter));

        if (state.getValue(BlockCircuit.TYPE) == CircuitType.LATCH_RS)
        {
            IModel model = this.sideModel.retexture(this.getSideModelTexturesLatchRS(false));
            models.add(model.bake(TRSRTransformation.from(state.getValue(BlockCircuit.FACING2)), this.format, this.bakedTextureGetter));

            model = this.sideModel.retexture(this.getSideModelTexturesLatchRS(true));
            models.add(model.bake(TRSRTransformation.from(state.getValue(BlockCircuit.FACING3)), this.format, this.bakedTextureGetter));
        }

        return models;
    }

    private ImmutableMap<String, String> getBaseModelTextures(boolean powered)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put("latch_base",       this.textures.get("base"));
        builder.put("latch_side_side",  this.textures.get("out_" + (powered ? "on" : "off") + "_side"));
        builder.put("latch_side_end",   this.textures.get("out_" + (powered ? "on" : "off") + "_end"));

        return builder.build();
    }

    private ImmutableMap<String, String> getSideModelTexturesLatchRS(boolean isReset)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put("latch_side_side",  this.textures.get("in_" + (isReset ? "reset" : "set") + "_side"));
        builder.put("latch_side_end",   this.textures.get("in_" + (isReset ? "reset" : "set") + "_end"));

        return builder.build();
    }

    private static void clearQuadCache()
    {
        QUAD_CACHE.clear();
    }

    private static ImmutableMap<ResourceLocation, ModelLoaderBase.IModelFactory> getModelFactories()
    {
        ImmutableMap.Builder<ResourceLocation, ModelLoaderBase.IModelFactory> builder = ImmutableMap.builder();

        final ImmutableList<ResourceLocation> modelDeps = ImmutableList.of(LATCH_BASE_MODEL, LATCH_SIDE_MODEL);
        final ImmutableList<ResourceLocation> textureDeps = ImmutableList.of(
                new ResourceLocation("autoverse:blocks/latch_t_base_side"),
                new ResourceLocation("autoverse:blocks/latch_rs_base_side"),
                new ResourceLocation("autoverse:blocks/latch_rs_out_off_end"),
                new ResourceLocation("autoverse:blocks/latch_rs_out_off_side"),
                new ResourceLocation("autoverse:blocks/latch_rs_out_on_end"),
                new ResourceLocation("autoverse:blocks/latch_rs_out_on_side"),
                new ResourceLocation("autoverse:blocks/latch_rs_in_reset_end"),
                new ResourceLocation("autoverse:blocks/latch_rs_in_reset_side"),
                new ResourceLocation("autoverse:blocks/latch_rs_in_set_end"),
                new ResourceLocation("autoverse:blocks/latch_rs_in_set_side")
        );

        builder.put(FAKE_LOCATION_LATCH_RS, (ml) -> new ModelBase(ml, BakedModelCircuit::new, modelDeps, textureDeps, ImmutableMap.of()));
        builder.put(FAKE_LOCATION_LATCH_T,  (ml) -> new ModelBase(ml, BakedModelCircuit::new, modelDeps, textureDeps, ImmutableMap.of()));

        return builder.build();
    }

    public static ICustomModelLoader getModelLoader()
    {
        return new ModelLoaderBase(getModelFactories(), BakedModelCircuit::clearQuadCache);
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
                case LATCH_T:  return LOCATION_LATCH_T;
                case LATCH_RS:
                default:       return LOCATION_LATCH_RS;
            }
        }
    }
}
