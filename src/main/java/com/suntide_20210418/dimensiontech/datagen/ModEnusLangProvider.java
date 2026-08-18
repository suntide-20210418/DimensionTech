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
        add("screen.dimension_tech.struct_marker.title", "Structure Marker Analysis");
        add("screen.dimension_tech.struct_marker.subtitle", "Loot expectations and value overview");
        add("screen.dimension_tech.struct_marker.dimension", "Dimension: %s");
        add("screen.dimension_tech.struct_marker.structure", "Structure: %s");
        add("screen.dimension_tech.struct_marker.dimension_value", "Dimension Value");
        add("screen.dimension_tech.struct_marker.structure_value", "Structure Value");
        add("screen.dimension_tech.struct_marker.structures", "Indexed structures: %s");
        add("screen.dimension_tech.struct_marker.item", "Item");
        add("screen.dimension_tech.struct_marker.expected", "Expected");
        add("screen.dimension_tech.struct_marker.multiplier_header", "Multiplier");
        add("screen.dimension_tech.struct_marker.multiplier", "Multiplier: %s");
        add("screen.dimension_tech.struct_marker.no_items", "No expected items available");
        add("screen.dimension_tech.mythic_miner.control", "Mining Console");
        add("screen.dimension_tech.mythic_miner.markers", "Structure Markers");
        add("screen.dimension_tech.mythic_miner.slots", "%s slots");
        add("screen.dimension_tech.mythic_miner.target", "Current Target");
        add("screen.dimension_tech.mythic_miner.dimension", "Dimension: %s");
        add("screen.dimension_tech.mythic_miner.progress", "Run Progress");
        add("screen.dimension_tech.mythic_miner.energy_tooltip", "Energy Status");
        add("screen.dimension_tech.mythic_miner.energy_value", "Reserve: %s / %s FE");
        add("screen.dimension_tech.mythic_miner.energy_consumption", "Consumption: %s FE/t");
        add("screen.dimension_tech.mythic_miner.energy", "Energy Reserve");
        add("screen.dimension_tech.mythic_miner.parallel", "Parallel Draws");
        add("screen.dimension_tech.mythic_miner.current_parallel", "Current Parallel");
        add("screen.dimension_tech.mythic_miner.extra_parallel", "Extra Parallel");
        add("screen.dimension_tech.mythic_miner.extra_items", "Extra Items");
        add("screen.dimension_tech.mythic_miner.output", "Output");
        add("screen.dimension_tech.mythic_miner.output.me_network", "ME Network");
        add("screen.dimension_tech.mythic_miner.output.item_handler", "Item Container");
        add("screen.dimension_tech.mythic_miner.output.none", "Unlinked");
        add("screen.dimension_tech.mythic_miner.inventory", "Player Inventory");
        add(
                TranslateHelper.message("struct_marker.saved"),
                "Saved %s at %s, %s, %s; structures found: %s");
        add(TranslateHelper.tooltip("struct_marker.dimension"), "Dimension: %s");
        add(TranslateHelper.tooltip("struct_marker.dimension_value"), "Dimension value: %s");
        add(TranslateHelper.tooltip("struct_marker.structure_value"), "Structure value: %s");
        add(TranslateHelper.tooltip("struct_marker.analysis_status"), "Loot analysis: %s");
        add("screen.dimension_tech.struct_marker.calculation_method", "Calculation method: %s");
        add("screen.dimension_tech.struct_marker.analysis_status.exact", "Exact");
        add("screen.dimension_tech.struct_marker.analysis_status.approximate", "Approximate");
        add("screen.dimension_tech.struct_marker.analysis_status.unsupported", "Unsupported");
        add("screen.dimension_tech.struct_marker.analysis_status.legacy", "Legacy");
        add(
                TranslateHelper.tooltip("struct_marker.legacy"),
                "Legacy value hidden; mark the structure again");
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
