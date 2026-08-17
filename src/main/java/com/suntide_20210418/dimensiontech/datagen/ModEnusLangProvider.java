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
        add(TranslateHelper.item("enchantment_mark"), "Enchantment Mark");
        add(TranslateHelper.block("tier_1_mythic_miner"), "Tier 1 Mythic Miner");
        add(TranslateHelper.container("tier_1_mythic_miner"), "Tier 1 Mythic Miner");
        add(
                TranslateHelper.message("struct_marker.saved"),
                "Saved %s at %s, %s, %s; structures found: %s");
        add(TranslateHelper.tooltip("struct_marker.dimension"), "Dimension: %s");
        add(TranslateHelper.tooltip("struct_marker.dimension_value"), "Dimension value: %s");
        add(TranslateHelper.tooltip("struct_marker.structure_value"), "Structure value: %s");
        add(TranslateHelper.tooltip("struct_marker.analysis_status"), "Loot analysis: %s");
        add(TranslateHelper.tooltip("struct_marker.legacy"), "Legacy value hidden; mark the structure again");
        add(TranslateHelper.tooltip("struct_marker.structure"), "Structure: %s");
        add(TranslateHelper.tooltip("struct_marker.no_structure"), "Structure: None");
        add(TranslateHelper.tooltip("enchantment_mark.enchantment"), "Enchantment: %s");
        add(TranslateHelper.tooltip("enchantment_mark.unbound"), "Unbound");
        add("config.jade.plugin_dimension_tech.mythic_miner_status", "Mythic Miner Status");
        add("jade.dimension_tech.status.idle", "Idle");
        add("jade.dimension_tech.status.running", "Running");
        add("jade.dimension_tech.status.blocked", "Output blocked");
        add("jade.dimension_tech.status", "Status: %s");
        add("jade.dimension_tech.structure", "Structure: %s");
        add("jade.dimension_tech.structure.none", "None");
        add("jade.dimension_tech.structure.minecraft.village_plains", "Plains Village");
        add("jade.dimension_tech.structure.minecraft.village_desert", "Desert Village");
        add("jade.dimension_tech.structure.minecraft.village_savanna", "Savanna Village");
        add("jade.dimension_tech.structure.minecraft.village_snowy", "Snowy Village");
        add("jade.dimension_tech.structure.minecraft.village_taiga", "Taiga Village");
        add("jade.dimension_tech.progress", "Progress: %s");
        add("jade.dimension_tech.progress_value", "Progress: %s%%");
        add("jade.dimension_tech.remaining", "Remaining: %s seconds");
        add("jade.dimension_tech.parallel", "Parallel: %s");
        add("jade.dimension_tech.output", "Output: %s");
        add("jade.dimension_tech.output.me_network", "ME network");
        add("jade.dimension_tech.output.item_handler", "Inventory");
        add("jade.dimension_tech.output.none", "No output");
        add("jade.dimension_tech.pending", "Pending output: %s items");
        add("jade.dimension_tech.reason", "Reason: %s");
        add("jade.dimension_tech.reason.me_full", "ME network storage is full");
        add("jade.dimension_tech.reason.inventory_full", "Inventory is full");
        add("jade.dimension_tech.reason.no_target", "No output container");
        add("jade.dimension_tech.energy", "Energy: %s");
        add("jade.dimension_tech.consumption", "Consumption: %s");
        add("jade.dimension_tech.energy_value", "%s/%s FE");
        add("jade.dimension_tech.energy_consumption_value", "%s FE/t");
        add("jade.dimension_tech.seconds", "%s");
        add("jade.dimension_tech.items", "%s");
    }
}
