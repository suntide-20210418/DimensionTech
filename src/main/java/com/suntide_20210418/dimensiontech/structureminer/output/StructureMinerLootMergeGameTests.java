package com.suntide_20210418.dimensiontech.structureminer.output;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Behavioral baseline for miner loot aggregation before any performance optimization.
 *
 * <p>1.20.1 用一层临时 NBT（"Marker" / "BenchmarkVariant"）区分产出物品以断言合并结果与顺序。1.21 没有 ItemStack NBT，这里改用
 * vanilla 自带的 {@code DataComponents.CUSTOM_DATA} 承载同一层临时数据：它本身就是「给第三方存任意
 * NBT」的组件，参与组件相等性比较（合并判等依赖的正是它），语义与旧写法一一对应 —— 同一个物品、不同数据就不合并、不重排 —— 而且不必为本测试新增注册一个组件类型。这些临时标记与结构标记的
 * {@code STRUCTURE_MARKER} 组件无关。
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureMinerLootMergeGameTests {
    private StructureMinerLootMergeGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void equivalentStacksMergeAndPreserveFirstOccurrenceOrder(GameTestHelper helper) {
        List<ItemStack> merged = new ArrayList<>();
        ItemStack stone = new ItemStack(Items.STONE, 2);
        ItemStack diamond = new ItemStack(Items.DIAMOND, 1);

        ExpectationRewardGenerator.mergeEquivalent(merged, stone);
        ExpectationRewardGenerator.mergeEquivalent(merged, diamond);
        ExpectationRewardGenerator.mergeEquivalent(merged, new ItemStack(Items.STONE, 3));

        if (merged.size() != 2
                || !merged.get(0).is(Items.STONE)
                || merged.get(0).getCount() != 5
                || !merged.get(1).is(Items.DIAMOND)
                || merged.get(1).getCount() != 1) {
            helper.fail(
                    "Equivalent stacks did not merge with stable first-occurrence order: "
                            + merged);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void stacksWithDifferentNbtRemainSeparate(GameTestHelper helper) {
        List<ItemStack> merged = new ArrayList<>();
        ItemStack first = markedStack(2, "Marker", "first");
        ItemStack second = markedStack(3, "Marker", "second");

        ExpectationRewardGenerator.mergeEquivalent(merged, first);
        ExpectationRewardGenerator.mergeEquivalent(merged, second);

        if (merged.size() != 2
                || merged.get(0).getCount() != 2
                || merged.get(1).getCount() != 3
                || !"first".equals(customDataString(merged.get(0), "Marker"))
                || !"second".equals(customDataString(merged.get(1), "Marker"))) {
            helper.fail("Stacks with different NBT were merged or reordered: " + merged);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void emptyStacksDoNotChangeMergedOutput(GameTestHelper helper) {
        List<ItemStack> merged = new ArrayList<>();
        ExpectationRewardGenerator.mergeEquivalent(merged, ItemStack.EMPTY);

        if (!merged.isEmpty()) {
            helper.fail("An empty stack changed merged output: " + merged);
            return;
        }
        helper.succeed();
    }

    /** Baseline only: report merge cost without imposing host-dependent timing assertions. */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void linearMergeBenchmarkReportsRepresentativeBaseline(GameTestHelper helper) {
        for (int existingStacks : List.of(8, 32, 128, 512)) {
            List<ItemStack> templates = templates(existingStacks);
            for (MergeCase mergeCase : MergeCase.values()) {
                long[] samples = new long[7];
                for (int sample = 0; sample < samples.length; sample++) {
                    samples[sample] = runMergeBenchmark(templates, mergeCase, 250);
                }
                java.util.Arrays.sort(samples);
                DimensionTechMod.LOGGER.info(
                        "Structure miner linear merge baseline: {} ns/merge ({} distinct stacks,"
                                + " {})",
                        samples[samples.length / 2] / 250,
                        existingStacks,
                        mergeCase.name().toLowerCase(java.util.Locale.ROOT));
            }
        }
        helper.succeed();
    }

    private static List<ItemStack> templates(int size) {
        List<ItemStack> templates = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            templates.add(markedStack(1, "BenchmarkVariant", index));
        }
        return templates;
    }

    private static long runMergeBenchmark(
            List<ItemStack> templates, MergeCase mergeCase, int iterations) {
        List<List<ItemStack>> inputs = new ArrayList<>(iterations);
        ItemStack candidate = candidate(templates, mergeCase);
        for (int iteration = 0; iteration < iterations; iteration++) {
            List<ItemStack> merged = new ArrayList<>(templates.size() + 1);
            for (ItemStack template : templates) merged.add(template.copy());
            inputs.add(merged);
        }
        long start = System.nanoTime();
        for (List<ItemStack> merged : inputs)
            ExpectationRewardGenerator.mergeEquivalent(merged, candidate);
        return System.nanoTime() - start;
    }

    private static ItemStack candidate(List<ItemStack> templates, MergeCase mergeCase) {
        if (mergeCase == MergeCase.FIRST_HIT) return templates.get(0).copy();
        if (mergeCase == MergeCase.LAST_HIT) return templates.get(templates.size() - 1).copy();
        return markedStack(1, "BenchmarkVariant", -1);
    }

    /** 临时标记物品栈：用 custom_data 承载一个 int 标记，替代旧写法的临时 NBT。 */
    private static ItemStack markedStack(int count, String key, int marker) {
        ItemStack stack = new ItemStack(Items.STONE, count);
        CompoundTag data = new CompoundTag();
        data.putInt(key, marker);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    /** 同上，标记值换成字符串。 */
    private static ItemStack markedStack(int count, String key, String marker) {
        ItemStack stack = new ItemStack(Items.STONE, count);
        CompoundTag data = new CompoundTag();
        data.putString(key, marker);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    private static String customDataString(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag().getString(key);
    }

    private enum MergeCase {
        FIRST_HIT,
        LAST_HIT,
        MISS
    }
}
