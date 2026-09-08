package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Behavioral baseline for miner loot aggregation before any performance optimization. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicMinerLootMergeGameTests {
    private MythicMinerLootMergeGameTests() {}

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
            helper.fail("Equivalent stacks did not merge with stable first-occurrence order: " + merged);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void stacksWithDifferentNbtRemainSeparate(GameTestHelper helper) {
        List<ItemStack> merged = new ArrayList<>();
        ItemStack first = new ItemStack(Items.STONE, 2);
        first.getOrCreateTag().putString("Marker", "first");
        ItemStack second = new ItemStack(Items.STONE, 3);
        second.getOrCreateTag().putString("Marker", "second");

        ExpectationRewardGenerator.mergeEquivalent(merged, first);
        ExpectationRewardGenerator.mergeEquivalent(merged, second);

        if (merged.size() != 2
                || merged.get(0).getCount() != 2
                || merged.get(1).getCount() != 3
                || !"first".equals(merged.get(0).getTag().getString("Marker"))
                || !"second".equals(merged.get(1).getTag().getString("Marker"))) {
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
                        "Mythic miner linear merge baseline: {} ns/merge ({} distinct stacks, {})",
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
            ItemStack stack = new ItemStack(Items.STONE, 1);
            stack.getOrCreateTag().putInt("BenchmarkVariant", index);
            templates.add(stack);
        }
        return templates;
    }

    private static long runMergeBenchmark(List<ItemStack> templates, MergeCase mergeCase, int iterations) {
        List<List<ItemStack>> inputs = new ArrayList<>(iterations);
        ItemStack candidate = candidate(templates, mergeCase);
        for (int iteration = 0; iteration < iterations; iteration++) {
            List<ItemStack> merged = new ArrayList<>(templates.size() + 1);
            for (ItemStack template : templates) merged.add(template.copy());
            inputs.add(merged);
        }
        long start = System.nanoTime();
        for (List<ItemStack> merged : inputs) ExpectationRewardGenerator.mergeEquivalent(merged, candidate);
        return System.nanoTime() - start;
    }

    private static ItemStack candidate(List<ItemStack> templates, MergeCase mergeCase) {
        if (mergeCase == MergeCase.FIRST_HIT) return templates.get(0).copy();
        if (mergeCase == MergeCase.LAST_HIT) return templates.get(templates.size() - 1).copy();
        ItemStack stack = new ItemStack(Items.STONE, 1);
        stack.getOrCreateTag().putInt("BenchmarkVariant", -1);
        return stack;
    }

    private enum MergeCase {
        FIRST_HIT,
        LAST_HIT,
        MISS
    }
}
