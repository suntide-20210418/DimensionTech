package com.suntide_20210418.dimensiontech.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.suntide_20210418.dimensiontech.utils.StructureValueSnapshot.Config;
import com.suntide_20210418.dimensiontech.utils.StructureValueSnapshot.Expectation;
import com.suntide_20210418.dimensiontech.utils.StructureValueSnapshot.TerminalItem;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LayeredAnalysisPipelineTest {
    @Test
    void seedLuckAndValueConfigurationInvalidateOnlyTheirOwningLayers() throws Exception {
        AtomicInteger staticScans = new AtomicInteger();
        AtomicInteger virtualScans = new AtomicInteger();
        AtomicInteger expectations = new AtomicInteger();
        AtomicInteger values = new AtomicInteger();
        try (var discovery = new MainThreadTaskCache<String, String>(32);
                var worker = new AnalysisTaskCache(1, 32)) {
            var staticFirst = discovery.submit("dim|structure|discovery-config", () -> {
                staticScans.incrementAndGet();
                return "template-snapshot";
            });
            assertSame(staticFirst, discovery.submit("dim|structure|discovery-config", () -> "wrong"));
            discovery.tick(1);
            assertEquals("template-snapshot", staticFirst.join());

            var seedOne = discovery.submit("dim|structure|seed=1|generation-config", () -> {
                virtualScans.incrementAndGet();
                return "virtual-one";
            });
            var seedTwo = discovery.submit("dim|structure|seed=2|generation-config", () -> {
                virtualScans.incrementAndGet();
                return "virtual-two";
            });
            discovery.tick(2);
            assertNotEquals(seedOne.join(), seedTwo.join());
            assertEquals(1, staticScans.get());
            assertEquals(2, virtualScans.get());

            var expectationZero = worker.submit(new AnalysisTaskCache.Key(
                    "loot-expectation", "loot-snapshot", "luck=0|exact", 1), () -> {
                expectations.incrementAndGet();
                return expectation(ExactProbability.ONE);
            });
            assertSame(expectationZero, worker.submit(new AnalysisTaskCache.Key(
                    "loot-expectation", "loot-snapshot", "luck=0|exact", 1),
                    () -> expectation(ExactProbability.ZERO)));
            var expectationLucky = worker.submit(new AnalysisTaskCache.Key(
                    "loot-expectation", "loot-snapshot", "luck=1|exact", 1), () -> {
                expectations.incrementAndGet();
                return expectation(ExactProbability.of(2, 1));
            });
            Expectation zero = expectationZero.get(5, TimeUnit.SECONDS);
            Expectation lucky = expectationLucky.get(5, TimeUnit.SECONDS);
            assertEquals(2, expectations.get());
            assertEquals(1, staticScans.get());

            Config normal = config(1);
            Config doubledDimension = config(4);
            var normalValue = worker.submit(valueKey(zero, normal), () -> {
                values.incrementAndGet();
                return StructureValueSnapshot.calculate(zero, normal);
            });
            var changedValue = worker.submit(valueKey(zero, doubledDimension), () -> {
                values.incrementAndGet();
                return StructureValueSnapshot.calculate(zero, doubledDimension);
            });
            assertEquals(normalValue.get().structureValue() * 2, changedValue.get().structureValue());
            assertEquals(2, values.get());
            assertEquals(2, expectations.get());
            assertNotEquals(zero.inputFingerprint(), lucky.inputFingerprint());
        }
    }

    @Test
    void obsoleteRunningResultFinishesNormallyButCannotCommit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var worker = new AnalysisTaskCache(1, 32)) {
            var old = new AnalysisLifecycle.CommitToken(1, "old-input", "config");
            var future = worker.submit(new AnalysisTaskCache.Key("value", "old-input", "config", 1), () -> {
                started.countDown();
                await(started);
                await(release);
                return 17;
            });
            release.countDown();
            assertEquals(17, future.get(5, TimeUnit.SECONDS));
            assertFalse(future.isCancelled());
            assertFalse(old.matches(2, "new-input", "config"));
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    private static Expectation expectation(ExactProbability amount) {
        return new Expectation(Map.of(new TerminalItem("minecraft:diamond", 1, "RARE"), amount));
    }

    private static Config config(double dimension) {
        return new Config(dimension, Map.of("minecraft:diamond", Map.of("RARE", 50.0)));
    }

    private static AnalysisTaskCache.Key valueKey(Expectation input, Config config) {
        return new AnalysisTaskCache.Key("structure-value", input.inputFingerprint(),
                config.configFingerprint(), StructureValueSnapshot.ALGORITHM_VERSION);
    }
}
