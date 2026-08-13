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
        add(TranslateHelper.block("tier_1_mythic_miner"), "一级神话采掘器");
        add(TranslateHelper.container("tier_1_mythic_miner"), "一级神话采掘器");
        add(TranslateHelper.message("struct_marker.saved"), "已保存 %s 的坐标 %s, %s, %s；找到结构：%s 个");
        add(TranslateHelper.tooltip("struct_marker.dimension"), "维度：%s");
        add(TranslateHelper.tooltip("struct_marker.dimension_value"), "维度价值：%s");
        add(TranslateHelper.tooltip("struct_marker.structure_value"), "结构价值：%s");
        add(TranslateHelper.tooltip("struct_marker.analysis_status"), "战利品分析：%s");
        add(TranslateHelper.tooltip("struct_marker.legacy"), "旧版价值已隐藏，请重新标记结构");
        add(TranslateHelper.tooltip("struct_marker.structure"), "结构：%s");
        add(TranslateHelper.tooltip("struct_marker.no_structure"), "结构：无");
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
        add("jade.dimension_tech.remaining", "剩余：%s 秒");
        add("jade.dimension_tech.parallel", "并行：%s");
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
