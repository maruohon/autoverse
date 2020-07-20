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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import fi.dy.masa.autoverse.block.BlockInventoryReader;
import fi.dy.masa.autoverse.block.base.AutoverseBlocks;
import fi.dy.masa.autoverse.reference.Reference;

public class BakedModelInventoryReader extends BakedModelBase
{
    public static final ResourceLocation FAKE_LOCATION_ITEMS = new ResourceLocation(Reference.MOD_ID, "models/block/custom/inventory_reader_items");
    public static final ResourceLocation FAKE_LOCATION_SLOTS = new ResourceLocation(Reference.MOD_ID, "models/block/custom/inventory_reader_slots");

    public static final ResourceLocation READER_BASE_MODEL   = new ResourceLocation(Reference.MOD_ID, "block/inventory_reader_base");
    public static final ResourceLocation READER_OUTPUT_MODEL = new ResourceLocation(Reference.MOD_ID, "block/inventory_reader_output");

    private static final ResourceLocation PARTICLE_TEXTURE_SLOTS = new ResourceLocation(Reference.MOD_ID, "blocks/inventory_reader_rod_bulge_slots");
    private static final ResourceLocation PARTICLE_TEXTURE_ITEMS = new ResourceLocation(Reference.MOD_ID, "blocks/inventory_reader_rod_bulge_items");

    private static final Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> QUAD_CACHE = new HashMap<>();

    private final BlockInventoryReader.ReaderType type;
    private final IModel outputModel;

    private BakedModelInventoryReader(ResourceLocation modelLocation,
                                      ImmutableMap<String, String> textures,
                                      IModelState modelState,
                                      VertexFormat format,
                                      Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        super(READER_BASE_MODEL, modelLocation.equals(FAKE_LOCATION_ITEMS) ? PARTICLE_TEXTURE_ITEMS : PARTICLE_TEXTURE_SLOTS,
              textures, QUAD_CACHE, modelState, format, bakedTextureGetter);

        this.type = modelLocation.equals(FAKE_LOCATION_ITEMS) ? BlockInventoryReader.ReaderType.ITEMS : BlockInventoryReader.ReaderType.SLOTS;
        this.outputModel = getModelOrMissing(READER_OUTPUT_MODEL);
    }

    @Override
    protected IBlockState getBaseStateForItemModel()
    {
        return AutoverseBlocks.INVENTORY_READER.getDefaultState().withProperty(BlockInventoryReader.TYPE, this.type);
    }

    @Override
    protected List<IBakedModel> getModelParts(IBlockState state)
    {
        List<IBakedModel> models = new ArrayList<>();

        boolean powered = state.getValue(BlockInventoryReader.POWERED);
        ImmutableMap<String, String> baseTextures = this.getBaseModelTextures(powered, this.type == BlockInventoryReader.ReaderType.ITEMS);

        models.add(this.baseModel  .retexture(baseTextures).bake(TRSRTransformation.from(state.getValue(BlockInventoryReader.FACING)), this.format, this.bakedTextureGetter));
        // The model is made as a "south-to-north" straight model. But since the output facing in that case
        // will be south and not north like the usual default facing, we need to flip it here.
        models.add(this.outputModel.retexture(baseTextures).bake(TRSRTransformation.from(state.getValue(BlockInventoryReader.FACING_OUT).getOpposite()), this.format, this.bakedTextureGetter));

        return models;
    }

    private ImmutableMap<String, String> getBaseModelTextures(boolean powered, boolean items)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put("base_front", this.textures.get("base_front"));
        builder.put("base_back",  this.textures.get("base_back"));
        builder.put("base_side",  this.textures.get("base_side"));
        builder.put("rod_side",   this.textures.get("rod_side_" + (powered ? "powered" : "unpowered")));
        builder.put("rod_bulge",  this.textures.get("rod_bulge_" + (items ? "items" : "slots")));
        builder.put("rod_end",    this.textures.get("rod_end_" + (powered ? "powered" : "unpowered")));

        return builder.build();
    }

    private static void clearQuadCache()
    {
        QUAD_CACHE.clear();
    }

    private static ImmutableMap<ResourceLocation, ModelLoaderBase.IModelFactory> getModelFactories()
    {
        ImmutableMap.Builder<ResourceLocation, ModelLoaderBase.IModelFactory> builder = ImmutableMap.builder();

        final ImmutableList<ResourceLocation> modelDeps = ImmutableList.of(READER_BASE_MODEL, READER_OUTPUT_MODEL);
        final ImmutableList<ResourceLocation> textureDeps = ImmutableList.of(
                new ResourceLocation("autoverse:blocks/inventory_reader_base_front"),
                new ResourceLocation("autoverse:blocks/inventory_reader_base_back"),
                new ResourceLocation("autoverse:blocks/inventory_reader_base_side"),
                new ResourceLocation("autoverse:blocks/inventory_reader_rod_side_unpowered"),
                new ResourceLocation("autoverse:blocks/inventory_reader_rod_side_powered"),
                new ResourceLocation("autoverse:blocks/inventory_reader_rod_bulge_slots"),
                new ResourceLocation("autoverse:blocks/inventory_reader_rod_bulge_items"),
                new ResourceLocation("autoverse:blocks/inventory_reader_rod_end_unpowered"),
                new ResourceLocation("autoverse:blocks/inventory_reader_rod_end_powered")
        );

        builder.put(FAKE_LOCATION_ITEMS, (ml) -> new ModelBase(ml, BakedModelInventoryReader::new, modelDeps, textureDeps, ImmutableMap.of()));
        builder.put(FAKE_LOCATION_SLOTS, (ml) -> new ModelBase(ml, BakedModelInventoryReader::new, modelDeps, textureDeps, ImmutableMap.of()));

        return builder.build();
    }

    public static ICustomModelLoader getModelLoader()
    {
        return new ModelLoaderBase(getModelFactories(), BakedModelInventoryReader::clearQuadCache);
    }

    public static class StateMapper extends StateMapperBase
    {
        private static final ModelResourceLocation LOCATION_ITEMS = new ModelResourceLocation(Reference.MOD_ID + ":inventory_reader", "type=items");
        private static final ModelResourceLocation LOCATION_SLOTS = new ModelResourceLocation(Reference.MOD_ID + ":inventory_reader", "type=slots");

        @Override
        protected ModelResourceLocation getModelResourceLocation(IBlockState state)
        {
            switch (state.getValue(BlockInventoryReader.TYPE))
            {
                case SLOTS: return LOCATION_SLOTS;
                case ITEMS:
                default:    return LOCATION_ITEMS;
            }
        }
    }
}
