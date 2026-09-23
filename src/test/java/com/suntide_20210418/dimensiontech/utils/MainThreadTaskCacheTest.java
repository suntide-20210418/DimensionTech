package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.structure.analysis.StructureAnalysisService;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class MainThreadTaskCacheTest {
    @Test
    void serverThreadWorkSharesOneFairBudgetAcrossAllLayers() {
        var first = StructureAnalysisService.allocateExecutionBudget(2, 0, 10, 10, 10);
        assertEquals(1, first.templates());
        assertEquals(1, first.runtimeCaptures());
        assertEquals(0, first.virtualSamples());
        assertEquals(2, first.nextLayer());

        var second = StructureAnalysisService.allocateExecutionBudget(
                2, first.nextLayer(), 10, 10, 10);
        assertEquals(1, second.templates());
        assertEquals(0, second.runtimeCaptures());
        assertEquals(1, second.virtualSamples());
        assertEquals(1, second.nextLayer());

        var sparse = StructureAnalysisService.allocateExecutionBudget(32, 0, 1, 0, 2);
        assertEquals(1, sparse.templates());
        assertEquals(0, sparse.runtimeCaptures());
        assertEquals(2, sparse.virtualSamples());
        assertEquals(3, sparse.templates() + sparse.runtimeCaptures() + sparse.virtualSamples());
    }

    @Test
    void discoveryRequestsShareFutureAndExecuteOnlyWithinTickBudget() {
        try (var cache = new MainThreadTaskCache<String, Integer>(32)) {
            AtomicInteger scans = new AtomicInteger();
            Thread owner = Thread.currentThread();
            var first = cache.submit("template", () -> {
                assertSame(owner, Thread.currentThread());
                return scans.incrementAndGet();
            });
            assertSame(first, cache.submit("template", () -> 99));
            var second = cache.submit("another", scans::incrementAndGet);
            assertFalse(first.isDone());
            assertEquals(0, scans.get());
            assertEquals(1, cache.tick(1));
            assertEquals(1, first.join());
            assertFalse(second.isDone());
            assertEquals(1, cache.submit("template", () -> 99).join());
            assertEquals(1, cache.tick(1));
            assertEquals(2, second.join());
        }
    }

    @Test
    void failureRetriesAndShutdownSettlesQueuedRequests() {
        var cache = new MainThreadTaskCache<String, Integer>(1);
        var failed = cache.submit("key", () -> { throw new IllegalStateException("failure"); });
        assertTrue(cache.submit("full", () -> 1).isCompletedExceptionally());
        cache.tick(1);
        assertTrue(failed.isCompletedExceptionally());
        var retry = cache.submit("key", () -> 2);
        cache.tick(1);
        assertEquals(2, retry.join());
        cache.invalidate("key");
        var queued = cache.submit("key", () -> 3);
        cache.close();
        assertTrue(queued.isCompletedExceptionally());
        assertEquals(0, cache.queuedCount());
        assertTrue(cache.submit("closed", () -> 4).isCompletedExceptionally());
    }

    @Test
    void sharedAndCachedRequestsDoNotConsumeAdmissionBudget() {
        try (var cache = new MainThreadTaskCache<String, Integer>(32)) {
            AtomicInteger admissions = new AtomicInteger();
            var first = cache.submit("shared", () -> 1, () -> {
                admissions.incrementAndGet();
                return true;
            });
            AtomicBoolean called = new AtomicBoolean();
            assertSame(first, cache.submit("shared", () -> 2, () -> {
                called.set(true);
                return false;
            }));
            assertFalse(called.get());
            assertTrue(cache.submit("rejected", () -> 3, () -> false).isCompletedExceptionally());
            cache.tick(1);
            assertEquals(1, cache.submit("shared", () -> 4, () -> false).join());
            assertEquals(1, admissions.get());
        }
    }
}
