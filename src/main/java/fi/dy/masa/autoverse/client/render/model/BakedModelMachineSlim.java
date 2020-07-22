package fi.dy.masa.autoverse.client.render.model;

import java.util.ArrayList;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.block.base.BlockMachineSlimBase;
import fi.dy.masa.autoverse.reference.Reference;
import fi.dy.masa.autoverse.util.PositionUtils;

public class BakedModelMachineSlim extends BakedModelBase
{
    private static final ResourceLocation FAKE_LOCATION_MACHINE_SLIM = new ResourceLocation(Reference.MOD_ID, "models/block/custom/machine_slim");

    protected static final ResourceLocation BASE_MODEL_12    = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_main_12_tex_16");
    protected static final ResourceLocation SIDE_MODEL_08x02 = new ResourceLocation(Reference.MOD_ID, "block/machine_slim_side_08x02_tex_10x03");

    private static final Map<IBlockState, ImmutableMap<Optional<EnumFacing>, ImmutableList<BakedQuad>>> QUAD_CACHE = new HashMap<>();

    protected final IModel outModel;
    protected final IModel sideModel;

    protected BakedModelMachineSlim(ResourceLocation modelLocation,
                                  ImmutableMap<String, String> textures,
                                  IModelState modelState,
                                  VertexFormat format,
                                  Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        super(BASE_MODEL_12, textures, QUAD_CACHE, modelState, format, bakedTextureGetter);

        this.outModel = getModelOrMissing(SIDE_MODEL_08x02);
        this.sideModel = getModelOrMissing(SIDE_MODEL_08x02);
    }

    @Override
    @Nullable
    protected IBlockState getBaseStateForItemModel()
    {
        // The slim model is not used for items
        return null;
    }

    @Override
    protected List<IBakedModel> getModelParts(IBlockState state)
    {
        EnumFacing mainFacing = state.getValue(BlockAutoverse.FACING);
        List<IBakedModel> models = new ArrayList<>();

        this.getBaseModel(models, state, mainFacing);
        this.getOutputModel(models, state, mainFacing);
        this.getSideModels(models, state, mainFacing);

        return models;
    }

    protected void getBaseModel(List<IBakedModel> models, IBlockState state, EnumFacing mainFacing)
    {
        IModel baseModel = this.baseModel.retexture(this.textures);
        models.add(baseModel.bake(TRSRTransformation.from(mainFacing), this.format, this.bakedTextureGetter));
    }

    protected void getOutputModel(List<IBakedModel> models, IBlockState state, EnumFacing mainFacing)
    {
        BlockMachineSlimBase block = (BlockMachineSlimBase) state.getBlock();

        if (block.hasMainOutput())
        {
            if (block.isMainOutputOppositeToFacing())
            {
                mainFacing = mainFacing.getOpposite();
            }

            IModel outputModel = this.outModel.retexture(this.textures);
            models.add(outputModel.bake(TRSRTransformation.from(mainFacing), this.format, this.bakedTextureGetter));
        }
    }

    protected void getSideModels(List<IBakedModel> models, IBlockState state, EnumFacing mainFacing)
    {
        BlockMachineSlimBase block = (BlockMachineSlimBase) state.getBlock();

        for (int side = 0; side < block.getNumModelSideFacings(); side++)
        {
            EnumFacing relativeFacing = state.getValue(block.getPropertyFacing(side));
            EnumFacing absoluteFacing = PositionUtils.getAbsoluteFacingFromNorth(mainFacing, relativeFacing);
            IModel sideModel = this.sideModel.retexture(this.getSideModelTextures(side));
            models.add(sideModel.bake(TRSRTransformation.from(absoluteFacing), this.format, this.bakedTextureGetter));
        }
    }

    private ImmutableMap<String, String> getSideModelTextures(int sideId)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put("slim_side_end", this.textures.get("slim_side_end_" + (sideId + 2))); // start numbering from 2, as "the second side"
        builder.put("slim_side_side", this.textures.get("slim_side_side_" + (sideId + 2)));

        return builder.build();
    }

    private static void clearQuadCache()
    {
        QUAD_CACHE.clear();
    }

    private static ImmutableMap<ResourceLocation, ModelLoaderBase.IModelFactory> getModelFactories()
    {
        ImmutableMap.Builder<ResourceLocation, ModelLoaderBase.IModelFactory> builder = ImmutableMap.builder();

        final ImmutableList<ResourceLocation> modelDeps = ImmutableList.of(BASE_MODEL_12, SIDE_MODEL_08x02);
        final ImmutableList<ResourceLocation> textureDeps = ImmutableList.of();

        builder.put(FAKE_LOCATION_MACHINE_SLIM, (ml) -> new ModelBase(ml, BakedModelMachineSlim::new, modelDeps, textureDeps, ImmutableMap.of()));

        return builder.build();
    }

    public static ICustomModelLoader getModelLoader()
    {
        return new ModelLoaderBase(getModelFactories(), BakedModelMachineSlim::clearQuadCache);
    }
}
