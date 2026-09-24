package com.suntide_20210418.dimensiontech.loot.expectation;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkedStructure;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer.StructureLoot;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator.StructureValue;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Behavioral contract for the loot fixtures under {@code data/dimension_tech/loot_table/gametest/}.
 *
 * <p>这 15 张表加上两个被引用的自引用定义（{@code item_modifier/gametest/recursive_function} 与 {@code
 * predicate/gametest/recursive_predicate}）在移植收尾时**没有任何 Java 消费者**：源 1.20.1 工程与目标 1.21.1
 * 工程都不曾有测试读它们（{@code docs/code-wiki.md} §13 曾把「18 个 fixture 由这些测试消费」写成事实，实际只有 {@code
 * loot_table/test/layered_equivalence.json} 被 {@code StructureValueCalculatorGameTests}
 * 使用）。本类把它们接回精确引擎。
 *
 * <p>接回来的第一件事就是查出两处 1.21 数据包格式断层 —— 两者都是「没人消费所以没人发现」的遗留：
 *
 * <ul>
 *   <li><b>fixture 自己是 1.20.1 语法</b>：{@code minecraft:loot_table} 条目写 {@code "name"}、{@code
 *       set_lore} 缺必填的 {@code mode}。在 1.21.1 上这些表**整个加载失败**（{@code Couldn't parse element ... No
 *       key value in MapLike[...]}），于是分析只会看到一张「不存在的表」。受影响 5 张：{@code missing_table} / {@code
 *       recursive_table} / {@code nested_terminal_root} / {@code nested_unsupported_function} /
 *       {@code unsupported_function}。其中 {@code missing_table} 的旧断言曾 **假通过** —— 根表没加载时同样会报 {@code
 *       MISSING_REFERENCE}，与「根表存在、被引用表缺失」的 语义撞车。
 *   <li><b>引擎自己也在按 1.20.1 的字段名解析嵌套表引用</b>（1.21 改成了 {@code value}，见 {@link
 *       NestedLootTableEntry1211}）。后果分两种：条目派发处把嵌套引用判成 UNSUPPORTED（至少不撒谎）， 而 {@link
 *       RuntimeLootAstSource#snapshotTables} 的递归抓取漏掉被引用表后，冻结路径会把引用当成 缺失表 ——
 *       **静默少算期望值**，正是本模组最不能接受的失效方式。
 * </ul>
 *
 * <p>每一条断言都对应源码里的确定语义，手算值优先：
 *
 * <ul>
 *   <li>缺失 / 递归引用**不是** UNSUPPORTED —— {@link ReferenceSemantics1211} 规定缺失表输出为空、缺失条件为 {@code
 *       false}、缺失函数为 {@code identity}，递归同理，并各自留下一条 warning 诊断。
 *   <li>未登记的战利品函数（{@code minecraft:set_lore}）落到「不支持」分支，且沿嵌套表级联。
 *   <li>{@code set_count} 会把数量夹到 {@code [0, maxStackSize]}，函数按 入口 → 池 → 表 的顺序叠加。
 *   <li>附魔枚举整体关闭时（{@code ExactEnchantmentSemantics1211.ENABLED == false}）， {@code
 *       enchant_with_levels} 是 no-op，产出保持未附魔。
 *   <li>冻结源引擎与实时引擎必须**同判**，因为它俩共用同一套语义，只有输入来源不同。
 *   <li>引擎判 UNSUPPORTED 时，{@code StructureValueCalculator} 还会再退化为采样兜底（{@code APPROXIMATE} + 诊断码
 *       {@code SAMPLING_APPROXIMATION}）—— 「不做近似 fallback」只对引擎成立，计算器是有降级阶梯的。
 * </ul>
 *
 * <p>凡实测与手算不符，按移植计划 §6.4 一律当 bug 处理，不允许为了让测试变绿而改写断言。
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LootFixtureSemanticsGameTests {
    /** {@code data/dimension_tech/loot_table/} 下 fixture 所在的子目录。 */
    private static final String FIXTURE_ROOT = "gametest/";

    /** 与 {@code StructureValueCalculator} 使用同一个状态空间上限。 */
    private static final int MAX_STATES = 1_000_000;

    /** 与 {@code score_provider.json} 里写死的 objective / target 名保持一致。 */
    private static final String SCORE_OBJECTIVE = "dt_loot_score";

    private static final String SCORE_HOLDER = "dimension_tech_gametest";

    private LootFixtureSemanticsGameTests() {}

    // ------------------------------------------------------------------
    // 缺失引用：输出为空 / 条件恒假 / 函数恒等，且都不得升级为 UNSUPPORTED
    // ------------------------------------------------------------------

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void missingReferencesDegradeWithoutFailing(GameTestHelper helper) {
        StructureValue missingTable = analyze(helper, "missing_table");
        requireStatus(helper, "missing_table", missingTable, AnalysisStatus.EXACT);
        requireDiagnostic(helper, "missing_table", missingTable, "MISSING_REFERENCE");
        requireCount(helper, "missing_table", missingTable, Items.STONE, 0);

        StructureValue missingPredicate = analyze(helper, "missing_predicate");
        requireStatus(helper, "missing_predicate", missingPredicate, AnalysisStatus.EXACT);
        requireDiagnostic(helper, "missing_predicate", missingPredicate, "MISSING_REFERENCE");
        requireCount(helper, "missing_predicate", missingPredicate, Items.STONE, 0);

        // 缺失函数解析为 identity，所以前面那条 set_count: 2 的结果必须原样保留。
        StructureValue missingFunction = analyze(helper, "missing_function");
        requireStatus(helper, "missing_function", missingFunction, AnalysisStatus.EXACT);
        requireDiagnostic(helper, "missing_function", missingFunction, "MISSING_REFERENCE");
        requireCount(helper, "missing_function", missingFunction, Items.STONE, 2);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 递归引用：与缺失引用同源地降级，不得把自引用判成不支持
    // ------------------------------------------------------------------

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void recursiveReferencesDegradeWithoutFailing(GameTestHelper helper) {
        StructureValue recursiveTable = analyze(helper, "recursive_table");
        requireStatus(helper, "recursive_table", recursiveTable, AnalysisStatus.EXACT);
        requireDiagnostic(helper, "recursive_table", recursiveTable, "RECURSIVE_REFERENCE");
        requireCount(helper, "recursive_table", recursiveTable, Items.STONE, 0);

        StructureValue recursivePredicate = analyze(helper, "recursive_predicate");
        requireStatus(helper, "recursive_predicate", recursivePredicate, AnalysisStatus.EXACT);
        requireDiagnostic(helper, "recursive_predicate", recursivePredicate, "RECURSIVE_REFERENCE");
        requireCount(helper, "recursive_predicate", recursivePredicate, Items.STONE, 0);

        StructureValue recursiveFunction = analyze(helper, "recursive_function");
        requireStatus(helper, "recursive_function", recursiveFunction, AnalysisStatus.EXACT);
        requireDiagnostic(helper, "recursive_function", recursiveFunction, "RECURSIVE_REFERENCE");
        requireCount(helper, "recursive_function", recursiveFunction, Items.STONE, 3);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 不支持函数：引擎判 UNSUPPORTED，计算器再退化为采样兜底
    // ------------------------------------------------------------------

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void unsupportedFunctionsPropagateThroughNesting(GameTestHelper helper) {
        /*
         * 两层契约，分别钉住：
         *
         * 引擎层 —— minecraft:set_lore 不在支持列表里，落到 DistributionalFunction1211 的
         *   "Unsupported reachable function" 分支，判 UNSUPPORTED。引擎层的证据是诊断码 UNSUPPORTED_TYPE
         *   出现在结果里（该码只由引擎的 unsupported 路径产生），而 analyze() 内部的
         *   requireEnginesAgree 已经证明冻结源引擎与实时引擎同判。
         *
         * 计算器层 —— 引擎判 UNSUPPORTED 且发现状态为 EXACT 时，StructureValueCalculator:887-896 会改走
         *   sampledValue(...)，于是对外状态是 APPROXIMATE 而不是 UNSUPPORTED。这一点与「精确分析、不做
         *   近似 fallback」的直觉相反，但它是计算器明确的降级阶梯（诊断码 SAMPLING_APPROXIMATION），
         *   不是移植回归 —— 所以断言按实测固化，不去迎合直觉。
         */
        StructureValue direct = analyze(helper, "unsupported_function");
        requireStatus(helper, "unsupported_function", direct, AnalysisStatus.APPROXIMATE);
        requireDiagnostic(helper, "unsupported_function", direct, "UNSUPPORTED_TYPE");
        requireDiagnostic(helper, "unsupported_function", direct, "SAMPLING_APPROXIMATION");

        // 同一张表经 loot_table 引用后嵌套进来，引擎同样判 UNSUPPORTED，计算器同样退化 —— 级联不会让它
        // 悄悄变成一个「精确」值。
        StructureValue nested = analyze(helper, "nested_unsupported_function");
        requireStatus(helper, "nested_unsupported_function", nested, AnalysisStatus.APPROXIMATE);
        requireDiagnostic(helper, "nested_unsupported_function", nested, "UNSUPPORTED_TYPE");
        requireDiagnostic(helper, "nested_unsupported_function", nested, "SAMPLING_APPROXIMATION");

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 有序执行：入口 → 池 → 表 的叠加顺序、多池并存、数量夹取
    // ------------------------------------------------------------------

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void orderedExecutionProducesExactCounts(GameTestHelper helper) {
        // 入口 set_count:2（绝对）→ 池 add uniform(1,2) → 表 add uniform(3,4)
        // 分布 {6:1/4, 7:1/2, 8:1/4}，期望 6/4 + 14/4 + 8/4 = 7。
        StructureValue ordered = analyze(helper, "function_order");
        requireStatus(helper, "function_order", ordered, AnalysisStatus.EXACT);
        requireCount(helper, "function_order", ordered, Items.STONE, 7);

        StructureValue multiStack = analyze(helper, "multi_stack");
        requireStatus(helper, "multi_stack", multiStack, AnalysisStatus.EXACT);
        requireCount(helper, "multi_stack", multiStack, Items.STONE, 2);
        requireCount(helper, "multi_stack", multiStack, Items.DIRT, 3);

        // set_count:100 必须夹到石头 64 的上限，而不是留下 100。
        StructureValue clamped = analyze(helper, "set_count_clamp");
        requireStatus(helper, "set_count_clamp", clamped, AnalysisStatus.EXACT);
        requireCount(helper, "set_count_clamp", clamped, Items.STONE, 64);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 计分板数量提供器：读实时计分板，scale 参与乘法
    // ------------------------------------------------------------------

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void scoreProviderReadsLiveScoreboardValues(GameTestHelper helper) {
        Scoreboard scoreboard = helper.getLevel().getScoreboard();
        Objective objective = scoreboard.getObjective(SCORE_OBJECTIVE);
        if (objective == null) {
            objective =
                    scoreboard.addObjective(
                            SCORE_OBJECTIVE,
                            ObjectiveCriteria.DUMMY,
                            Component.literal(SCORE_OBJECTIVE),
                            ObjectiveCriteria.RenderType.INTEGER,
                            false,
                            null);
        }
        ScoreHolder holder = ScoreHolder.forNameOnly(SCORE_HOLDER);

        // 4 × scale 1.5 = 6.0；因为是整数结果，取整方向不影响断言。
        // 这张表读活体计分板，而冻结上下文按设计没有 level（`DistributionalNumberProvider1211:255`
        // 直接返回 null ⇒ UNSUPPORTED），所以只有实时路径参与断言。
        scoreboard.getOrCreatePlayerScore(holder, objective).set(4);
        StructureValue scored = analyze(helper, "score_provider", "scored", false);
        requireStatus(helper, "score_provider(4)", scored, AnalysisStatus.EXACT);
        requireCount(helper, "score_provider(4)", scored, Items.STONE, 6);

        // 分数被清掉以后必须静默取 0（与 ScoreboardValue#getFloat 同义），而不是判 UNSUPPORTED。
        scoreboard.resetSinglePlayerScore(holder, objective);
        StructureValue unscored = analyze(helper, "score_provider", "unscored", false);
        requireStatus(helper, "score_provider(no score)", unscored, AnalysisStatus.EXACT);
        requireCount(helper, "score_provider(no score)", unscored, Items.STONE, 0);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 附魔开关契约：ENABLED=false 期间，enchant_with_levels 不产出附魔物品
    // ------------------------------------------------------------------

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void disabledEnchantmentKeepsTerminalOutputExact(GameTestHelper helper) {
        // score_provider 之外唯一两个带 enchant_with_levels 的表：末位函数被当作 no-op，
        // 因此产出的是未附魔的基础物品。
        StructureValue mixed = analyze(helper, "mixed_terminal_output");
        requireStatus(helper, "mixed_terminal_output", mixed, AnalysisStatus.EXACT);
        requireCount(helper, "mixed_terminal_output", mixed, Items.STONE, 3);
        requireCount(helper, "mixed_terminal_output", mixed, Items.DIAMOND_SWORD, 1);
        requireNoEnchantedOutput(helper, "mixed_terminal_output", mixed);

        // 经 loot_table 嵌套引用 child：引用层不得改变判定，也不得凭空引入附魔。
        StructureValue nested = analyze(helper, "nested_terminal_root");
        requireStatus(helper, "nested_terminal_root", nested, AnalysisStatus.EXACT);
        requireCount(helper, "nested_terminal_root", nested, Items.DIAMOND_SWORD, 1);
        requireCount(helper, "nested_terminal_root", nested, Items.STONE, 0);
        requireNoEnchantedOutput(helper, "nested_terminal_root", nested);

        StructureValue child = analyze(helper, "nested_terminal_child");
        requireStatus(helper, "nested_terminal_child", child, AnalysisStatus.EXACT);
        requireCount(helper, "nested_terminal_child", child, Items.DIAMOND_SWORD, 1);
        requireNoEnchantedOutput(helper, "nested_terminal_child", child);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 冻结源：递归抓取必须拿到被引用表，否则冻结路径静默少算
    // ------------------------------------------------------------------

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void frozenSourceCapturesNestedReferences(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ResourceLocation root = fixtureId("nested_terminal_root");
        ResourceLocation child = fixtureId("nested_terminal_child");

        RuntimeLootAstSource source = RuntimeLootAstSource.snapshotTables(server, List.of(root));
        // 这一条是本节的核心：`nested_terminal_root` 只通过 `"value"` 引用 child。抓取环节漏掉它不会抛错，
        // 只会让冻结路径把引用当成缺失表并返回空产出 —— 期望值静默偏低，且没有任何诊断能区分二者。
        if (source.table(child).isEmpty()) {
            helper.fail(
                    "nested_terminal_root references "
                            + child
                            + " but snapshotTables only captured the roots; the frozen engine"
                            + " would silently under-count the loot instead of resolving it");
        }

        // 冻结源的缓存键完全依赖 inputFingerprint()，同样的输入必须给出同样的指纹。
        String fingerprint = source.inputFingerprint();
        String repeated =
                RuntimeLootAstSource.snapshotTables(server, List.of(root)).inputFingerprint();
        if (!fingerprint.equals(repeated)) {
            helper.fail(
                    "snapshotTables must be stable for identical inputs: "
                            + fingerprint
                            + " != "
                            + repeated);
        }

        LootExpectationResult frozen =
                DistributionalLootTableExecutor1211.evaluate(
                        source,
                        root,
                        LootAnalysisContext.snapshot(
                                helper.absolutePos(BlockPos.ZERO), 0.0F, source.registries()),
                        MAX_STATES);
        if (frozen.status() != AnalysisStatus.EXACT) {
            helper.fail(
                    "nested_terminal_root(frozen): expected EXACT but got "
                            + frozen.status()
                            + " (diagnostics="
                            + frozen.diagnostics()
                            + ")");
        }
        requireCount(
                helper,
                "nested_terminal_root(frozen)",
                frozen.terminalMeasure(),
                Items.DIAMOND_SWORD,
                1);
        requireCount(
                helper, "nested_terminal_root(frozen)", frozen.terminalMeasure(), Items.STONE, 0);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 断言辅助
    // ------------------------------------------------------------------

    private static StructureValue analyze(GameTestHelper helper, String fixture) {
        return analyze(helper, fixture, fixture, true);
    }

    /**
     * 把一张 fixture 表当成「只含这一张表的结构」跑一次同步分析。
     *
     * @param cacheKey 合成结构 id 的后半段；同一张表在不同断言场景下要用不同 key，避免任何按标记指纹的缓存串味
     * @param requireFrozenAgreement 是否同时要求冻结源路径与实时路径同判；读取活体状态（计分板）的 fixture 必须传 {@code
     *     false}，因为冻结上下文按设计没有 level/计分板
     */
    private static StructureValue analyze(
            GameTestHelper helper,
            String fixture,
            String cacheKey,
            boolean requireFrozenAgreement) {
        ResourceLocation table = fixtureId(fixture);
        ResourceLocation structure =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "loot_fixture/" + cacheKey.replace('/', '_'));
        MarkerInfo marker =
                new MarkerInfo(
                        helper.getLevel().dimension().location(),
                        helper.absolutePos(BlockPos.ZERO),
                        new MarkedStructure(structure, new BoundingBox(0, 0, 0, 0, 0, 0)));
        DiscoveryResult discovery =
                new DiscoveryResult(
                        AnalysisStatus.EXACT,
                        List.of(new StructureLoot(structure, List.of(table), List.of(), List.of())),
                        List.of());
        StructureValue live =
                StructureValueCalculator.calculate(helper.getLevel(), marker, 0.0F, discovery);
        if (requireFrozenAgreement) {
            requireEnginesAgree(helper, fixture, table, marker, live);
        }
        return live;
    }

    private static ResourceLocation fixtureId(String fixture) {
        return ResourceLocation.fromNamespaceAndPath(
                DimensionTechMod.MOD_ID, FIXTURE_ROOT + fixture);
    }

    /**
     * 实时引擎与冻结源引擎必须给出同一个判定 —— 两者共用同一套语义，只有输入来源不同。
     *
     * <p>这一条专门盯住 {@link RuntimeLootAstSource#snapshotTables} 的递归抓取：漏抓被引用表时不会抛错，冻结
     * 引擎只会把引用当作缺失表并返回空产出，于是期望值**静默偏低**。
     *
     * <p>比较的是两个引擎而不是「计算器的结果 vs 引擎的结果」：{@code StructureValueCalculator} 在引擎判 UNSUPPORTED
     * 时还会再退化为采样兜底（见 {@code StructureValueCalculator:887-896}），那是它自己的策略层， 与「两条数据来源是否同判」无关。
     */
    private static void requireEnginesAgree(
            GameTestHelper helper,
            String fixture,
            ResourceLocation table,
            MarkerInfo marker,
            StructureValue live) {
        MinecraftServer server = helper.getLevel().getServer();
        RuntimeLootAstSource source = RuntimeLootAstSource.snapshotTables(server, List.of(table));
        source.verifyRuntimeInputs();
        LootExpectationResult liveEngine =
                DistributionalLootTableExecutor1211.evaluate(
                        server,
                        table,
                        LootAnalysisContext.at(helper.getLevel(), marker.position(), 0.0F),
                        MAX_STATES);
        LootExpectationResult frozenEngine =
                DistributionalLootTableExecutor1211.evaluate(
                        source,
                        table,
                        LootAnalysisContext.snapshot(marker.position(), 0.0F, source.registries()),
                        MAX_STATES);
        if (liveEngine.status() != frozenEngine.status()
                || !liveEngine
                        .terminalMeasure()
                        .exactItemCounts()
                        .equals(frozenEngine.terminalMeasure().exactItemCounts())) {
            helper.fail(
                    fixture
                            + ": the frozen source engine disagrees with the live engine (live"
                            + " status="
                            + liveEngine.status()
                            + ", terminal="
                            + liveEngine.terminalMeasure().exactItemCounts()
                            + "; frozen status="
                            + frozenEngine.status()
                            + ", terminal="
                            + frozenEngine.terminalMeasure().exactItemCounts()
                            + ", frozen diagnostics="
                            + frozenEngine.diagnostics()
                            + ")");
        }
        if (live.status() == AnalysisStatus.EXACT && liveEngine.status() != AnalysisStatus.EXACT) {
            helper.fail(
                    fixture
                            + ": an exact calculator result must come from an exact engine result,"
                            + " but the engine reported "
                            + liveEngine.status());
        }
    }

    private static void requireStatus(
            GameTestHelper helper, String fixture, StructureValue value, AnalysisStatus expected) {
        if (value.status() != expected) {
            helper.fail(
                    fixture
                            + ": expected status "
                            + expected
                            + " but got "
                            + value.status()
                            + " (diagnostics="
                            + value.diagnostics()
                            + ")");
        }
    }

    /** 以精确期望数量断言，避免经过 double。缺失物品时 {@code exactItemCount} 返回 0。 */
    private static void requireCount(
            GameTestHelper helper, String fixture, StructureValue value, Item item, long expected) {
        ExactProbability actual = value.terminalMeasure().exactItemCount(item);
        ExactProbability wanted = ExactProbability.of(expected, 1);
        if (!wanted.equals(actual)) {
            helper.fail(
                    fixture
                            + ": expected "
                            + expected
                            + " × "
                            + item
                            + " but the exact terminal measure is "
                            + actual
                            + " (status="
                            + value.status()
                            + ", diagnostics="
                            + value.diagnostics()
                            + ")");
        }
    }

    private static void requireCount(
            GameTestHelper helper,
            String fixture,
            TerminalStackMeasure measure,
            Item item,
            long expected) {
        ExactProbability actual = measure.exactItemCount(item);
        ExactProbability wanted = ExactProbability.of(expected, 1);
        if (!wanted.equals(actual)) {
            helper.fail(
                    fixture
                            + ": expected "
                            + expected
                            + " × "
                            + item
                            + " but the exact terminal measure is "
                            + actual);
        }
    }

    private static void requireDiagnostic(
            GameTestHelper helper, String fixture, StructureValue value, String code) {
        for (Diagnostic diagnostic : value.diagnostics()) {
            if (code.equals(diagnostic.code())) return;
        }
        helper.fail(fixture + ": expected a " + code + " diagnostic, got " + value.diagnostics());
    }

    /**
     * 附魔枚举关闭时，任何产出物品都不得带附魔。
     *
     * <p>终局压缩后的 {@link TerminalStackKey} 只保留 (item, count, rarity)，附魔身份已被丢弃，因此这一条只在完整 StackMeasure
     * 仍然可用时才能判定；这种情况下数量断言依然生效。
     */
    private static void requireNoEnchantedOutput(
            GameTestHelper helper, String fixture, StructureValue value) {
        if (!value.fullStackMeasureAvailable()) {
            return;
        }
        for (StackState state : value.measure().values().keySet()) {
            if (state.stack().isEnchanted()) {
                helper.fail(
                        fixture
                                + ": enchantment enumeration is disabled, but the analysis still"
                                + " produced an enchanted stack: "
                                + state);
            }
        }
    }
}
