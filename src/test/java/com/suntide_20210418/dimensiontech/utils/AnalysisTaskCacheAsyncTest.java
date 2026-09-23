package com.suntide_20210418.dimensiontech.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AnalysisTaskCacheAsyncTest {
    @Test
    void identicalAsyncKeysShareQueuedRunningAndCompletedFuture() {
        try (var cache = new AnalysisTaskCache(1, 8)) {
            var key = new AnalysisTaskCache.Key("expectation", "same", "config", 1);
            var gate = new CompletableFuture<Integer>();
            var calls = new AtomicInteger();
            var first = cache.submitAsync(key, () -> {
                calls.incrementAndGet();
                return gate;
            });
            var second = cache.submitAsync(key, () -> {
                calls.incrementAndGet();
                return CompletableFuture.completedFuture(99);
            });
            assertSame(first, second);
            gate.complete(42);
            assertEquals(42, first.join());
            assertSame(first, cache.submitAsync(key, () -> CompletableFuture.completedFuture(7)));
            assertEquals(1, calls.get());
        }
    }

    @Test
    void failedAsyncFutureIsRecordedAndCanBeRetriedWithSameKey() {
        try (var cache = new AnalysisTaskCache(1, 8)) {
            var key = new AnalysisTaskCache.Key("expectation", "failure", "config", 1);
            var failed = cache.submitAsync(key, () -> CompletableFuture.failedFuture(new IllegalStateException("boom")));
            try {
                failed.join();
            } catch (Exception ignored) {
                // expected
            }
            org.junit.jupiter.api.Assertions.assertEquals(
                    AnalysisLifecycle.TaskStatus.FAILED, cache.taskStatus(key));
            var retried = cache.submitAsync(key, () -> CompletableFuture.completedFuture(8));
            assertEquals(8, retried.join());
        }
    }
}
