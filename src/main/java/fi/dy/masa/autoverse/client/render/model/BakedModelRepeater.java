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
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import fi.dy.masa.autoverse.block.BlockInventoryReader;
import fi.dy.masa.autoverse.block.BlockRepeater;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.reference.Reference;

public class BakedModelRepeater extends BakedModelBase
{
    public static final ResourceLocation FAKE_LOCATION_REPEATER = new ResourceLocation(Reference.MOD_ID, "models/block/custom/repeater");

    public static final ResourceLocation REPEATER_BASE_MODEL   = new ResourceLocation(Reference.MOD_ID, "block/repeater_base");
    public static final ResourceLocation REPEATER_OUTPUT_MODEL = new ResourceLocation(Reference.MOD_ID, "block/repeater_output");

    private static final Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> QUAD_CACHE = new HashMap<>();

    private final IModel outputModel;

    private BakedModelRepeater(ResourceLocation modelLocation,
                                      ImmutableMap<String, String> textures,
                                      IModelState modelState,
                                      VertexFormat format,
                                      Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        super(REPEATER_BASE_MODEL, textures, QUAD_CACHE, modelState, format, bakedTextureGetter);

        this.outputModel = getModelOrMissing(REPEATER_OUTPUT_MODEL);
    }

    @Override
    protected IBlockState getBaseStateForItemModel()
    {
        return AutoverseBlocks.REPEATER.getDefaultState();
    }

    @Override
    protected List<IBakedModel> getModelParts(IBlockState state)
    {
        List<IBakedModel> models = new ArrayList<>();
        BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();

        if (layer == BlockRenderLayer.CUTOUT || layer == null)
        {
            boolean powered = state.getValue(BlockInventoryReader.POWERED);
            ImmutableMap<String, String> baseTextures = this.getBaseModelTextures(powered, state.getValue(BlockRepeater.DELAY));

            models.add(this.baseModel  .retexture(baseTextures).bake(TRSRTransformation.from(state.getValue(BlockInventoryReader.FACING)), this.format, this.bakedTextureGetter));
            // The model is made as a "south-to-north" straight model. But since the output facing in that case
            // will be south and not north like the usual default facing, we need to flip it here.
            models.add(this.outputModel.retexture(baseTextures).bake(TRSRTransformation.from(state.getValue(BlockInventoryReader.FACING_OUT).getOpposite()), this.format, this.bakedTextureGetter));
        }

        return models;
    }

    private ImmutableMap<String, String> getBaseModelTextures(boolean powered, int delay)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put("base_front", this.textures.get("base_front"));
        builder.put("base_back",  this.textures.get("base_back"));
        builder.put("base_side",  this.textures.get("base_side"));
        builder.put("rod_side",   this.textures.get("rod_side_" + (powered ? "powered" : "unpowered")));
        builder.put("rod_end",    this.textures.get("rod_end_" + (powered ? "powered" : "unpowered")));
        builder.put("rod_bulge",  this.textures.get("rod_bulge_base"));
        builder.put("delay",      this.textures.get("delay_" + String.format("%02d", delay)));

        return builder.build();
    }

    private static void clearQuadCache()
    {
        QUAD_CACHE.clear();
    }

    private static ImmutableMap<ResourceLocation, ModelLoaderBase.IModelFactory> getModelFactories()
    {
        ImmutableMap.Builder<ResourceLocation, ModelLoaderBase.IModelFactory> builder = ImmutableMap.builder();

        final ImmutableList<ResourceLocation> modelDeps = ImmutableList.of(REPEATER_BASE_MODEL, REPEATER_OUTPUT_MODEL);
        final ImmutableList<ResourceLocation> textureDeps = ImmutableList.of(
                new ResourceLocation("autoverse:blocks/repeater_base_front"),
                new ResourceLocation("autoverse:blocks/repeater_base_back"),
                new ResourceLocation("autoverse:blocks/repeater_base_side"),
                new ResourceLocation("autoverse:blocks/repeater_rod_side_unpowered"),
                new ResourceLocation("autoverse:blocks/repeater_rod_side_powered"),
                new ResourceLocation("autoverse:blocks/repeater_rod_end_unpowered"),
                new ResourceLocation("autoverse:blocks/repeater_rod_end_powered"),
                new ResourceLocation("autoverse:blocks/repeater_rod_bulge_base"),
                new ResourceLocation("autoverse:blocks/repeater_delay_01"),
                new ResourceLocation("autoverse:blocks/repeater_delay_02"),
                new ResourceLocation("autoverse:blocks/repeater_delay_03"),
                new ResourceLocation("autoverse:blocks/repeater_delay_04"),
                new ResourceLocation("autoverse:blocks/repeater_delay_05"),
                new ResourceLocation("autoverse:blocks/repeater_delay_06"),
                new ResourceLocation("autoverse:blocks/repeater_delay_07"),
                new ResourceLocation("autoverse:blocks/repeater_delay_08"),
                new ResourceLocation("autoverse:blocks/repeater_delay_09"),
                new ResourceLocation("autoverse:blocks/repeater_delay_10"),
                new ResourceLocation("autoverse:blocks/repeater_delay_11"),
                new ResourceLocation("autoverse:blocks/repeater_delay_12"),
                new ResourceLocation("autoverse:blocks/repeater_delay_13"),
                new ResourceLocation("autoverse:blocks/repeater_delay_14"),
                new ResourceLocation("autoverse:blocks/repeater_delay_15"),
                new ResourceLocation("autoverse:blocks/repeater_delay_16"),
                new ResourceLocation("autoverse:blocks/repeater_delay_17"),
                new ResourceLocation("autoverse:blocks/repeater_delay_18"),
                new ResourceLocation("autoverse:blocks/repeater_delay_19"),
                new ResourceLocation("autoverse:blocks/repeater_delay_20")
        );

        builder.put(FAKE_LOCATION_REPEATER, (ml) -> new ModelBase(ml, BakedModelRepeater::new, modelDeps, textureDeps, ImmutableMap.of()));

        return builder.build();
    }

    public static ICustomModelLoader getModelLoader()
    {
        return new ModelLoaderBase(getModelFactories(), BakedModelRepeater::clearQuadCache);
    }

    public static class StateMapper extends StateMapperBase
    {
        private static final ModelResourceLocation LOCATION_REPEATER = new ModelResourceLocation(Reference.MOD_ID + ":repeater", "");

        @Override
        protected ModelResourceLocation getModelResourceLocation(IBlockState state)
        {
            return LOCATION_REPEATER;
        }
    }
}
