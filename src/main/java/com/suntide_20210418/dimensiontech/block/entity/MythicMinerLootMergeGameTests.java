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

        MythicMinerLootSampler.mergeEquivalent(merged, stone);
        MythicMinerLootSampler.mergeEquivalent(merged, diamond);
        MythicMinerLootSampler.mergeEquivalent(merged, new ItemStack(Items.STONE, 3));

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

        MythicMinerLootSampler.mergeEquivalent(merged, first);
        MythicMinerLootSampler.mergeEquivalent(merged, second);

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
        MythicMinerLootSampler.mergeEquivalent(merged, ItemStack.EMPTY);

        if (!merged.isEmpty()) {
            helper.fail("An empty stack changed merged output: " + merged);
            return;
        }
        helper.succeed();
    }

    /**
     * Baseline only: a realistic high-parallel merge is measured before considering an index.
     * There is deliberately no time assertion because GameTest hosts have variable load.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void linearMergeBenchmarkReportsHighParallelBaseline(GameTestHelper helper) {
        final int existingStacks = 128;
        final int iterations = 500;
        List<ItemStack> templates = new ArrayList<>(existingStacks);
        for (int index = 0; index < existingStacks; index++) {
            ItemStack stack = new ItemStack(Items.STONE, 1);
            stack.getOrCreateTag().putInt("BenchmarkVariant", index);
            templates.add(stack);
        }
        ItemStack candidate = new ItemStack(Items.STONE, 1);
        candidate.getOrCreateTag().putInt("BenchmarkVariant", -1);

        runMergeBenchmark(templates, candidate, 50);
        long bestNanos = Long.MAX_VALUE;
        for (int sample = 0; sample < 5; sample++) {
            bestNanos = Math.min(bestNanos, runMergeBenchmark(templates, candidate, iterations));
        }
        DimensionTechMod.LOGGER.info(
                "Mythic miner linear merge baseline: {} ns/merge ({} existing distinct stacks)",
                bestNanos / iterations,
                existingStacks);
        helper.succeed();
    }

    private static long runMergeBenchmark(
            List<ItemStack> templates, ItemStack candidate, int iterations) {
        long start = System.nanoTime();
        for (int iteration = 0; iteration < iterations; iteration++) {
            List<ItemStack> merged = new ArrayList<>(templates.size() + 1);
            for (ItemStack template : templates) merged.add(template.copy());
            MythicMinerLootSampler.mergeEquivalent(merged, candidate);
        }
        return System.nanoTime() - start;
    }
}
