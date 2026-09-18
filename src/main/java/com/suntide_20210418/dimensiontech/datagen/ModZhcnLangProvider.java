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
        addVanillaRegistryNames();
        add(TranslateHelper.itemGroup("tab"), "维度科技");
        add(TranslateHelper.keyCategory("main"), "维度科技");
        add(TranslateHelper.key("switch"), "切换卡片模式");
        add(TranslateHelper.item("structure_marker"), "结构标记器");
        add(TranslateHelper.item("enchantment_mark"), "附魔印记");
        add(TranslateHelper.item("dimension_deconstruction_core"), "维度解构核心");
        for (int tier = 1; tier <= 6; tier++) {
            add(TranslateHelper.item("dimension_fragment_tier_" + tier), tier + "级维度碎片");
            add(TranslateHelper.item("mining_token_tier_" + tier), tier + "级采掘代币");
        }
        add(TranslateHelper.item("data_integrator"), "数据整合器");
        add(TranslateHelper.item("structure_interpreter"), "结构阐释器");
        add("block.dimension_tech.structure_data_operator", "结构数据操作仪");
        add("container.dimension_tech.structure_data_operator", "结构数据操作仪");
        add("block.dimension_tech.structure_reactor", "结构反应堆");
        add("container.dimension_tech.structure_reactor", "结构反应堆");
        add("screen.dimension_tech.structure_reactor.title", "结构反应堆");
        add("screen.dimension_tech.structure_reactor.inventory_label", "物品栏");
        add("screen.dimension_tech.structure_reactor.status.idle", "空闲");
        add("screen.dimension_tech.structure_reactor.status.running", "运行中");
        add("screen.dimension_tech.structure_reactor.status.ready", "等待提交");
        add("screen.dimension_tech.structure_reactor.status_line.current", "状态：%s  %s");
        add("screen.dimension_tech.structure_reactor.status_line.progress_value", "%s/%s tick");
        add("screen.dimension_tech.structure_reactor.status_line.refining_value", "耗时%s tick");
        add("screen.dimension_tech.structure_reactor.status_line.sequence", "配方步骤：");
        add("screen.dimension_tech.structure_reactor.status_line.progress", "状态进度：%s/%s tick");
        add(
                "screen.dimension_tech.structure_reactor.status_line.refining_progress",
                "状态进度：当前耗时 %s tick");
        add("screen.dimension_tech.structure_reactor.status_line.needs", "需要：%s");
        add(
                "screen.dimension_tech.structure_reactor.status_line.changes",
                "变化：%s产出 %s消耗，耗时-%s/+%s tick，额外递归%s");
        add("screen.dimension_tech.structure_reactor.status_line.previous", "上一步骤状态：%s");
        add("screen.dimension_tech.structure_reactor.status_line.previous.reward", "%s：回卷奖励（%s）");
        add("screen.dimension_tech.structure_reactor.status_line.previous.normal", "%s：正常回卷");
        add("screen.dimension_tech.structure_reactor.status_line.previous.penalty", "%s：惩罚（%s）");
        add(
                "screen.dimension_tech.structure_reactor.status_line.previous.reward.branch",
                "耗时减少%s tick");
        add(
                "screen.dimension_tech.structure_reactor.status_line.previous.reward.recurse",
                "消耗减少%s%%");
        add(
                "screen.dimension_tech.structure_reactor.status_line.previous.reward.converge",
                "产出增加%s%%");
        add(
                "screen.dimension_tech.structure_reactor.status_line.previous.reward.stabilize",
                "未结算奖励翻倍");
        add(
                "screen.dimension_tech.structure_reactor.status_line.previous.penalty.phase_idle",
                "相位空转，耗时增加%s tick");
        add(
                "screen.dimension_tech.structure_reactor.status_line.previous.penalty.branch",
                "分支冲突，耗时增加%s tick");
        add(
                "screen.dimension_tech.structure_reactor.status_line.previous.penalty.recurse",
                "递归溢出，消耗增加%s%%");
        add(
                "screen.dimension_tech.structure_reactor.status_line.previous.penalty.converge",
                "提前收敛，产出减少%s%%/层");
        add(
                "screen.dimension_tech.structure_reactor.status_line.previous.penalty.stabilize",
                "提前稳定，额外碎片+%s");
        add("screen.dimension_tech.structure_reactor.status_line.previous.none", "无");
        add("screen.dimension_tech.structure_reactor.status_line.no_recipe", "暂无配方");
        add("screen.dimension_tech.structure_reactor.status_line.no_requirement", "无");
        add("screen.dimension_tech.structure_reactor.stage.branch", "分支");
        add("screen.dimension_tech.structure_reactor.stage.recurse", "递归");
        add("screen.dimension_tech.structure_reactor.stage.converge", "收敛");
        add("screen.dimension_tech.structure_reactor.stage.stabilize", "稳定");
        add("screen.dimension_tech.structure_reactor.input", "输入精华");
        add("screen.dimension_tech.structure_reactor.output", "输出精华");
        add("screen.dimension_tech.structure_reactor.ritual", "仪式流程");
        add("screen.dimension_tech.structure_reactor.current", "当前：%s %s/%s tick");
        add("screen.dimension_tech.structure_reactor.waiting", "等待输入流体 / 碎片");
        add("screen.dimension_tech.structure_reactor.fragments", "碎片槽");
        add("screen.dimension_tech.structure_reactor.operation", "操作槽");
        add("screen.dimension_tech.structure_reactor.reward", "奖励窗口 %s-%s");
        add("screen.dimension_tech.structure_reactor.inventory", "玩家背包");
        add("screen.dimension_tech.structure_reactor.readout.idle", "尚无进行中的仪式");
        add("screen.dimension_tech.structure_reactor.readout.waiting", "等待正确的操作物品");
        add("screen.dimension_tech.structure_reactor.event.rewarded", "奖励触发：%s");
        add("screen.dimension_tech.structure_reactor.event.correct", "操作正确（未在奖励窗口内）");
        add("screen.dimension_tech.structure_reactor.event.phase_idle", "错误操作或超时：%s");
        add("screen.dimension_tech.structure_reactor.event.branch_conflict", "分支冲突：%s");
        add("screen.dimension_tech.structure_reactor.event.early_converge", "提前收敛：%s");
        add("screen.dimension_tech.structure_reactor.event.recursion_overflow", "递归溢出：%s");
        add("screen.dimension_tech.structure_reactor.event.stabilize_failure", "提前提交稳定：%s");
        add("screen.dimension_tech.structure_reactor.modifier.time.reward", "时间 −%s");
        add("screen.dimension_tech.structure_reactor.modifier.time.penalty", "时间 +%s");
        add("screen.dimension_tech.structure_reactor.modifier.fluid.reward", "流体 −%s%%");
        add("screen.dimension_tech.structure_reactor.modifier.fluid.penalty", "流体 +%s%%");
        add("screen.dimension_tech.structure_reactor.modifier.output.reward", "产出 +%s%%");
        add("screen.dimension_tech.structure_reactor.modifier.output.penalty", "产出 −%s%%");
        add("screen.dimension_tech.structure_reactor.modifier.fragments", "碎片 +%s");
        add("screen.dimension_tech.structure_reactor.modifier.time.mixed", "时间 −%s/+%s");
        add("screen.dimension_tech.structure_reactor.modifier.fluid.mixed", "流体 −%s%%/+%s%%");
        add("screen.dimension_tech.structure_reactor.modifier.output.mixed", "产出 +%s%%/−%s%%");
        add("screen.dimension_tech.structure_reactor.result", "预计结算：%s tick · %s mB → %s mB");
        add("screen.dimension_tech.structure_reactor.refining", "炼制中 %s/%s tick");
        add("screen.dimension_tech.structure_reactor.refined", "炼制完成 %s tick");
        add("screen.dimension_tech.structure_reactor.refining_phase", "所有操作已完成，正在炼制");
        add("screen.dimension_tech.structure_reactor.status.refining", "炼制中");
        add("screen.dimension_tech.structure_reactor.control.fluid_faces", "六面流体配置");
        add("screen.dimension_tech.structure_reactor.face_config", "流体面配置");
        add("screen.dimension_tech.structure_reactor.control.input_lock", "锁定输入流体");
        add("screen.dimension_tech.structure_reactor.control.auto_pull", "自动拉取");
        add("screen.dimension_tech.structure_reactor.control.auto_push", "自动弹出");
        add("screen.dimension_tech.structure_reactor.control.me_network", "ME 网络");
        add("screen.dimension_tech.structure_reactor.control.enabled", "已启用");
        add("screen.dimension_tech.structure_reactor.control.disabled", "已禁用");
        add("screen.dimension_tech.structure_reactor.fluid_lock.locked", "输入流体：已锁定");
        add("screen.dimension_tech.structure_reactor.fluid_lock.unlocked", "输入流体：未锁定");
        add("screen.dimension_tech.structure_reactor.fluid_lock.click_to_toggle", "点击输入槽切换锁定");
        add("screen.dimension_tech.structure_reactor.fluid_clear_hint", "Shift+左键：清空该槽");
        add("screen.dimension_tech.structure_reactor.fluid_faces", "坩埚流体面");
        add("screen.dimension_tech.structure_reactor.fluid_face_cycle", "点击：无 → 输入 → 输出 → 输入/输出");
        add("screen.dimension_tech.structure_reactor.fluid_face_mode.disabled", "无");
        add("screen.dimension_tech.structure_reactor.fluid_face_mode.input", "输入");
        add("screen.dimension_tech.structure_reactor.fluid_face_mode.output", "输出");
        add("screen.dimension_tech.structure_reactor.fluid_face_mode.input_output", "输入/输出");
        add("screen.dimension_tech.structure_reactor.face.up", "上面");
        add("screen.dimension_tech.structure_reactor.face.down", "下面");
        add("screen.dimension_tech.structure_reactor.face.north", "北面");
        add("screen.dimension_tech.structure_reactor.face.south", "南面");
        add("screen.dimension_tech.structure_reactor.face.west", "西面");
        add("screen.dimension_tech.structure_reactor.face.east", "东面");
        add("screen.dimension_tech.structure_reactor.tooltip.input", "输入流体");
        add("screen.dimension_tech.structure_reactor.tooltip.output", "输出流体");
        add("screen.dimension_tech.structure_reactor.tooltip.empty", "空");
        add("screen.dimension_tech.structure_reactor.tooltip.fluid", "流体：%s");
        add("screen.dimension_tech.structure_reactor.tooltip.amount", "当前：%s / %s mB");
        add("screen.dimension_tech.structure_reactor.tooltip.required", "需要：%s mB");
        add("screen.dimension_tech.structure_reactor.tooltip.required_count", "需要数量：%s");
        add("screen.dimension_tech.structure_reactor.tooltip.expected", "预计：%s × %s mB");
        add("screen.dimension_tech.structure_reactor.tooltip.free", "剩余空间：%s mB");
        add("screen.dimension_tech.structure_reactor.tooltip.fragment", "碎片要求");
        add("screen.dimension_tech.structure_reactor.tooltip.requirement", "配方要求：%s");
        add(
                "screen.dimension_tech.structure_reactor.tooltip.fragment_custom_detail",
                "具体物品由当前配方规则决定");
        add("screen.dimension_tech.structure_reactor.tooltip.operation", "操作要求");
        add("screen.dimension_tech.structure_reactor.tooltip.operation_needs", "需要操作：%s");
        add("screen.dimension_tech.structure_reactor.tooltip.step", "当前步骤：%s/%s");
        add("screen.dimension_tech.structure_reactor.tooltip.state", "仪式状态：%s");
        add("screen.dimension_tech.structure_reactor.tooltip.current", "当前物品：%s");
        add("screen.dimension_tech.structure_reactor.tooltip.current_count", "当前数量：%s");
        add("screen.dimension_tech.structure_reactor.tooltip.needs", "需要：%s");
        add("screen.dimension_tech.structure_reactor.tooltip.custom", "自定义匹配物品");
        add("screen.dimension_tech.structure_reactor.tooltip.custom_detail", "当前步骤只接受配方规则匹配的物品");
        add("screen.dimension_tech.structure_reactor.tooltip.more", "还有 %s 种可用物品");
        add("screen.dimension_tech.structure_reactor.tooltip.timeout", "超时：%s tick");
        add("screen.dimension_tech.structure_reactor.tooltip.empty_item", "空");
        add("screen.dimension_tech.structure_reactor.tooltip.status.no_recipe", "无匹配配方");
        add("screen.dimension_tech.structure_reactor.tooltip.status.available", "满足");
        add("screen.dimension_tech.structure_reactor.tooltip.status.insufficient", "数量不足");
        add("screen.dimension_tech.structure_reactor.tooltip.status.mismatch", "类型不匹配");
        add("screen.dimension_tech.structure_reactor.tooltip.status.output_space", "输出空间不足");
        add("screen.dimension_tech.structure_reactor.tooltip.status_line", "状态：%s");
        add("jei.dimension_tech.structure_reactor.title", "结构反应堆");
        add("jei.dimension_tech.structure_reactor.io", "输入 %s mB → 输出 %s mB");
        add("jei.dimension_tech.structure_reactor.branch", "分支 %s · 目标递归 %s 层");
        add("jei.dimension_tech.structure_reactor.branch.alternate", "每次提交后 A/B 分支交替");
        add("jei.dimension_tech.structure_reactor.sequence", "仪式状态序列");
        add("jei.dimension_tech.structure_reactor.row", "%s · %s");
        add("jei.dimension_tech.structure_reactor.row.unknown", "未指定");
        add("jei.dimension_tech.structure_reactor.more", "还有 %s 个状态未显示");
        add("jei.dimension_tech.structure_reactor.step", "第 %s 步：%s");
        add("jei.dimension_tech.structure_reactor.step.requires", "需要：%s（每步消耗 1 个）");
        add("jei.dimension_tech.structure_reactor.step.requires.unknown", "需要：未指定的操作物品");
        add("jei.dimension_tech.structure_reactor.step.timeout", "超过 %s tick 未结算：时间 +%s tick");
        add("jei.dimension_tech.structure_reactor.reward.window", "奖励窗口：第 %s-%s tick 内结算");
        add("jei.dimension_tech.structure_reactor.reward.branch", "奖励：结算时间 -%s tick");
        add("jei.dimension_tech.structure_reactor.reward.recurse", "奖励：流体消耗 -%s%%，并获得 1 次额外递归推进");
        add("jei.dimension_tech.structure_reactor.reward.converge", "奖励：产出 +%s%%");
        add("jei.dimension_tech.structure_reactor.reward.stabilize", "奖励：本次未结算的奖励翻倍");
        add("jei.dimension_tech.structure_reactor.penalty.stabilize", "惩罚：提前提交稳定，碎片额外 +%s（最多 ×%s）");
        add("jei.dimension_tech.structure_reactor.penalty.converge", "惩罚：未达目标层数就收敛，产出 -%s%%/层");
        add("jei.dimension_tech.structure_reactor.penalty.recurse", "惩罚：超出目标层数提交递归，流体 +%s%%");
        add("jei.dimension_tech.structure_reactor.penalty.branch", "惩罚：提交另一分支的操作，时间 +%s tick");
        add("jei.dimension_tech.structure_reactor.range.time", "结算时间：%s-%s tick");
        add("jei.dimension_tech.structure_reactor.range.fluid", "流体消耗：%s-%s mB");
        add("jei.dimension_tech.structure_reactor.range.output", "产出：%s-%s mB");
        add("jei.dimension_tech.structure_reactor.range.fragments", "碎片：%s 个（上限 %s 个）");
        add("screen.dimension_tech.structure_operator.copy", "↓复制↓");
        add("screen.dimension_tech.structure_operator.clear", "清除");
        add("screen.dimension_tech.structure_operator.refresh", "已探索结构");
        add("screen.dimension_tech.structure_operator.all", "全部结构");
        add("screen.dimension_tech.structure_operator.slots", "目标 / 待操作 / 插件");
        add("screen.dimension_tech.structure_operator.tab.operation", "操作");
        add("screen.dimension_tech.structure_operator.tab.integrator", "数据整合器");
        add("screen.dimension_tech.structure_operator.tab.interpreter", "结构阐释器");
        add("screen.dimension_tech.structure_operator.target", "目标标记器");
        add("screen.dimension_tech.structure_operator.destination", "待操作标记器");
        add("screen.dimension_tech.structure_operator.source_status", "来源数据");
        add("screen.dimension_tech.structure_operator.destination_status", "写入目标");
        add("screen.dimension_tech.structure_operator.integrator", "数据整合器");
        add("screen.dimension_tech.structure_operator.interpreter", "结构阐释器");
        add("screen.dimension_tech.structure_operator.inventory", "玩家背包");
        add("screen.dimension_tech.structure_operator.page.integrator", "数据整合器");
        add("screen.dimension_tech.structure_operator.page.interpreter", "结构阐释器");
        add("screen.dimension_tech.structure_operator.source.explored", "来源：玩家已探索结构（%s 个）");
        add("screen.dimension_tech.structure_operator.source.all", "来源：游戏中全部结构（%s 个）");
        add("screen.dimension_tech.structure_operator.search", "搜索结构 ID");
        add("screen.dimension_tech.structure_operator.write", "写入");
        add("screen.dimension_tech.structure_operator.refresh_short", "刷新");
        add("screen.dimension_tech.structure_operator.write_short", "写入");
        add("screen.dimension_tech.structure_operator.operands", "待操作标记器：%s");
        add("screen.dimension_tech.structure_operator.empty", "没有可用结构");
        add("screen.dimension_tech.structure_operator.empty_marker", "未安装");
        add("screen.dimension_tech.structure_operator.no_data", "没有结构数据");
        add("screen.dimension_tech.structure_operator.confirm.copy", "确认复制结构数据");
        add("screen.dimension_tech.structure_operator.confirm.write", "确认写入结构数据");
        add("screen.dimension_tech.structure_operator.preview.source", "来源：%s");
        add("screen.dimension_tech.structure_operator.preview.target", "目标：%s");
        add("screen.dimension_tech.structure_operator.preview.overwrite", "目标已有数据，将被覆盖");
        add("screen.dimension_tech.structure_operator.preview.empty", "目标当前为空");
        add("screen.dimension_tech.structure_operator.cancel", "取消");
        add("screen.dimension_tech.structure_operator.confirm", "确认");
        add("screen.dimension_tech.structure_operator.status.copied", "结构数据已复制");
        add("screen.dimension_tech.structure_operator.status.written", "结构数据已写入");
        add("screen.dimension_tech.structure_operator.status.cleared", "结构数据已清除");
        add("screen.dimension_tech.structure_operator.catalogue_marker", "目录写入标记器");
        add("screen.dimension_tech.structure_operator.catalogue_analysis", "目录分析");
        add("screen.dimension_tech.structure_operator.dimension", "维度：%s");
        add("screen.dimension_tech.structure_operator.structure", "结构：%s");
        add("screen.dimension_tech.structure_operator.select_entry", "选择一个有战利品的结构");
        add("screen.dimension_tech.structure_operator.loading", "正在读取分析数据");
        add("screen.dimension_tech.structure_operator.virtual_progress", "虚拟采样：%s/%s");
        add("screen.dimension_tech.structure_operator.virtual_approximate", "近似结果（样本 %s）");
        add(TranslateHelper.block("tier_1_structure_miner"), "第1阶虚空结构资源采掘器");
        add(TranslateHelper.container("tier_1_structure_miner"), "第1阶虚空结构资源采掘器");
        for (int tier = 2; tier <= 5; tier++) {
            add(
                    "block.dimension_tech.tier_" + tier + "_structure_miner",
                    "第" + tier + "阶虚空结构资源采掘器");
            add(
                    "container.dimension_tech.tier_" + tier + "_structure_miner",
                    "第" + tier + "阶虚空结构资源采掘器");
        }
        add("block.dimension_tech.tier_6_structure_miner", "第六阶虚空结构资源采掘器");
        add("container.dimension_tech.tier_6_structure_miner", "第六阶虚空结构资源采掘器");
        add("block.dimension_tech.structure_miner_casing", "采掘器外壳");
        add("block.dimension_tech.structure_miner_structure", "采掘器结构方块");
        add("block.dimension_tech.structure_miner_upgrade_parallel", "一级并行升级方块");
        add("block.dimension_tech.structure_miner_upgrade_luck", "一级幸运升级方块");
        add("block.dimension_tech.structure_miner_upgrade_energy", "一级能量升级方块");
        add("block.dimension_tech.structure_miner_upgrade_efficiency", "一级效率升级方块");
        add("block.dimension_tech.structure_miner_upgrade_aggregate", "一级聚合升级方块");
        for (int tier = 2; tier <= 6; tier++) {
            add(
                    "block.dimension_tech.structure_miner_upgrade_parallel_tier_" + tier,
                    tier + "级并行升级方块");
            add("block.dimension_tech.structure_miner_upgrade_luck_tier_" + tier, tier + "级幸运升级方块");
            add("block.dimension_tech.structure_miner_upgrade_energy_tier_" + tier, tier + "级能量升级方块");
            add(
                    "block.dimension_tech.structure_miner_upgrade_efficiency_tier_" + tier,
                    tier + "级效率升级方块");
            add(
                    "block.dimension_tech.structure_miner_upgrade_aggregate_tier_" + tier,
                    tier + "级聚合升级方块");
        }
        for (int tier = 1; tier <= 6; tier++)
            add("block.dimension_tech.dimension_focus_tier_" + tier, "维度聚焦方块 " + tier);
        add("screen.dimension_tech.structure_miner.place_structure", "一键搭建");
        add("screen.dimension_tech.structure_miner.place_structure_short", "一键搭建");
        add("screen.dimension_tech.structure_miner.overview.working", "工作中：%s / %s");
        add("screen.dimension_tech.structure_miner.overview.active", "工作槽位");
        add("screen.dimension_tech.structure_miner.overview.total_parallel", "总并行");
        add("screen.dimension_tech.structure_miner.overview.equivalent_acceleration", "本周期等效加速");
        add("screen.dimension_tech.structure_miner.equipment_dismantling", "装备分解");
        add("screen.dimension_tech.structure_miner.equipment_dismantling_on", "装备分解：开");
        add("screen.dimension_tech.structure_miner.equipment_dismantling_off", "装备分解：关");
        add("screen.dimension_tech.structure_miner.enabled", "已开启");
        add("screen.dimension_tech.structure_miner.disabled", "已关闭");
        add("screen.dimension_tech.structure_miner.structure.complete", "结构完整");
        add("screen.dimension_tech.structure_miner.structure_incomplete", "结构不完整");
        add("screen.dimension_tech.structure_miner.status.ready", "就绪");
        add("screen.dimension_tech.structure_miner.status.waiting_structure", "等待结构");
        add("message.dimension_tech.structure_miner.projection_on", "已显示多方块结构投影");
        add("message.dimension_tech.structure_miner.projection_off", "已隐藏多方块结构投影");
        add(
                "message.dimension_tech.structure_miner.build_blocked",
                "搭建位置被 %s 个方块阻挡，请先清理");
        add("message.dimension_tech.structure_miner.build_missing", "材料不足：%s");
        add("message.dimension_tech.structure_miner.build_missing_entry", "%s x%s");
        add("tooltip.dimension_tech.structure_miner.base_parallel", "基础并行：%s");
        add("tooltip.dimension_tech.structure_miner.efficiency", "效率：%s");
        add("tooltip.dimension_tech.structure_miner.luck", "幸运：%s");
        add("tooltip.dimension_tech.structure_miner.energy_capacity", "能量储存：%s FE");
        add("tooltip.dimension_tech.structure_miner.energy_consumption", "能量消耗：%s FE/t");
        add("tooltip.dimension_tech.structure_miner.marker_slots", "标记器槽位：%s");
        add("tooltip.dimension_tech.structure_miner.hold_shift", "按住 Shift 查看多方块搭建材料");
        add("tooltip.dimension_tech.structure_miner.materials", "多方块搭建材料");
        add("tooltip.dimension_tech.structure_miner.material.casing", "采掘器外壳 x%s");
        add("tooltip.dimension_tech.structure_miner.material.structure", "采掘器结构方块 x%s");
        add("tooltip.dimension_tech.structure_miner.material.focus", "%s 级维度聚焦方块 x%s");
        add(
                "tooltip.dimension_tech.structure_miner.material.upgrade",
                "任意升级方块或采掘器外壳 x%s");
        add("tooltip.dimension_tech.structure_miner.upgrade.efficiency", "效率提升：+%s%%");
        add("tooltip.dimension_tech.structure_miner.upgrade.energy_capacity", "储能提升：+%s%%");
        add(
                "tooltip.dimension_tech.structure_miner.upgrade.energy_consumption.multiplicative",
                "能耗降低（乘算）：-%s%%");
        add(
                "tooltip.dimension_tech.structure_miner.upgrade.energy_consumption.additive",
                "能耗降低（加算）：-%s%%");
        add("tooltip.dimension_tech.structure_miner.upgrade.parallel", "并行提升：+%s%%");
        add("tooltip.dimension_tech.structure_miner.upgrade.luck", "幸运提升：+%s%%");
        add("tooltip.dimension_tech.structure_miner.upgrade.none", "无属性加成");
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
        add("screen.dimension_tech.structure_miner.control", "采掘控制台");
        add("screen.dimension_tech.structure_miner.markers", "结构标记器");
        add("screen.dimension_tech.structure_miner.slots", "%s 个槽位");
        add("screen.dimension_tech.structure_miner.target", "当前目标");
        add("screen.dimension_tech.structure_miner.dimension", "维度：%s");
        add("screen.dimension_tech.structure_miner.energy_tooltip", "能量状态");
        add("screen.dimension_tech.structure_miner.energy_value", "储备：%s / %s FE");
        add("screen.dimension_tech.structure_miner.energy_consumption", "消耗：%s FE/t");
        add("screen.dimension_tech.structure_miner.energy", "能量储备");
        add("screen.dimension_tech.structure_miner.fluid_input", "流体输入");
        add("screen.dimension_tech.structure_miner.fluid_empty", "空");
        add("screen.dimension_tech.structure_miner.fluid_amount", "%s / %s mB");
        add("screen.dimension_tech.structure_miner.fluid_required", "需求：每周期 %s mB");
        add("screen.dimension_tech.structure_miner.fluid_insufficient", "流体不足");
        add("screen.dimension_tech.structure_miner.fluid_wrong_type", "流体类型错误");
        add("screen.dimension_tech.structure_miner.fluid_blocked", "流体输入阻塞");
        add("screen.dimension_tech.structure_miner.fluid_status", "状态：正常");
        add("screen.dimension_tech.structure_miner.fluid_faces", "流体面");
        add("screen.dimension_tech.structure_miner.fluid_face_mode.disabled", "流体：禁用");
        add("screen.dimension_tech.structure_miner.fluid_face_mode.input", "流体：输入");
        add("screen.dimension_tech.structure_miner.fluid_face_mode.output", "流体：输出");
        add("screen.dimension_tech.structure_miner.auto_extract_enabled", "自动抽取流体：已开启");
        add("screen.dimension_tech.structure_miner.auto_extract_disabled", "自动抽取流体：已关闭");
        add("screen.dimension_tech.structure_miner.fluid_required_type", "仅接受此精华");
        add("fluid.dimension_tech.structure_essence", "结构精华");
        add("fluid.dimension_tech.surging_structure_essence", "澎湃结构精华");
        add("fluid.dimension_tech.recursive_essence", "递归精华");
        add("fluid.dimension_tech.surging_recursive_essence", "澎湃递归精华");
        add("fluid.dimension_tech.fractal_essence", "分形精华");
        add("item.dimension_tech.structure_essence_bucket", "结构精华桶");
        add("item.dimension_tech.surging_structure_essence_bucket", "澎湃结构精华桶");
        add("item.dimension_tech.recursive_essence_bucket", "递归精华桶");
        add("item.dimension_tech.surging_recursive_essence_bucket", "澎湃递归精华桶");
        add("item.dimension_tech.fractal_essence_bucket", "分形精华桶");
        add("screen.dimension_tech.structure_miner.parallel", "并行数量");
        add("screen.dimension_tech.structure_miner.attribute.efficiency", "效率");
        add("screen.dimension_tech.structure_miner.attribute.capacity", "储能");
        add("screen.dimension_tech.structure_miner.attribute.consumption", "能耗");
        add("screen.dimension_tech.structure_miner.attribute.parallel", "并行");
        add("screen.dimension_tech.structure_miner.attribute.luck", "幸运值");
        add("screen.dimension_tech.structure_miner.attribute.external_acceleration", "外部等效加速");
        add("screen.dimension_tech.structure_miner.tab.work", "工作");
        add("screen.dimension_tech.structure_miner.tab.info", "信息");
        add("screen.dimension_tech.structure_miner.tab.attributes", "属性");
        add("screen.dimension_tech.structure_miner.info.slots", "标记槽位");
        add("screen.dimension_tech.structure_miner.info.structure", "结构 / 周期：%s tick");
        add("screen.dimension_tech.structure_miner.info.natural", "自然 tick：%s");
        add("screen.dimension_tech.structure_miner.info.actual", "实际 tick：%s");
        add("screen.dimension_tech.structure_miner.info.external", "本周期外部等效加速：%sx");
        add("screen.dimension_tech.structure_miner.info.total_parallel", "总并行：%s");
        add("screen.dimension_tech.structure_miner.info.section.marker", "标记器属性");
        add("screen.dimension_tech.structure_miner.info.section.work", "工作状况");
        add("screen.dimension_tech.structure_miner.info.section.products", "产物信息和操作");
        add("screen.dimension_tech.structure_miner.hint.slot_toggle", "右键槽位以启用或停用该线程");
        add("screen.dimension_tech.structure_miner.expected_item.enable", "点击启用产出");
        add("screen.dimension_tech.structure_miner.expected_item.disable", "点击禁用产出");
        add("screen.dimension_tech.structure_miner.natural_progress", "自然tick：%s / %s");
        add("screen.dimension_tech.structure_miner.actual_progress", "实际tick：%s / %s（x%s）");
        add("screen.dimension_tech.structure_miner.parallel.expand_hint", "点击展开并行明细");
        add("screen.dimension_tech.structure_miner.parallel.collapse_hint", "点击收起并行明细");
        add("screen.dimension_tech.structure_miner.overview", "机器总览");
        add("screen.dimension_tech.structure_miner.overview.progress", "%s / %s tick");
        add(
                "screen.dimension_tech.structure_miner.waiting_for_natural_window",
                "等待自然 tick 窗口（400 tick）");
        add("screen.dimension_tech.structure_miner.attribute.upgrades", "升级方块");
        add("screen.dimension_tech.structure_miner.attribute.composition", "属性构成");
        add("screen.dimension_tech.structure_miner.attribute.no_upgrades", "未安装升级");
        add("screen.dimension_tech.structure_miner.attribute.installed", "当前安装的升级");
        add("screen.dimension_tech.structure_miner.attribute.upgrade_count", "×%s");
        add("screen.dimension_tech.structure_miner.attribute.total_short", "总计 %s");
        add("screen.dimension_tech.structure_miner.attribute.summary_counts", "专精 %s | 聚合 %s");
        add("screen.dimension_tech.structure_miner.attribute.efficiency_value", "有效效率：%s");
        add("screen.dimension_tech.structure_miner.attribute.capacity_value", "有效储能：%s FE");
        add("screen.dimension_tech.structure_miner.attribute.consumption_value", "有效能耗：%s FE/t");
        add("screen.dimension_tech.structure_miner.attribute.parallel_value", "有效并行：%s");
        add("screen.dimension_tech.structure_miner.attribute.luck_value", "有效幸运值：%s");
        add("screen.dimension_tech.structure_miner.attribute.bonus", "最终加成：+%s%%");
        add("screen.dimension_tech.structure_miner.attribute.reduction", "最终降低：%s%%");
        add("screen.dimension_tech.structure_miner.attribute.total_count", "升级方块总数：%s");
        add("screen.dimension_tech.structure_miner.attribute.upgrade_breakdown", "专精升级：%s，聚合升级：%s");
        add("screen.dimension_tech.structure_miner.output.me_network", "ME 网络");
        add("screen.dimension_tech.structure_miner.output.item_handler", "物品容器");
        add("screen.dimension_tech.structure_miner.output.none", "未连接");
        add("screen.dimension_tech.structure_miner.output.blocked", "输出堵塞：%s 件无法弹出");
        add("screen.dimension_tech.structure_miner.inventory", "玩家背包");
        add("screen.dimension_tech.structure_miner.output_face", "输出面");
        add("screen.dimension_tech.structure_miner.output_mode", "输出状态");
        add("screen.dimension_tech.structure_miner.redstone", "红石控制");
        add("screen.dimension_tech.structure_miner.redstone_mode", "红石：%s");
        add("screen.dimension_tech.structure_miner.redstone_control_on", "红石控制：开");
        add("screen.dimension_tech.structure_miner.redstone_control_off", "红石控制：关");
        add("screen.dimension_tech.structure_miner.redstone.always", "总是工作");
        add("screen.dimension_tech.structure_miner.redstone.signal", "有信号时工作");
        add("screen.dimension_tech.structure_miner.redstone.no_signal", "无信号时工作");
        add("screen.dimension_tech.structure_miner.redstone.never", "从不工作");
        add("screen.dimension_tech.structure_miner.work.base_parallel", "基础并行");
        add("screen.dimension_tech.structure_miner.marker.no_plan", "尚无工作计划：结构分析未完成或失败");
        add("screen.dimension_tech.structure_miner.marker_progress", "进度：%s / %s tick");
        add("screen.dimension_tech.structure_miner.marker_info.select", "点击上方槽位选择标记器");
        add("screen.dimension_tech.structure_miner.marker_info.unconfigured", "未配置结构标记器");
        add("screen.dimension_tech.structure_miner.marker_info.structure", "槽位 %s：%s");
        add("screen.dimension_tech.structure_miner.marker_info.dimension", "维度：%s");
        add("screen.dimension_tech.structure_miner.marker_info.parallel_status", "并行：%s / %s");
        add("screen.dimension_tech.structure_miner.marker_info.dimension_value", "维度价值：%s");
        add("screen.dimension_tech.structure_miner.marker_info.structure_value", "结构价值：%s");
        add("screen.dimension_tech.structure_miner.marker_info.expected_items", "物品期望");
        add("screen.dimension_tech.structure_miner.marker_info.loading", "正在计算机器有效期望...");
        add("screen.dimension_tech.structure_miner.marker_info.no_items", "无可用物品期望");
        add("screen.dimension_tech.structure_miner.slot.enable", "启用当前槽位");
        add("screen.dimension_tech.structure_miner.slot.enable_right", "右键启用该线程");
        add("screen.dimension_tech.structure_miner.slot.disable_right", "右键停用该线程");
        add("screen.dimension_tech.structure_miner.slot.disable", "停用当前槽位");
        add("screen.dimension_tech.structure_miner.marker_info.parallel", "总并行：%s  [%s]");
        add("screen.dimension_tech.structure_miner.marker_info.parallel.expand", "点击展开");
        add("screen.dimension_tech.structure_miner.marker_info.parallel.collapse", "点击收起");
        add("screen.dimension_tech.structure_miner.marker_info.parallel.base", "基础并行：%s");
        add("screen.dimension_tech.structure_miner.marker_info.parallel.efficiency", "额外效率并行：%s");
        add("screen.dimension_tech.structure_miner.marker_info.parallel.external", "外部加速并行：%s");
        add("screen.dimension_tech.structure_miner.marker_info.natural_ticks", "本周期自然 tick：%s");
        add("screen.dimension_tech.structure_miner.marker_info.actual_ticks", "实际 tick：%s");
        add("screen.dimension_tech.structure_miner.marker_info.actual_parallel", "实时外部加速并行：%s");
        add(
                "screen.dimension_tech.structure_miner.marker_info.previous_cycle_ticks",
                "上一周期实际 tick：%s");
        add(
                "screen.dimension_tech.structure_miner.marker_info.previous_cycle_parallel",
                "上一周期外部加速并行：%s");
        add(
                "screen.dimension_tech.structure_miner.marker_parallel",
                "并行：%s（%s 基础 + %s 额外效率 + %s 外部加速）");
        add("screen.dimension_tech.structure_miner.face.selected", "当前面：%s");
        add("screen.dimension_tech.structure_miner.output_faces_enabled", "已开启 %s / 6 个面");
        add("screen.dimension_tech.structure_miner.face.tooltip", "面：%s");
        add("screen.dimension_tech.structure_miner.face.adjacent", "相邻机器：%s");
        add("screen.dimension_tech.structure_miner.face.state", "输出开关：%s");
        add("screen.dimension_tech.structure_miner.face.empty", "无方块");
        add("screen.dimension_tech.structure_miner.face.north", "正");
        add("screen.dimension_tech.structure_miner.face.south", "后");
        add("screen.dimension_tech.structure_miner.face.east", "东");
        add("screen.dimension_tech.structure_miner.face.west", "西");
        add("screen.dimension_tech.structure_miner.face.up", "上");
        add("screen.dimension_tech.structure_miner.face.down", "下");
        add("screen.dimension_tech.structure_miner.face.status.fluid_input", "拥有流体输入");
        add("screen.dimension_tech.structure_miner.face.status.auto_output", "自动输出");
        add("screen.dimension_tech.structure_miner.face.status.both", "流体输入/自动输出");
        add("screen.dimension_tech.structure_miner.face.status.disabled", "禁用");
        add("screen.dimension_tech.structure_miner.output.ae_mode", "AE模式");
        add("screen.dimension_tech.structure_miner.output.auto_pull_fluid", "自动拉取流体");
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
        add("config.jade.plugin_dimension_tech.structure_miner_status", "结构采掘器状态");
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
        add("jade.dimension_tech.external_equivalent_acceleration", "本周期外部等效加速：%sx");
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

    private void addVanillaRegistryNames() {
        add("dimension_tech.dimension.minecraft.overworld", "主世界");
        add("dimension_tech.dimension.minecraft.the_nether", "下界");
        add("dimension_tech.dimension.minecraft.the_end", "末地");
        add("dimension_tech.structure.minecraft.pillager_outpost", "掠夺者前哨站");
        add("dimension_tech.structure.minecraft.mineshaft", "废弃矿井");
        add("dimension_tech.structure.minecraft.mineshaft_mesa", "恶地废弃矿井");
        add("dimension_tech.structure.minecraft.mansion", "林地府邸");
        add("dimension_tech.structure.minecraft.jungle_pyramid", "丛林神庙");
        add("dimension_tech.structure.minecraft.desert_pyramid", "沙漠神殿");
        add("dimension_tech.structure.minecraft.igloo", "雪屋");
        add("dimension_tech.structure.minecraft.shipwreck", "沉船");
        add("dimension_tech.structure.minecraft.shipwreck_beached", "搁浅沉船");
        add("dimension_tech.structure.minecraft.swamp_hut", "沼泽小屋");
        add("dimension_tech.structure.minecraft.stronghold", "要塞");
        add("dimension_tech.structure.minecraft.monument", "海底神殿");
        add("dimension_tech.structure.minecraft.ocean_ruin_cold", "寒冷海洋废墟");
        add("dimension_tech.structure.minecraft.ocean_ruin_warm", "温暖海洋废墟");
        add("dimension_tech.structure.minecraft.fortress", "下界要塞");
        add("dimension_tech.structure.minecraft.nether_fossil", "下界化石");
        add("dimension_tech.structure.minecraft.end_city", "末地城");
        add("dimension_tech.structure.minecraft.buried_treasure", "埋藏的宝藏");
        add("dimension_tech.structure.minecraft.bastion_remnant", "堡垒遗迹");
        add("dimension_tech.structure.minecraft.village_plains", "平原村庄");
        add("dimension_tech.structure.minecraft.village_desert", "沙漠村庄");
        add("dimension_tech.structure.minecraft.village_savanna", "热带草原村庄");
        add("dimension_tech.structure.minecraft.village_snowy", "雪原村庄");
        add("dimension_tech.structure.minecraft.village_taiga", "针叶林村庄");
        add("dimension_tech.structure.minecraft.ruined_portal", "废弃传送门");
        add("dimension_tech.structure.minecraft.ruined_portal_desert", "沙漠废弃传送门");
        add("dimension_tech.structure.minecraft.ruined_portal_jungle", "丛林废弃传送门");
        add("dimension_tech.structure.minecraft.ruined_portal_swamp", "沼泽废弃传送门");
        add("dimension_tech.structure.minecraft.ruined_portal_mountain", "山地废弃传送门");
        add("dimension_tech.structure.minecraft.ruined_portal_ocean", "海洋废弃传送门");
        add("dimension_tech.structure.minecraft.ruined_portal_nether", "下界废弃传送门");
        add("dimension_tech.structure.minecraft.ancient_city", "远古城市");
        add("dimension_tech.structure.minecraft.trail_ruins", "古迹废墟");
        add("dimension_tech.structure.minecraft.trial_chambers", "试炼密室");
    }
}
