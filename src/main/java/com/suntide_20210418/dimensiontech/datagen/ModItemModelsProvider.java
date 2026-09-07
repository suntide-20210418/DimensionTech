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

        String enchantmentMarkName = ResourceLocationHelper.getPath(ModItems.ENCHANTMENT_MARK_ID);
        withExistingParent(enchantmentMarkName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.itemModelTexture(enchantmentMarkName));

        String coreName = ResourceLocationHelper.getPath(ModItems.DIMENSION_DECONSTRUCTION_CORE_ID);
        withExistingParent(coreName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.itemModelTexture(coreName));
        for (int tier = 1; tier <= 6; tier++) {
            for (String name :
                    new String[] {"dimension_fragment_tier_" + tier, "mining_token_tier_" + tier}) {
                withExistingParent(name, ResourceLocationHelper.vanilla("item/generated"))
                        .texture("layer0", ResourceLocationHelper.itemModelTexture(coreName));
            }
        }
        for (String name : new String[] {"data_integrator", "structure_interpreter"})
            withExistingParent(name, ResourceLocationHelper.vanilla("item/generated"))
                    // Reuse the existing core texture until dedicated plugin artwork is added.
                    .texture("layer0", ResourceLocationHelper.itemModelTexture(coreName));

        String[] fluidBuckets = {
            "mythic_essence_bucket",
            "surging_mythic_essence_bucket",
            "recursive_essence_bucket",
            "surging_recursive_essence_bucket",
            "fractal_essence_bucket"
        };
        ModFluids.EssenceFluid[] fluids = {
            ModFluids.MYTHIC_ESSENCE,
            ModFluids.SURGING_MYTHIC_ESSENCE,
            ModFluids.RECURSIVE_ESSENCE,
            ModFluids.SURGING_RECURSIVE_ESSENCE,
            ModFluids.FRACTAL_ESSENCE
        };
        for (int i = 0; i < fluidBuckets.length; i++) {
            withExistingParent(
                            fluidBuckets[i],
                            ResourceLocation.fromNamespaceAndPath("forge", "item/bucket"))
                    .customLoader(DynamicFluidContainerModelBuilder::begin)
                    .fluid(fluids[i].source().get());
        }
    }
}
