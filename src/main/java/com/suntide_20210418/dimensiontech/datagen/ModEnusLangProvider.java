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
        add(TranslateHelper.item("dimension_deconstruction_core"), "Dimension Deconstruction Core");
        add(TranslateHelper.block("tier_1_mythic_miner"), "Tier 1 Mythic Miner");
        add(TranslateHelper.container("tier_1_mythic_miner"), "Tier 1 Mythic Miner");
        for (int tier = 2; tier <= 6; tier++) {
            add(
                    "block.dimension_tech.tier_" + tier + "_mythic_miner",
                    "Tier " + tier + " Mythic Miner");
            add(
                    "container.dimension_tech.tier_" + tier + "_mythic_miner",
                    "Tier " + tier + " Mythic Miner");
        }
        add("block.dimension_tech.mythic_miner_casing", "Miner Casing");
        add("block.dimension_tech.mythic_miner_structure", "Miner Structure Block");
        add("block.dimension_tech.mythic_miner_upgrade_parallel", "Tier 1 Parallel Upgrade");
        add("block.dimension_tech.mythic_miner_upgrade_luck", "Tier 1 Luck Upgrade");
        add("block.dimension_tech.mythic_miner_upgrade_energy", "Tier 1 Energy Upgrade");
        add("block.dimension_tech.mythic_miner_upgrade_efficiency", "Tier 1 Efficiency Upgrade");
        add("block.dimension_tech.mythic_miner_upgrade_aggregate", "Tier 1 Aggregate Upgrade");
        for (int tier = 2; tier <= 6; tier++) {
            add(
                    "block.dimension_tech.mythic_miner_upgrade_parallel_tier_" + tier,
                    "Tier " + tier + " Parallel Upgrade");
            add(
                    "block.dimension_tech.mythic_miner_upgrade_luck_tier_" + tier,
                    "Tier " + tier + " Luck Upgrade");
            add(
                    "block.dimension_tech.mythic_miner_upgrade_energy_tier_" + tier,
                    "Tier " + tier + " Energy Upgrade");
            add(
                    "block.dimension_tech.mythic_miner_upgrade_efficiency_tier_" + tier,
                    "Tier " + tier + " Efficiency Upgrade");
            add(
                    "block.dimension_tech.mythic_miner_upgrade_aggregate_tier_" + tier,
                    "Tier " + tier + " Aggregate Upgrade");
        }
        add("block.dimension_tech.mythic_miner_upgrade_none", "No Upgrade Block");
        for (int tier = 1; tier <= 6; tier++)
            add("block.dimension_tech.dimension_focus_tier_" + tier, "Dimension Focus " + tier);
        add("screen.dimension_tech.mythic_miner.place_structure", "Place Multiblock Structure");
        add("screen.dimension_tech.mythic_miner.equipment_dismantling", "Equipment Dismantling");
        add("screen.dimension_tech.mythic_miner.enabled", "Enabled");
        add("screen.dimension_tech.mythic_miner.disabled", "Disabled");
        add("screen.dimension_tech.mythic_miner.structure_incomplete", "Structure incomplete");
        add("message.dimension_tech.mythic_miner.projection_on", "Multiblock projection shown");
        add("message.dimension_tech.mythic_miner.projection_off", "Multiblock projection hidden");
        add("tooltip.dimension_tech.mythic_miner.base_parallel", "Base parallel: %s");
        add("tooltip.dimension_tech.mythic_miner.efficiency", "Efficiency: %s");
        add("tooltip.dimension_tech.mythic_miner.luck", "Luck: %s");
        add("tooltip.dimension_tech.mythic_miner.energy_capacity", "Energy capacity: %s FE");
        add(
                "tooltip.dimension_tech.mythic_miner.energy_consumption",
                "Energy consumption: %s FE/t");
        add("tooltip.dimension_tech.mythic_miner.marker_slots", "Marker slots: %s");
        add(
                "tooltip.dimension_tech.mythic_miner.hold_shift",
                "Hold Shift for multiblock materials");
        add("tooltip.dimension_tech.mythic_miner.materials", "Multiblock Materials");
        add("tooltip.dimension_tech.mythic_miner.material.casing", "Miner Casing x%s");
        add("tooltip.dimension_tech.mythic_miner.material.structure", "Miner Structure Block x%s");
        add("tooltip.dimension_tech.mythic_miner.material.focus", "Tier %s Dimension Focus x%s");
        add("tooltip.dimension_tech.mythic_miner.material.upgrade", "Any Upgrade Block x%s");
        add("tooltip.dimension_tech.mythic_miner.upgrade.efficiency", "Efficiency: +%s%%");
        add(
                "tooltip.dimension_tech.mythic_miner.upgrade.energy_capacity",
                "Energy capacity: +%s%%");
        add(
                "tooltip.dimension_tech.mythic_miner.upgrade.energy_consumption.multiplicative",
                "Energy consumption (multiplicative): -%s%%");
        add(
                "tooltip.dimension_tech.mythic_miner.upgrade.energy_consumption.additive",
                "Energy consumption (additive): -%s%%");
        add("tooltip.dimension_tech.mythic_miner.upgrade.parallel", "Parallel: +%s%%");
        add("tooltip.dimension_tech.mythic_miner.upgrade.luck", "Luck: +%s%%");
        add("tooltip.dimension_tech.mythic_miner.upgrade.none", "No attribute bonuses");
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
        add("screen.dimension_tech.struct_marker.select", "Select Structure");
        add("screen.dimension_tech.struct_marker.clear", "Clear Structure");
        add(
                "screen.dimension_tech.struct_marker.select_prompt",
                "Multiple structures found here; select one");
        add("screen.dimension_tech.struct_marker.selection.selected", "Structure selected");
        add("screen.dimension_tech.struct_marker.selection.empty", "No structure selected");
        add(
                "message.dimension_tech.struct_marker.no_structure_here",
                "No structure exists at this position");
        add(
                "message.dimension_tech.struct_marker.selection_invalid",
                "Structure choices changed; select again");
        add("screen.dimension_tech.mythic_miner.control", "Mining Console");
        add("screen.dimension_tech.mythic_miner.markers", "Structure Markers");
        add("screen.dimension_tech.mythic_miner.slots", "%s slots");
        add("screen.dimension_tech.mythic_miner.target", "Current Target");
        add("screen.dimension_tech.mythic_miner.dimension", "Dimension: %s");
        add("screen.dimension_tech.mythic_miner.energy_tooltip", "Energy Status");
        add("screen.dimension_tech.mythic_miner.energy_value", "Reserve: %s / %s FE");
        add("screen.dimension_tech.mythic_miner.energy_consumption", "Consumption: %s FE/t");
        add("screen.dimension_tech.mythic_miner.energy", "Energy Reserve");
        add("screen.dimension_tech.mythic_miner.parallel", "Parallel Draws");
        add("screen.dimension_tech.mythic_miner.attribute.efficiency", "Efficiency");
        add("screen.dimension_tech.mythic_miner.attribute.capacity", "Capacity");
        add("screen.dimension_tech.mythic_miner.attribute.consumption", "Consumption");
        add("screen.dimension_tech.mythic_miner.attribute.parallel", "Parallel");
        add("screen.dimension_tech.mythic_miner.attribute.luck", "Luck");
        add("screen.dimension_tech.mythic_miner.attribute.external_acceleration", "External acceleration");
        add("screen.dimension_tech.mythic_miner.tab.work", "Work");
        add("screen.dimension_tech.mythic_miner.tab.info", "Info");
        add("screen.dimension_tech.mythic_miner.tab.attributes", "Attributes");
        add("screen.dimension_tech.mythic_miner.info.slots", "Marker slots");
        add("screen.dimension_tech.mythic_miner.info.structure", "Structure / cycle: %s tick");
        add("screen.dimension_tech.mythic_miner.info.natural", "Natural ticks: %s");
        add("screen.dimension_tech.mythic_miner.info.actual", "Actual ticks: %s");
        add("screen.dimension_tech.mythic_miner.info.external", "External acceleration: %sx");
        add("screen.dimension_tech.mythic_miner.waiting_for_natural_window", "Waiting for natural tick window (400 ticks)");
        add("screen.dimension_tech.mythic_miner.attribute.upgrades", "Upgrade Blocks");
        add("screen.dimension_tech.mythic_miner.attribute.upgrade_count", "×%s");
        add("screen.dimension_tech.mythic_miner.attribute.total_short", "Total %s");
        add("screen.dimension_tech.mythic_miner.attribute.summary_counts", "Focus %s | Combo %s");
        add("screen.dimension_tech.mythic_miner.attribute.efficiency_value", "Effective efficiency: %s");
        add("screen.dimension_tech.mythic_miner.attribute.capacity_value", "Effective capacity: %s FE");
        add("screen.dimension_tech.mythic_miner.attribute.consumption_value", "Effective consumption: %s FE/t");
        add("screen.dimension_tech.mythic_miner.attribute.parallel_value", "Effective parallel: %s");
        add("screen.dimension_tech.mythic_miner.attribute.luck_value", "Effective luck: %s");
        add("screen.dimension_tech.mythic_miner.attribute.bonus", "Final bonus: +%s%%");
        add("screen.dimension_tech.mythic_miner.attribute.reduction", "Final reduction: %s%%");
        add("screen.dimension_tech.mythic_miner.attribute.total_count", "Total upgrade blocks: %s");
        add(
                "screen.dimension_tech.mythic_miner.attribute.upgrade_breakdown",
                "Focused upgrades: %s, aggregate upgrades: %s");
        add("screen.dimension_tech.mythic_miner.output.me_network", "ME Network");
        add("screen.dimension_tech.mythic_miner.output.item_handler", "Item Container");
        add("screen.dimension_tech.mythic_miner.output.none", "Unlinked");
        add("screen.dimension_tech.mythic_miner.inventory", "Player Inventory");
        add("screen.dimension_tech.mythic_miner.output_face", "Output Face");
        add("screen.dimension_tech.mythic_miner.output_mode", "Output State");
        add("screen.dimension_tech.mythic_miner.redstone", "Redstone");
        add("screen.dimension_tech.mythic_miner.redstone_mode", "Redstone: %s");
        add("screen.dimension_tech.mythic_miner.redstone.always", "Always on");
        add("screen.dimension_tech.mythic_miner.redstone.signal", "With signal");
        add("screen.dimension_tech.mythic_miner.redstone.no_signal", "Without signal");
        add("screen.dimension_tech.mythic_miner.redstone.never", "Never");
        add("screen.dimension_tech.mythic_miner.marker_progress", "Progress: %s / %s tick");
        add("screen.dimension_tech.mythic_miner.marker_info.select", "Click a progress bar to select a marker");
        add("screen.dimension_tech.mythic_miner.marker_info.structure", "Slot %s: %s");
        add("screen.dimension_tech.mythic_miner.marker_info.dimension", "Dimension: %s");
        add("screen.dimension_tech.mythic_miner.marker_info.position", "Position: %s, %s, %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.dimension_value",
                "Dimension value: %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.structure_value",
                "Structure value: %s");
        add("screen.dimension_tech.mythic_miner.marker_info.expected_items", "Item expectations");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.loading",
                "Calculating effective machine expectations...");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.no_items",
                "No item expectations available");
        add("screen.dimension_tech.mythic_miner.slot.enable", "Enable selected slot");
        add("screen.dimension_tech.mythic_miner.slot.disable", "Disable selected slot");
        add("screen.dimension_tech.mythic_miner.marker_info.parallel", "Total parallel: %s  [%s]");
        add("screen.dimension_tech.mythic_miner.marker_info.parallel.expand", "Click to expand");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.parallel.collapse",
                "Click to collapse");
        add("screen.dimension_tech.mythic_miner.marker_info.parallel.base", "Base parallel: %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.parallel.efficiency",
                "Efficiency parallel: %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.parallel.external",
                "External acceleration parallel: %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.natural_ticks",
                "Natural ticks in current window: %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.actual_ticks",
                "Actual ticks: %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.actual_parallel",
                "Live external acceleration parallel: %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.previous_cycle_ticks",
                "Previous cycle actual ticks: %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.previous_cycle_parallel",
                "Previous cycle external acceleration parallel: %s");
        add(
                "screen.dimension_tech.mythic_miner.marker_parallel",
                "Parallel: %s (%s base + %s efficiency bonus + %s external acceleration)");
        add("screen.dimension_tech.mythic_miner.face.selected", "Selected face: %s");
        add("screen.dimension_tech.mythic_miner.output_faces_enabled", "%s / 6 faces enabled");
        add("screen.dimension_tech.mythic_miner.face.tooltip", "Face: %s");
        add("screen.dimension_tech.mythic_miner.face.adjacent", "Adjacent machine: %s");
        add("screen.dimension_tech.mythic_miner.face.state", "Output: %s");
        add("screen.dimension_tech.mythic_miner.face.empty", "No block");
        add("screen.dimension_tech.mythic_miner.face.north", "Front");
        add("screen.dimension_tech.mythic_miner.face.south", "Back");
        add("screen.dimension_tech.mythic_miner.face.east", "East");
        add("screen.dimension_tech.mythic_miner.face.west", "West");
        add("screen.dimension_tech.mythic_miner.face.up", "Up");
        add("screen.dimension_tech.mythic_miner.face.down", "Down");
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
        add("jade.dimension_tech.slot", "Slot %s: %s");
        add("jade.dimension_tech.slot_progress", "Progress: %s");
        add("jade.dimension_tech.slot_progress_value", "%s / %s tick");
        add("jade.dimension_tech.slot_progress_percent", "%s%%");
        add("jade.dimension_tech.slot_parallel", "Parallel: %s");
        add("jade.dimension_tech.base_parameters", "Base parameters: %s");
        add("jade.dimension_tech.parameters", "Miner");
        add("jade.dimension_tech.efficiency", "Efficiency: %s");
        add("jade.dimension_tech.capacity", "Energy capacity: %s");
        add("jade.dimension_tech.base_consumption", "Base consumption: %s");
        add("jade.dimension_tech.base_parallel", "Base parallel: %s");
        add("jade.dimension_tech.luck", "Base luck: %s");
        add("jade.dimension_tech.slot_usage", "Slots: %s/%s");
        add("jade.dimension_tech.remaining", "Remaining: %s seconds");
        add("jade.dimension_tech.parallel", "Parallel: %s");
        add("jade.dimension_tech.external_parallel", "External acceleration parallel: %s");
        add("jade.dimension_tech.external_equivalent_acceleration", "External equivalent acceleration: %sx");
        add("jade.dimension_tech.waiting_for_natural_window", "Waiting for natural tick window (400 ticks)");
        add("jade.dimension_tech.actual_ticks", "Machine ticks in current window: %s");
        add("jade.dimension_tech.previous_ticks", "Previous cycle machine ticks: %s");
        add("jade.dimension_tech.previous_parallel", "Previous cycle external parallel: %s");
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
