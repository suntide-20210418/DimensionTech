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
        addVanillaRegistryNames();
        add(TranslateHelper.itemGroup("tab"), "Dimension Tech");
        add(TranslateHelper.keyCategory("main"), "Dimension Tech");
        add(TranslateHelper.key("switch"), "Switch card mode");
        add(TranslateHelper.item("structure_marker"), "Structure Marker");
        add(TranslateHelper.item("enchantment_mark"), "Enchantment Mark");
        add(TranslateHelper.item("dimension_deconstruction_core"), "Dimension Deconstruction Core");
        for (int tier = 1; tier <= 6; tier++) {
            add(
                    TranslateHelper.item("dimension_fragment_tier_" + tier),
                    "Tier " + tier + " Dimension Fragment");
            add(
                    TranslateHelper.item("mining_token_tier_" + tier),
                    "Tier " + tier + " Mining Token");
        }
        add(TranslateHelper.item("data_integrator"), "Data Integrator");
        add(TranslateHelper.item("structure_interpreter"), "Structure Interpreter");
        add("block.dimension_tech.structure_data_operator", "Structure Data Operator");
        add("block.dimension_tech.mythic_crucible", "Mythic Crucible");
        add("container.dimension_tech.mythic_crucible", "Mythic Crucible");
        add("container.dimension_tech.structure_data_operator", "Structure Data Operator");
        add("screen.dimension_tech.structure_operator.copy", "↓Copy↓");
        add("screen.dimension_tech.structure_operator.clear", "Clear");
        add("screen.dimension_tech.structure_operator.refresh", "Explored Structures");
        add("screen.dimension_tech.structure_operator.all", "All Structures");
        add("screen.dimension_tech.structure_operator.slots", "Target / Destination / Plugins");
        add("screen.dimension_tech.structure_operator.tab.operation", "Operate");
        add("screen.dimension_tech.structure_operator.tab.integrator", "Integrator");
        add("screen.dimension_tech.structure_operator.tab.interpreter", "Interpreter");
        add("screen.dimension_tech.structure_operator.target", "Target Marker");
        add("screen.dimension_tech.structure_operator.destination", "Destination Marker");
        add("screen.dimension_tech.structure_operator.source_status", "Source data");
        add("screen.dimension_tech.structure_operator.destination_status", "Write target");
        add("screen.dimension_tech.structure_operator.integrator", "Data Integrator");
        add("screen.dimension_tech.structure_operator.interpreter", "Structure Interpreter");
        add("screen.dimension_tech.structure_operator.inventory", "Player Inventory");
        add("screen.dimension_tech.structure_operator.page.integrator", "Data Integrator");
        add("screen.dimension_tech.structure_operator.page.interpreter", "Structure Interpreter");
        add(
                "screen.dimension_tech.structure_operator.source.explored",
                "Source: Explored structures (%s)");
        add(
                "screen.dimension_tech.structure_operator.source.all",
                "Source: All game structures (%s)");
        add("screen.dimension_tech.structure_operator.search", "Search structure ID");
        add("screen.dimension_tech.structure_operator.write", "Write");
        add("screen.dimension_tech.structure_operator.refresh_short", "Refresh");
        add("screen.dimension_tech.structure_operator.write_short", "Write");
        add("screen.dimension_tech.structure_operator.operands", "Destination markers: %s");
        add("screen.dimension_tech.structure_operator.empty", "No structures available");
        add("screen.dimension_tech.structure_operator.empty_marker", "Not installed");
        add("screen.dimension_tech.structure_operator.no_data", "No structure data");
        add("screen.dimension_tech.structure_operator.confirm.copy", "Confirm structure copy");
        add("screen.dimension_tech.structure_operator.confirm.write", "Confirm structure write");
        add("screen.dimension_tech.structure_operator.preview.source", "Source: %s");
        add("screen.dimension_tech.structure_operator.preview.target", "Target: %s");
        add(
                "screen.dimension_tech.structure_operator.preview.overwrite",
                "Existing data will be overwritten");
        add("screen.dimension_tech.structure_operator.preview.empty", "Target is currently empty");
        add("screen.dimension_tech.structure_operator.cancel", "Cancel");
        add("screen.dimension_tech.structure_operator.confirm", "Confirm");
        add("screen.dimension_tech.structure_operator.status.copied", "Structure data copied");
        add("screen.dimension_tech.structure_operator.status.written", "Structure data written");
        add("screen.dimension_tech.structure_operator.status.cleared", "Structure data cleared");
        add("screen.dimension_tech.structure_operator.catalogue_marker", "Catalogue marker");
        add("screen.dimension_tech.structure_operator.catalogue_analysis", "Catalogue analysis");
        add("screen.dimension_tech.structure_operator.dimension", "Dimension: %s");
        add("screen.dimension_tech.structure_operator.structure", "Structure: %s");
        add(
                "screen.dimension_tech.structure_operator.select_entry",
                "Select a structure with loot");
        add("screen.dimension_tech.structure_operator.loading", "Loading analysis data");
        add(
                TranslateHelper.block("tier_1_mythic_miner"),
                "Tier 1 Mythic Shell Chikens Void Structre Resource Miner");
        add(
                TranslateHelper.container("tier_1_mythic_miner"),
                "Tier 1 Mythic Shell Chikens Void Structre Resource Miner");
        for (int tier = 2; tier <= 5; tier++) {
            add(
                    "block.dimension_tech.tier_" + tier + "_mythic_miner",
                    "Tier " + tier + " Mythic Shell Chikens Void Structre Resource Miner");
            add(
                    "container.dimension_tech.tier_" + tier + "_mythic_miner",
                    "Tier " + tier + " Mythic Shell Chikens Void Structre Resource Miner");
        }
        add("block.dimension_tech.tier_6_mythic_miner", "Kashan Void Structre Resource Miner");
        add("container.dimension_tech.tier_6_mythic_miner", "Kashan Void Structre Resource Miner");
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
        add("screen.dimension_tech.mythic_miner.place_structure_short", "Build");
        add("screen.dimension_tech.mythic_miner.overview.working", "Working: %s / %s");
        add("screen.dimension_tech.mythic_miner.overview.active", "Active");
        add("screen.dimension_tech.mythic_miner.overview.total_parallel", "Total parallel");
        add(
                "screen.dimension_tech.mythic_miner.overview.equivalent_acceleration",
                "Cycle equivalent acceleration");
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
        add("screen.dimension_tech.mythic_miner.fluid_input", "Fluid Input");
        add("screen.dimension_tech.mythic_miner.fluid_empty", "Empty");
        add("screen.dimension_tech.mythic_miner.fluid_amount", "%s / %s mB");
        add("screen.dimension_tech.mythic_miner.fluid_required", "Required: %s mB / cycle");
        add("screen.dimension_tech.mythic_miner.fluid_insufficient", "Fluid insufficient");
        add("screen.dimension_tech.mythic_miner.fluid_wrong_type", "Wrong fluid type");
        add("screen.dimension_tech.mythic_miner.fluid_blocked", "Fluid input blocked");
        add("screen.dimension_tech.mythic_miner.fluid_status", "Status: normal");
        add("screen.dimension_tech.mythic_miner.fluid_faces", "Fluid faces");
        add("screen.dimension_tech.mythic_miner.fluid_face_mode.disabled", "Fluid: disabled");
        add("screen.dimension_tech.mythic_miner.fluid_face_mode.input", "Fluid: input");
        add("screen.dimension_tech.mythic_miner.fluid_face_mode.output", "Fluid: output");
        add(
                "screen.dimension_tech.mythic_miner.auto_extract_enabled",
                "Automatic fluid extraction: enabled");
        add(
                "screen.dimension_tech.mythic_miner.auto_extract_disabled",
                "Automatic fluid extraction: disabled");
        add("screen.dimension_tech.mythic_miner.fluid_required_type", "Accepts only this essence");
        add("fluid.dimension_tech.mythic_essence", "Mythic Essence");
        add("fluid.dimension_tech.surging_mythic_essence", "Surging Mythic Essence");
        add("fluid.dimension_tech.recursive_essence", "Recursive Essence");
        add("fluid.dimension_tech.surging_recursive_essence", "Surging Recursive Essence");
        add("fluid.dimension_tech.fractal_essence", "Fractal Essence");
        add("item.dimension_tech.mythic_essence_bucket", "Mythic Essence Bucket");
        add("item.dimension_tech.surging_mythic_essence_bucket", "Surging Mythic Essence Bucket");
        add("item.dimension_tech.recursive_essence_bucket", "Recursive Essence Bucket");
        add(
                "item.dimension_tech.surging_recursive_essence_bucket",
                "Surging Recursive Essence Bucket");
        add("item.dimension_tech.fractal_essence_bucket", "Fractal Essence Bucket");
        add("screen.dimension_tech.mythic_miner.parallel", "Parallel Draws");
        add("screen.dimension_tech.mythic_miner.attribute.efficiency", "Efficiency");
        add("screen.dimension_tech.mythic_miner.attribute.capacity", "Capacity");
        add("screen.dimension_tech.mythic_miner.attribute.consumption", "Consumption");
        add("screen.dimension_tech.mythic_miner.attribute.parallel", "Parallel");
        add("screen.dimension_tech.mythic_miner.attribute.luck", "Luck");
        add(
                "screen.dimension_tech.mythic_miner.attribute.external_acceleration",
                "External acceleration");
        add("screen.dimension_tech.mythic_miner.tab.work", "Work");
        add("screen.dimension_tech.mythic_miner.tab.info", "Info");
        add("screen.dimension_tech.mythic_miner.tab.attributes", "Attributes");
        add("screen.dimension_tech.mythic_miner.info.slots", "Marker slots");
        add("screen.dimension_tech.mythic_miner.info.structure", "Structure / cycle: %s tick");
        add("screen.dimension_tech.mythic_miner.info.natural", "Natural ticks: %s");
        add("screen.dimension_tech.mythic_miner.info.actual", "Actual ticks: %s");
        add(
                "screen.dimension_tech.mythic_miner.info.external",
                "Current cycle external acceleration: %sx");
        add("screen.dimension_tech.mythic_miner.info.total_parallel", "Total parallel: %s");
        add("screen.dimension_tech.mythic_miner.info.section.marker", "Marker properties");
        add("screen.dimension_tech.mythic_miner.info.section.work", "Work status");
        add("screen.dimension_tech.mythic_miner.info.section.products", "Products and actions");
        add("screen.dimension_tech.mythic_miner.marker_progress_toggle", "Click to %s");
        add("screen.dimension_tech.mythic_miner.expected_item.enable", "Click to enable output");
        add("screen.dimension_tech.mythic_miner.expected_item.disable", "Click to disable output");
        add("screen.dimension_tech.mythic_miner.natural_progress", "Natural ticks: %s / %s");
        add("screen.dimension_tech.mythic_miner.actual_progress", "Actual ticks: %s / %s (x%s)");
        add(
                "screen.dimension_tech.mythic_miner.parallel.expand_hint",
                "Click to expand parallel details");
        add(
                "screen.dimension_tech.mythic_miner.parallel.collapse_hint",
                "Click to collapse parallel details");
        add("screen.dimension_tech.mythic_miner.overview", "Machine overview");
        add("screen.dimension_tech.mythic_miner.overview.progress", "%s / %s tick");
        add(
                "screen.dimension_tech.mythic_miner.waiting_for_natural_window",
                "Waiting for natural tick window (400 ticks)");
        add("screen.dimension_tech.mythic_miner.attribute.upgrades", "Upgrade Blocks");
        add("screen.dimension_tech.mythic_miner.attribute.installed", "Installed upgrades");
        add("screen.dimension_tech.mythic_miner.attribute.upgrade_count", "×%s");
        add("screen.dimension_tech.mythic_miner.attribute.total_short", "Total %s");
        add("screen.dimension_tech.mythic_miner.attribute.summary_counts", "Focus %s | Combo %s");
        add(
                "screen.dimension_tech.mythic_miner.attribute.efficiency_value",
                "Effective efficiency: %s");
        add(
                "screen.dimension_tech.mythic_miner.attribute.capacity_value",
                "Effective capacity: %s FE");
        add(
                "screen.dimension_tech.mythic_miner.attribute.consumption_value",
                "Effective consumption: %s FE/t");
        add(
                "screen.dimension_tech.mythic_miner.attribute.parallel_value",
                "Effective parallel: %s");
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
        add(
                "screen.dimension_tech.mythic_miner.marker_info.select",
                "Click a progress bar to select a marker");
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
        add("screen.dimension_tech.mythic_miner.marker_info.actual_ticks", "Actual ticks: %s");
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
        add(
                "jade.dimension_tech.external_equivalent_acceleration",
                "Current cycle external acceleration: %sx");
        add(
                "jade.dimension_tech.waiting_for_natural_window",
                "Waiting for natural tick window (400 ticks)");
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

    private void addVanillaRegistryNames() {
        add("dimension_tech.dimension.minecraft.overworld", "Overworld");
        add("dimension_tech.dimension.minecraft.the_nether", "Nether");
        add("dimension_tech.dimension.minecraft.the_end", "The End");
        add("dimension_tech.structure.minecraft.pillager_outpost", "Pillager Outpost");
        add("dimension_tech.structure.minecraft.mineshaft", "Mineshaft");
        add("dimension_tech.structure.minecraft.mineshaft_mesa", "Badlands Mineshaft");
        add("dimension_tech.structure.minecraft.mansion", "Woodland Mansion");
        add("dimension_tech.structure.minecraft.jungle_pyramid", "Jungle Temple");
        add("dimension_tech.structure.minecraft.desert_pyramid", "Desert Temple");
        add("dimension_tech.structure.minecraft.igloo", "Igloo");
        add("dimension_tech.structure.minecraft.shipwreck", "Shipwreck");
        add("dimension_tech.structure.minecraft.shipwreck_beached", "Beached Shipwreck");
        add("dimension_tech.structure.minecraft.swamp_hut", "Swamp Hut");
        add("dimension_tech.structure.minecraft.stronghold", "Stronghold");
        add("dimension_tech.structure.minecraft.monument", "Ocean Monument");
        add("dimension_tech.structure.minecraft.ocean_ruin_cold", "Cold Ocean Ruin");
        add("dimension_tech.structure.minecraft.ocean_ruin_warm", "Warm Ocean Ruin");
        add("dimension_tech.structure.minecraft.fortress", "Nether Fortress");
        add("dimension_tech.structure.minecraft.nether_fossil", "Nether Fossil");
        add("dimension_tech.structure.minecraft.end_city", "End City");
        add("dimension_tech.structure.minecraft.buried_treasure", "Buried Treasure");
        add("dimension_tech.structure.minecraft.bastion_remnant", "Bastion Remnant");
        add("dimension_tech.structure.minecraft.village_plains", "Plains Village");
        add("dimension_tech.structure.minecraft.village_desert", "Desert Village");
        add("dimension_tech.structure.minecraft.village_savanna", "Savanna Village");
        add("dimension_tech.structure.minecraft.village_snowy", "Snowy Village");
        add("dimension_tech.structure.minecraft.village_taiga", "Taiga Village");
        add("dimension_tech.structure.minecraft.ruined_portal", "Ruined Portal");
        add("dimension_tech.structure.minecraft.ruined_portal_desert", "Desert Ruined Portal");
        add("dimension_tech.structure.minecraft.ruined_portal_jungle", "Jungle Ruined Portal");
        add("dimension_tech.structure.minecraft.ruined_portal_swamp", "Swamp Ruined Portal");
        add("dimension_tech.structure.minecraft.ruined_portal_mountain", "Mountain Ruined Portal");
        add("dimension_tech.structure.minecraft.ruined_portal_ocean", "Ocean Ruined Portal");
        add("dimension_tech.structure.minecraft.ruined_portal_nether", "Nether Ruined Portal");
        add("dimension_tech.structure.minecraft.ancient_city", "Ancient City");
        add("dimension_tech.structure.minecraft.trail_ruins", "Trail Ruins");
        add("dimension_tech.structure.minecraft.trial_chambers", "Trial Chambers");
    }
}
