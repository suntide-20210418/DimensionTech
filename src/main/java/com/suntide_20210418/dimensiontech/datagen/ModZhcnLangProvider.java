package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModZhcnLangProvider extends LanguageProvider {

    public ModZhcnLangProvider(PackOutput pOutput) {
        super(pOutput, DimensionTechMod.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add(TranslateHelper.itemGroup("tab"), "维度科技");
        add(TranslateHelper.keyCategory("main"), "维度科技");
        add(TranslateHelper.key("switch"), "切换卡片模式");
        add(TranslateHelper.item("struct_marker"), "结构标记器");
        add(TranslateHelper.item("enchantment_mark"), "附魔印记");
        add(TranslateHelper.item("dimension_deconstruction_core"), "维度解构核心");
        add(TranslateHelper.block("tier_1_mythic_miner"), "一级神话采掘器");
        add(TranslateHelper.container("tier_1_mythic_miner"), "一级神话采掘器");
        for (int tier = 2; tier <= 6; tier++) {
            add("block.dimension_tech.tier_" + tier + "_mythic_miner", tier + "级神话采掘器");
            add("container.dimension_tech.tier_" + tier + "_mythic_miner", tier + "级神话采掘器");
        }
        add("block.dimension_tech.mythic_miner_casing", "采掘器外壳");
        add("block.dimension_tech.mythic_miner_structure", "采掘器结构方块");
        add("block.dimension_tech.mythic_miner_upgrade_parallel", "一级并行升级方块");
        add("block.dimension_tech.mythic_miner_upgrade_luck", "一级幸运升级方块");
        add("block.dimension_tech.mythic_miner_upgrade_energy", "一级能量升级方块");
        add("block.dimension_tech.mythic_miner_upgrade_efficiency", "一级效率升级方块");
        add("block.dimension_tech.mythic_miner_upgrade_aggregate", "一级聚合升级方块");
        for (int tier = 2; tier <= 6; tier++) {
            add(
                    "block.dimension_tech.mythic_miner_upgrade_parallel_tier_" + tier,
                    tier + "级并行升级方块");
            add("block.dimension_tech.mythic_miner_upgrade_luck_tier_" + tier, tier + "级幸运升级方块");
            add("block.dimension_tech.mythic_miner_upgrade_energy_tier_" + tier, tier + "级能量升级方块");
            add(
                    "block.dimension_tech.mythic_miner_upgrade_efficiency_tier_" + tier,
                    tier + "级效率升级方块");
            add(
                    "block.dimension_tech.mythic_miner_upgrade_aggregate_tier_" + tier,
                    tier + "级聚合升级方块");
        }
        add("block.dimension_tech.mythic_miner_upgrade_none", "无升级方块");
        for (int tier = 1; tier <= 6; tier++)
            add("block.dimension_tech.dimension_focus_tier_" + tier, "维度聚焦方块 " + tier);
        add("screen.dimension_tech.mythic_miner.place_structure", "一键搭建");
        add("screen.dimension_tech.mythic_miner.place_structure_short", "一键搭建");
        add("screen.dimension_tech.mythic_miner.overview.working", "工作中：%s / %s");
        add("screen.dimension_tech.mythic_miner.overview.active", "工作槽位");
        add("screen.dimension_tech.mythic_miner.overview.total_parallel", "总并行");
        add("screen.dimension_tech.mythic_miner.equipment_dismantling", "装备分解");
        add("screen.dimension_tech.mythic_miner.enabled", "已开启");
        add("screen.dimension_tech.mythic_miner.disabled", "已关闭");
        add("screen.dimension_tech.mythic_miner.structure_incomplete", "结构不完整");
        add("message.dimension_tech.mythic_miner.projection_on", "已显示多方块结构投影");
        add("message.dimension_tech.mythic_miner.projection_off", "已隐藏多方块结构投影");
        add("tooltip.dimension_tech.mythic_miner.base_parallel", "基础并行：%s");
        add("tooltip.dimension_tech.mythic_miner.efficiency", "效率：%s");
        add("tooltip.dimension_tech.mythic_miner.luck", "幸运：%s");
        add("tooltip.dimension_tech.mythic_miner.energy_capacity", "能量储存：%s FE");
        add("tooltip.dimension_tech.mythic_miner.energy_consumption", "能量消耗：%s FE/t");
        add("tooltip.dimension_tech.mythic_miner.marker_slots", "标记器槽位：%s");
        add("tooltip.dimension_tech.mythic_miner.hold_shift", "按住 Shift 查看多方块搭建材料");
        add("tooltip.dimension_tech.mythic_miner.materials", "多方块搭建材料");
        add("tooltip.dimension_tech.mythic_miner.material.casing", "采掘器外壳 x%s");
        add("tooltip.dimension_tech.mythic_miner.material.structure", "采掘器结构方块 x%s");
        add("tooltip.dimension_tech.mythic_miner.material.focus", "%s 级维度聚焦方块 x%s");
        add("tooltip.dimension_tech.mythic_miner.material.upgrade", "任意升级方块 x%s");
        add("tooltip.dimension_tech.mythic_miner.upgrade.efficiency", "效率提升：+%s%%");
        add("tooltip.dimension_tech.mythic_miner.upgrade.energy_capacity", "储能提升：+%s%%");
        add(
                "tooltip.dimension_tech.mythic_miner.upgrade.energy_consumption.multiplicative",
                "能耗降低（乘算）：-%s%%");
        add(
                "tooltip.dimension_tech.mythic_miner.upgrade.energy_consumption.additive",
                "能耗降低（加算）：-%s%%");
        add("tooltip.dimension_tech.mythic_miner.upgrade.parallel", "并行提升：+%s%%");
        add("tooltip.dimension_tech.mythic_miner.upgrade.luck", "幸运提升：+%s%%");
        add("tooltip.dimension_tech.mythic_miner.upgrade.none", "无属性加成");
        add("screen.dimension_tech.struct_marker.title", "结构标记器分析");
        add("screen.dimension_tech.struct_marker.subtitle", "战利品期望与价值概览");
        add("screen.dimension_tech.struct_marker.dimension", "维度：%s");
        add("screen.dimension_tech.struct_marker.structure", "结构：%s");
        add("screen.dimension_tech.struct_marker.dimension_value", "维度价值");
        add("screen.dimension_tech.struct_marker.structure_value", "结构价值");
        add("screen.dimension_tech.struct_marker.structures", "已索引 %s 个结构");
        add("screen.dimension_tech.struct_marker.item", "物品");
        add("screen.dimension_tech.struct_marker.expected", "期望数量");
        add("screen.dimension_tech.struct_marker.multiplier_header", "倍率");
        add("screen.dimension_tech.struct_marker.multiplier", "倍率：%s");
        add("screen.dimension_tech.struct_marker.no_items", "没有可用的期望物品");
        add("screen.dimension_tech.struct_marker.select", "选择结构");
        add("screen.dimension_tech.struct_marker.clear", "清除结构");
        add("screen.dimension_tech.struct_marker.select_prompt", "当前位置属于多个结构，请选择");
        add("screen.dimension_tech.struct_marker.selection.selected", "已选择结构");
        add("screen.dimension_tech.struct_marker.selection.empty", "未选择结构");
        add("message.dimension_tech.struct_marker.no_structure_here", "当前位置不属于任何结构");
        add("message.dimension_tech.struct_marker.selection_invalid", "结构候选已变化，请重新选择");
        add("screen.dimension_tech.mythic_miner.control", "采掘控制台");
        add("screen.dimension_tech.mythic_miner.markers", "结构标记器");
        add("screen.dimension_tech.mythic_miner.slots", "%s 个槽位");
        add("screen.dimension_tech.mythic_miner.target", "当前目标");
        add("screen.dimension_tech.mythic_miner.dimension", "维度：%s");
        add("screen.dimension_tech.mythic_miner.energy_tooltip", "能量状态");
        add("screen.dimension_tech.mythic_miner.energy_value", "储备：%s / %s FE");
        add("screen.dimension_tech.mythic_miner.energy_consumption", "消耗：%s FE/t");
        add("screen.dimension_tech.mythic_miner.energy", "能量储备");
        add("screen.dimension_tech.mythic_miner.parallel", "并行数量");
        add("screen.dimension_tech.mythic_miner.attribute.efficiency", "效率");
        add("screen.dimension_tech.mythic_miner.attribute.capacity", "储能");
        add("screen.dimension_tech.mythic_miner.attribute.consumption", "能耗");
        add("screen.dimension_tech.mythic_miner.attribute.parallel", "并行");
        add("screen.dimension_tech.mythic_miner.attribute.luck", "幸运值");
        add("screen.dimension_tech.mythic_miner.attribute.external_acceleration", "外部等效加速");
        add("screen.dimension_tech.mythic_miner.tab.work", "工作");
        add("screen.dimension_tech.mythic_miner.tab.info", "信息");
        add("screen.dimension_tech.mythic_miner.tab.attributes", "属性");
        add("screen.dimension_tech.mythic_miner.info.slots", "标记槽位");
        add("screen.dimension_tech.mythic_miner.info.structure", "结构 / 周期：%s tick");
        add("screen.dimension_tech.mythic_miner.info.natural", "自然 tick：%s");
        add("screen.dimension_tech.mythic_miner.info.actual", "实际 tick：%s");
        add("screen.dimension_tech.mythic_miner.info.external", "外部等效加速：%sx");
        add("screen.dimension_tech.mythic_miner.info.total_parallel", "总并行：%s");
        add("screen.dimension_tech.mythic_miner.info.section.marker", "标记器属性");
        add("screen.dimension_tech.mythic_miner.info.section.work", "工作状况");
        add("screen.dimension_tech.mythic_miner.info.section.products", "产物信息和操作");
        add("screen.dimension_tech.mythic_miner.marker_progress_toggle", "点击%s");
        add("screen.dimension_tech.mythic_miner.overview", "机器总览");
        add("screen.dimension_tech.mythic_miner.overview.progress", "%s / %s tick");
        add("screen.dimension_tech.mythic_miner.waiting_for_natural_window", "等待自然 tick 窗口（400 tick）");
        add("screen.dimension_tech.mythic_miner.attribute.upgrades", "升级方块");
        add("screen.dimension_tech.mythic_miner.attribute.installed", "当前安装的升级");
        add("screen.dimension_tech.mythic_miner.attribute.upgrade_count", "×%s");
        add("screen.dimension_tech.mythic_miner.attribute.total_short", "总计 %s");
        add("screen.dimension_tech.mythic_miner.attribute.summary_counts", "专精 %s | 聚合 %s");
        add("screen.dimension_tech.mythic_miner.attribute.efficiency_value", "有效效率：%s");
        add("screen.dimension_tech.mythic_miner.attribute.capacity_value", "有效储能：%s FE");
        add("screen.dimension_tech.mythic_miner.attribute.consumption_value", "有效能耗：%s FE/t");
        add("screen.dimension_tech.mythic_miner.attribute.parallel_value", "有效并行：%s");
        add("screen.dimension_tech.mythic_miner.attribute.luck_value", "有效幸运值：%s");
        add("screen.dimension_tech.mythic_miner.attribute.bonus", "最终加成：+%s%%");
        add("screen.dimension_tech.mythic_miner.attribute.reduction", "最终降低：%s%%");
        add("screen.dimension_tech.mythic_miner.attribute.total_count", "升级方块总数：%s");
        add(
                "screen.dimension_tech.mythic_miner.attribute.upgrade_breakdown",
                "专精升级：%s，聚合升级：%s");
        add("screen.dimension_tech.mythic_miner.output.me_network", "ME 网络");
        add("screen.dimension_tech.mythic_miner.output.item_handler", "物品容器");
        add("screen.dimension_tech.mythic_miner.output.none", "未连接");
        add("screen.dimension_tech.mythic_miner.inventory", "玩家背包");
        add("screen.dimension_tech.mythic_miner.output_face", "输出面");
        add("screen.dimension_tech.mythic_miner.output_mode", "输出状态");
        add("screen.dimension_tech.mythic_miner.redstone", "红石控制");
        add("screen.dimension_tech.mythic_miner.redstone_mode", "红石：%s");
        add("screen.dimension_tech.mythic_miner.redstone.always", "总是工作");
        add("screen.dimension_tech.mythic_miner.redstone.signal", "有信号时工作");
        add("screen.dimension_tech.mythic_miner.redstone.no_signal", "无信号时工作");
        add("screen.dimension_tech.mythic_miner.redstone.never", "从不工作");
        add("screen.dimension_tech.mythic_miner.marker_progress", "进度：%s / %s tick");
        add("screen.dimension_tech.mythic_miner.marker_info.select", "点击进度条选择标记器");
        add("screen.dimension_tech.mythic_miner.marker_info.structure", "槽位 %s：%s");
        add("screen.dimension_tech.mythic_miner.marker_info.dimension", "维度：%s");
        add("screen.dimension_tech.mythic_miner.marker_info.position", "坐标：%s, %s, %s");
        add("screen.dimension_tech.mythic_miner.marker_info.dimension_value", "维度价值：%s");
        add("screen.dimension_tech.mythic_miner.marker_info.structure_value", "结构价值：%s");
        add("screen.dimension_tech.mythic_miner.marker_info.expected_items", "物品期望");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.loading",
                "正在计算机器有效期望...");
        add("screen.dimension_tech.mythic_miner.marker_info.no_items", "无可用物品期望");
        add("screen.dimension_tech.mythic_miner.slot.enable", "启用当前槽位");
        add("screen.dimension_tech.mythic_miner.slot.disable", "停用当前槽位");
        add("screen.dimension_tech.mythic_miner.marker_info.parallel", "总并行：%s  [%s]");
        add("screen.dimension_tech.mythic_miner.marker_info.parallel.expand", "点击展开");
        add("screen.dimension_tech.mythic_miner.marker_info.parallel.collapse", "点击收起");
        add("screen.dimension_tech.mythic_miner.marker_info.parallel.base", "基础并行：%s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.parallel.efficiency",
                "额外效率并行：%s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.parallel.external",
                "外部加速并行：%s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.natural_ticks",
                "本周期自然 tick：%s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.actual_ticks",
                "实际 tick：%s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.actual_parallel",
                "实时外部加速并行：%s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.previous_cycle_ticks",
                "上一周期实际 tick：%s");
        add(
                "screen.dimension_tech.mythic_miner.marker_info.previous_cycle_parallel",
                "上一周期外部加速并行：%s");
        add(
                "screen.dimension_tech.mythic_miner.marker_parallel",
                "并行：%s（%s 基础 + %s 额外效率 + %s 外部加速）");
        add("screen.dimension_tech.mythic_miner.face.selected", "当前面：%s");
        add("screen.dimension_tech.mythic_miner.output_faces_enabled", "已开启 %s / 6 个面");
        add("screen.dimension_tech.mythic_miner.face.tooltip", "面：%s");
        add("screen.dimension_tech.mythic_miner.face.adjacent", "相邻机器：%s");
        add("screen.dimension_tech.mythic_miner.face.state", "输出开关：%s");
        add("screen.dimension_tech.mythic_miner.face.empty", "无方块");
        add("screen.dimension_tech.mythic_miner.face.north", "正");
        add("screen.dimension_tech.mythic_miner.face.south", "后");
        add("screen.dimension_tech.mythic_miner.face.east", "东");
        add("screen.dimension_tech.mythic_miner.face.west", "西");
        add("screen.dimension_tech.mythic_miner.face.up", "上");
        add("screen.dimension_tech.mythic_miner.face.down", "下");
        add(TranslateHelper.message("struct_marker.saved"), "已保存 %s 的坐标 %s, %s, %s；找到结构：%s 个");
        add(TranslateHelper.tooltip("struct_marker.dimension"), "维度：%s");
        add(TranslateHelper.tooltip("struct_marker.dimension_value"), "维度价值：%s");
        add(TranslateHelper.tooltip("struct_marker.structure_value"), "结构价值：%s");
        add(TranslateHelper.tooltip("struct_marker.analysis_status"), "战利品分析：%s");
        add("screen.dimension_tech.struct_marker.calculation_method", "计算方式：%s");
        add("screen.dimension_tech.struct_marker.analysis_status.exact", "精确");
        add("screen.dimension_tech.struct_marker.analysis_status.approximate", "近似");
        add("screen.dimension_tech.struct_marker.analysis_status.unsupported", "不支持");
        add("screen.dimension_tech.struct_marker.analysis_status.legacy", "旧版");
        add(TranslateHelper.tooltip("struct_marker.legacy"), "旧版价值已隐藏，请重新标记结构");
        add(TranslateHelper.tooltip("struct_marker.structure"), "结构：%s");
        add(TranslateHelper.tooltip("struct_marker.no_structure"), "结构：无");
        add(TranslateHelper.tooltip("enchantment_mark.enchantment"), "附魔：%s");
        add(TranslateHelper.tooltip("enchantment_mark.unbound"), "未绑定附魔");
        add("config.jade.plugin_dimension_tech.mythic_miner_status", "神话采掘器状态");
        add("jade.dimension_tech.status.idle", "空闲");
        add("jade.dimension_tech.status.running", "运行中");
        add("jade.dimension_tech.status.blocked", "输出阻塞");
        add("jade.dimension_tech.status", "状态：%s");
        add("jade.dimension_tech.structure", "结构：%s");
        add("jade.dimension_tech.structure.none", "无");
        add("jade.dimension_tech.structure.minecraft.village_plains", "平原村庄");
        add("jade.dimension_tech.structure.minecraft.village_desert", "沙漠村庄");
        add("jade.dimension_tech.structure.minecraft.village_savanna", "热带草原村庄");
        add("jade.dimension_tech.structure.minecraft.village_snowy", "雪原村庄");
        add("jade.dimension_tech.structure.minecraft.village_taiga", "针叶林村庄");
        add("jade.dimension_tech.progress", "进度：%s");
        add("jade.dimension_tech.progress_value", "进度：%s%%");
        add("jade.dimension_tech.slot", "槽位 %s：%s");
        add("jade.dimension_tech.slot_progress", "进度：%s");
        add("jade.dimension_tech.slot_progress_value", "%s / %s tick");
        add("jade.dimension_tech.slot_progress_percent", "%s%%");
        add("jade.dimension_tech.slot_parallel", "并行：%s");
        add("jade.dimension_tech.base_parameters", "基础参数：%s");
        add("jade.dimension_tech.parameters", "矿机");
        add("jade.dimension_tech.efficiency", "效率：%s");
        add("jade.dimension_tech.capacity", "储能：%s");
        add("jade.dimension_tech.base_consumption", "基础能耗：%s");
        add("jade.dimension_tech.base_parallel", "基础并行：%s");
        add("jade.dimension_tech.luck", "基础幸运：%s");
        add("jade.dimension_tech.slot_usage", "槽位：%s/%s");
        add("jade.dimension_tech.remaining", "剩余：%s 秒");
        add("jade.dimension_tech.parallel", "并行：%s");
        add("jade.dimension_tech.external_parallel", "外部加速并行：%s");
        add("jade.dimension_tech.external_equivalent_acceleration", "外部等效加速：%sx");
        add("jade.dimension_tech.waiting_for_natural_window", "等待自然 tick 窗口（400 tick）");
        add("jade.dimension_tech.actual_ticks", "本周期实际机器 tick：%s");
        add("jade.dimension_tech.previous_ticks", "上一周期实际机器 tick：%s");
        add("jade.dimension_tech.previous_parallel", "上一周期外部加速并行：%s");
        add("jade.dimension_tech.output", "输出：%s");
        add("jade.dimension_tech.output.me_network", "ME 网络");
        add("jade.dimension_tech.output.item_handler", "物品容器");
        add("jade.dimension_tech.output.none", "无输出");
        add("jade.dimension_tech.pending", "待输出：%s 个物品");
        add("jade.dimension_tech.reason", "原因：%s");
        add("jade.dimension_tech.reason.me_full", "网络存储空间不足");
        add("jade.dimension_tech.reason.inventory_full", "物品容器已满");
        add("jade.dimension_tech.reason.no_target", "没有可用的输出目标");
        add("jade.dimension_tech.energy", "能量：%s");
        add("jade.dimension_tech.consumption", "耗能：%s");
        add("jade.dimension_tech.energy_value", "%s/%s FE");
        add("jade.dimension_tech.energy_consumption_value", "%s FE/t");
        add("jade.dimension_tech.seconds", "%s");
        add("jade.dimension_tech.items", "%s");
    }
}
