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
        add(
                TranslateHelper.message("struct_marker.saved"),
                "已保存 %s 的坐标 %s, %s, %s；找到结构：%s 个");
        add(TranslateHelper.tooltip("struct_marker.dimension"), "维度：%s");
        add(TranslateHelper.tooltip("struct_marker.structure"), "结构：%s");
        add(TranslateHelper.tooltip("struct_marker.no_structure"), "结构：无");
    }
}
