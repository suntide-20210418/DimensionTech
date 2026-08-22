package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelsProvider extends ItemModelProvider {
    public ModItemModelsProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, DimensionTechMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        String markerName = ResourceLocationHelper.getPath(ModItems.STRUCT_MARKER_ID);
        withExistingParent(markerName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.itemModelTexture(markerName));

        String enchantmentMarkName = ResourceLocationHelper.getPath(ModItems.ENCHANTMENT_MARK_ID);
        withExistingParent(enchantmentMarkName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.vanilla("item/echo_shard"));

        String coreName = ResourceLocationHelper.getPath(ModItems.DIMENSION_DECONSTRUCTION_CORE_ID);
        withExistingParent(coreName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.vanilla("item/echo_shard"));
    }
}
