package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.loot.fingerprint.LootAnalysisFingerprint;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;

/** Deterministic lifecycle checks for the miner's asynchronous marker analysis. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicMinerMarkerAnalysisCacheGameTests {
    private MythicMinerMarkerAnalysisCacheGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 80)
    public static void sameFingerprintSubmitsOnlyOneRequest(GameTestHelper helper) {
        CacheFixture fixture = fixture(helper);
        fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 0L);
        fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 0L);
        fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 1L);
        if (fixture.starts.get() != 1) {
            helper.fail("Equivalent analysis inputs submitted duplicate requests");
            return;
        }
        fixture.futures.get(0).complete(value());
        helper.runAfterDelay(
                2,
                () -> {
                    fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 2L);
                    if (fixture.starts.get() != 1 || fixture.cache.entryForSlot(0) == null) {
                        helper.fail("Completed analysis was not reused for an equal fingerprint");
                        return;
                    }
                    helper.succeed();
                });
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 80)
    public static void oldCompletionCannotOverwriteNewFingerprint(GameTestHelper helper) {
        CacheFixture fixture = fixture(helper);
        fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 0L);
        ItemStack replacement = marker(helper, 42);
        fixture.inventory.setStackInSlot(0, replacement);
        fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 1L);
        if (fixture.starts.get() != 2) {
            helper.fail("Changed fingerprint did not submit a replacement request");
            return;
        }
        fixture.futures.get(1).complete(value());
        helper.runAfterDelay(
                2,
                () -> {
                    fixture.futures.get(0).complete(value());
                    helper.runAfterDelay(
                            2,
                            () -> {
                                var cached = fixture.cache.entryForSlot(0);
                                if (cached == null
                                        || !ItemStack.isSameItemSameTags(
                                                cached.marker(), replacement)) {
                                    helper.fail(
                                            "Old async completion overwrote the replacement"
                                                    + " marker");
                                    return;
                                }
                                helper.succeed();
                            });
                });
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 80)
    public static void clearedSlotRejectsOutstandingAnalysis(GameTestHelper helper) {
        CacheFixture fixture = fixture(helper);
        fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 0L);
        fixture.inventory.setStackInSlot(0, ItemStack.EMPTY);
        fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 1L);
        fixture.futures.get(0).complete(value());
        helper.runAfterDelay(
                2,
                () -> {
                    if (fixture.cache.entryForSlot(0) != null) {
                        helper.fail("Cleared marker slot accepted an outstanding analysis result");
                        return;
                    }
                    helper.succeed();
                });
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 160)
    public static void failedAnalysisRetriesOnlyAfterDelayAndRemovedResultsDoNotCommit(
            GameTestHelper helper) {
        CacheFixture fixture = fixture(helper);
        fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 0L);
        fixture.futures.get(0).completeExceptionally(new IllegalStateException("test failure"));
        helper.runAfterDelay(
                2,
                () -> {
                    fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 99L);
                    if (fixture.starts.get() != 1) {
                        helper.fail("Failed analysis retried before the delay elapsed");
                        return;
                    }
                    fixture.cache.refresh(helper.getLevel().getServer(), 0.0F, 100L);
                    if (fixture.starts.get() != 2) {
                        helper.fail("Failed analysis did not retry at the configured delay");
                        return;
                    }
                    fixture.removed.set(true);
                    fixture.futures.get(1).complete(value());
                    helper.runAfterDelay(
                            2,
                            () -> {
                                if (fixture.cache.entryForSlot(0) != null) {
                                    helper.fail("Removed miner accepted an async analysis result");
                                    return;
                                }
                                helper.succeed();
                            });
                });
    }

    private static CacheFixture fixture(GameTestHelper helper) {
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, marker(helper, 0));
        AtomicReference<LootAnalysisFingerprint> fingerprint =
                new AtomicReference<>(LootAnalysisFingerprint.from(inventory, 0.0F, "test"));
        AtomicBoolean removed = new AtomicBoolean();
        AtomicInteger starts = new AtomicInteger();
        List<CompletableFuture<StructureValueCalculator.StructureValue>> futures =
                new ArrayList<>();
        MythicMinerMarkerAnalysisCache cache =
                new MythicMinerMarkerAnalysisCache(
                        inventory,
                        () -> {
                            LootAnalysisFingerprint next =
                                    LootAnalysisFingerprint.from(inventory, 0.0F, "test");
                            fingerprint.set(next);
                            return next;
                        },
                        removed::get,
                        (server, level, info, luck) -> {
                            starts.incrementAndGet();
                            CompletableFuture<StructureValueCalculator.StructureValue> future =
                                    new CompletableFuture<>();
                            futures.add(future);
                            return future;
                        });
        return new CacheFixture(inventory, removed, starts, futures, cache);
    }

    private static ItemStack marker(GameTestHelper helper, int x) {
        ItemStack marker = new ItemStack(ModItems.STRUCTURE_MARKER.get());
        marker.getOrCreateTag()
                .put(
                        "StructureMarkerData",
                        StructMarkerItem.createCatalogueMarkerData(
                                helper.getLevel(),
                                ResourceLocation.fromNamespaceAndPath(
                                        "minecraft", "jungle_pyramid")));
        marker.getTag().getCompound("StructureMarkerData").getCompound("Position").putInt("X", x);
        return marker;
    }

    private static StructureValueCalculator.StructureValue value() {
        return new StructureValueCalculator.StructureValue(
                AnalysisStatus.EXACT, 1.0D, 1.0D, new StackMeasure(), List.of());
    }

    private record CacheFixture(
            ItemStackHandler inventory,
            AtomicBoolean removed,
            AtomicInteger starts,
            List<CompletableFuture<StructureValueCalculator.StructureValue>> futures,
            MythicMinerMarkerAnalysisCache cache) {}
}
