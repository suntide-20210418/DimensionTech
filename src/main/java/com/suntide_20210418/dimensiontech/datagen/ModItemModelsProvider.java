package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelsProvider extends ItemModelProvider {
    public ModItemModelsProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, DimensionTechMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        String markerName = ResourceLocationHelper.getPath(ModItems.STRUCTURE_MARKER_ID);
        withExistingParent(markerName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.itemModelTexture(markerName));

        String chestMarkerName = ResourceLocationHelper.getPath(ModItems.CHEST_MARKER_ID);
        withExistingParent(chestMarkerName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.itemModelTexture(chestMarkerName));

        String coreName = ResourceLocationHelper.getPath(ModItems.DIMENSION_DECONSTRUCTION_CORE_ID);
        withExistingParent(coreName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.itemModelTexture(coreName));

        String wrenchName = ResourceLocationHelper.getPath(ModItems.WRENCH_ID);
        withExistingParent(wrenchName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.itemModelTexture(wrenchName));
        for (int tier = 1; tier <= 6; tier++) {
            String fragmentName = "dimension_fragment_tier_" + tier;
            withExistingParent(fragmentName, ResourceLocationHelper.vanilla("item/generated"))
                    .texture("layer0", ResourceLocationHelper.itemModelTexture(fragmentName));

            String tokenName = "mining_token_tier_" + tier;
            withExistingParent(tokenName, ResourceLocationHelper.vanilla("item/generated"))
                    .texture("layer0", ResourceLocationHelper.itemModelTexture(tokenName));
        }
        for (String name : new String[] {"data_integrator", "structure_interpreter"})
            withExistingParent(name, ResourceLocationHelper.vanilla("item/generated"))
                    .texture("layer0", ResourceLocationHelper.itemModelTexture(name));

        String[] fluidBuckets = {
            "structure_essence_bucket",
            "surging_structure_essence_bucket",
            "recursive_essence_bucket",
            "surging_recursive_essence_bucket",
            "fractal_essence_bucket"
        };
        ModFluids.EssenceFluid[] fluids = {
            ModFluids.STRUCTURE_ESSENCE,
            ModFluids.SURGING_STRUCTURE_ESSENCE,
            ModFluids.RECURSIVE_ESSENCE,
            ModFluids.SURGING_RECURSIVE_ESSENCE,
            ModFluids.FRACTAL_ESSENCE
        };
        for (int i = 0; i < fluidBuckets.length; i++) {
            withExistingParent(
                            fluidBuckets[i],
                            new ResourceLocation("forge", "item/bucket"))
                    .customLoader(DynamicFluidContainerModelBuilder::begin)
                    .fluid(fluids[i].source().get());
        }
    }
}
