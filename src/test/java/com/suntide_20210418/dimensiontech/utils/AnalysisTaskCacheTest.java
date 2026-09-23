package com.suntide_20210418.dimensiontech.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AnalysisTaskCacheTest {
    private static AnalysisTaskCache.Key key(String input) {
        return new AnalysisTaskCache.Key("expectation", input, "config", 1);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    @Test
    void sharesRunningFutureAndRetainsCompletedResult() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger();
        try (AnalysisTaskCache cache = new AnalysisTaskCache(1, 32)) {
            CompletableFuture<Integer> first = cache.submit(key("same"), () -> {
                executions.incrementAndGet();
                await(release);
                return 42;
            });
            try {
                assertSame(first, cache.submit(key("same"), () -> 99));
            } finally {
                release.countDown();
            }
            assertEquals(42, first.get(5, TimeUnit.SECONDS));
            assertEquals(42, cache.submit(key("same"), () -> 99).get());
            assertEquals(1, executions.get());
            assertEquals(0, cache.inFlightCount());
        }
    }

    @Test
    void queueIsBoundedAndOnlyWaitingWorkCanBeDiscarded() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (AnalysisTaskCache cache = new AnalysisTaskCache(1, 1)) {
            CompletableFuture<Integer> running = cache.submit(key("running"), () -> {
                started.countDown();
                await(release);
                return 7;
            });
            try {
                await(started);
                CompletableFuture<Integer> queued = cache.submit(key("queued"), () -> 8);
                assertTrue(cache.submit(key("rejected"), () -> 9).isCompletedExceptionally());
                assertFalse(cache.discardQueued(key("running")));
                assertTrue(cache.discardQueued(key("queued")));
                assertTrue(queued.isCompletedExceptionally());
                assertEquals(0, cache.queuedCount());
            } finally {
                release.countDown();
            }
            assertEquals(7, running.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void failureCanRetryWithoutDestroyingOtherResults() throws Exception {
        try (AnalysisTaskCache cache = new AnalysisTaskCache(1, 32)) {
            assertEquals(1, cache.submit(key("old"), () -> 1).get());
            assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> cache.submit(key("new"), () -> { throw new IllegalStateException(); })
                            .get(5, TimeUnit.SECONDS));
            assertEquals(1, cache.cachedCount());
            assertEquals(AnalysisLifecycle.TaskStatus.FAILED, cache.taskStatus(key("new")));
            assertNotNull(cache.failure(key("new")));
            assertTrue(cache.failure(key("new")).failedAtMillis() > 0);
            assertEquals(2, cache.submit(key("new"), () -> 2).get());
            assertEquals(AnalysisLifecycle.TaskStatus.SUCCEEDED, cache.taskStatus(key("new")));
            assertNull(cache.failure(key("new")));
            assertEquals(1, cache.submit(key("old"), () -> 99).get());
        }
    }

    @Test
    void differentConfigurationsAndAlgorithmVersionsRemainIndependent() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (AnalysisTaskCache cache = new AnalysisTaskCache(1, 32)) {
            CompletableFuture<Integer> running = cache.submit(key("blocker"), () -> {
                started.countDown();
                await(release);
                return 0;
            });
            CompletableFuture<Integer> first;
            CompletableFuture<Integer> second;
            CompletableFuture<Integer> revised;
            try {
                await(started);
                first = cache.submit(key("same"), () -> 1);
                second = cache.submit(new AnalysisTaskCache.Key(
                        "expectation", "same", "other-config", 1), () -> 2);
                revised = cache.submit(new AnalysisTaskCache.Key(
                        "expectation", "same", "config", 2), () -> 3);
                assertFalse(first.isDone());
                assertNotSame(first, second);
                assertNotSame(first, revised);
            } finally {
                release.countDown();
            }
            assertEquals(0, running.get(5, TimeUnit.SECONDS));
            assertEquals(1, first.get(5, TimeUnit.SECONDS));
            assertEquals(2, second.get(5, TimeUnit.SECONDS));
            assertEquals(3, revised.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void shutdownSettlesQueuedFuturesAndLetsRunningWorkFinish() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AnalysisTaskCache cache = new AnalysisTaskCache(1, 32);
        CompletableFuture<Integer> running = cache.submit(key("running"), () -> {
            started.countDown();
            await(release);
            interrupted.set(Thread.currentThread().isInterrupted());
            return 3;
        });
        try {
            await(started);
            CompletableFuture<Integer> queued = cache.submit(key("queued"), () -> 4);
            cache.close();
            assertTrue(queued.isCompletedExceptionally());
            assertEquals(0, cache.inFlightCount());
            assertEquals(0, cache.queuedCount());
            assertTrue(cache.submit(key("closed"), () -> 5).isCompletedExceptionally());
        } finally {
            release.countDown();
            cache.close();
        }
        assertEquals(3, running.get(5, TimeUnit.SECONDS));
        assertFalse(interrupted.get());
        assertTrue(cache.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(0, cache.cachedCount());
    }
}
