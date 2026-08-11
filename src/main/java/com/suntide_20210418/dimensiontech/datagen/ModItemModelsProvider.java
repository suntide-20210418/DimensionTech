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
        String itemName = ResourceLocationHelper.getPath(ModItems.STRUCT_MARKER_ID);
        withExistingParent(itemName, ResourceLocationHelper.vanilla("item/generated"))
                .texture("layer0", ResourceLocationHelper.itemModelTexture(itemName));
    }
}
