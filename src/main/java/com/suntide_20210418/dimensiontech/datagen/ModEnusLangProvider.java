package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;


public class ModEnusLangProvider extends LanguageProvider {

    public ModEnusLangProvider(PackOutput pOutput) {
        super(pOutput, DimensionTechMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(TranslateHelper.itemGroup("tab"), "Dimension Tech");
        add(TranslateHelper.keyCategory("main"), "Dimension Tech");
        add(TranslateHelper.key("switch"), "Switch card mode");
        add(TranslateHelper.item("struct_marker"), "Structure Marker");
        add(TranslateHelper.block("tier_1_mythic_miner"), "Tier 1 Mythic Miner");
        add(TranslateHelper.container("tier_1_mythic_miner"), "Tier 1 Mythic Miner");
        add(
                TranslateHelper.message("struct_marker.saved"),
                "Saved %s at %s, %s, %s; structures found: %s");
        add(TranslateHelper.tooltip("struct_marker.dimension"), "Dimension: %s");
        add(TranslateHelper.tooltip("struct_marker.structure"), "Structure: %s");
        add(TranslateHelper.tooltip("struct_marker.no_structure"), "Structure: None");
    }
}
