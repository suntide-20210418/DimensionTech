package com.suntide_20210418.dimensiontech.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Server-owned mathematical results. Request generations belong to consumers, never this cache. */
final class AnalysisTaskCache implements AutoCloseable {
    record Key(String layer, String input, String config, int algorithmVersion) {}

    record Failure(long failedAtMillis, String error) {}

    private final ThreadPoolExecutor executor;
    private final Map<Key, CompletableFuture<?>> inFlight = new ConcurrentHashMap<>();
    private final Map<Key, Object> results = new LinkedHashMap<>();
    private final Map<Key, Pending<?>> pending = new LinkedHashMap<>();
    private final Map<Key, Failure> failures = new LinkedHashMap<>();
    private boolean closed;

    AnalysisTaskCache(int threads, int capacity) {
        executor =
                new ThreadPoolExecutor(
                        threads,
                        threads,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(capacity),
                        runnable -> {
                            Thread thread = new Thread(runnable, "dimension-tech-analysis");
                            thread.setDaemon(true);
                            return thread;
                        },
                        new ThreadPoolExecutor.AbortPolicy());
    }

    @SuppressWarnings("unchecked")
    synchronized <T> CompletableFuture<T> submit(Key key, Supplier<T> computation) {
        return submit(key, computation, true);
    }

    @SuppressWarnings("unchecked")
    synchronized <T> CompletableFuture<T> submit(
            Key key, Supplier<T> computation, boolean retainResult) {
        if (closed)
            return CompletableFuture.failedFuture(
                    new RejectedExecutionException("Analysis service is closed"));
        CompletableFuture<?> existing = inFlight.get(key);
        if (existing != null) return (CompletableFuture<T>) existing;
        if (results.containsKey(key))
            return CompletableFuture.completedFuture((T) results.get(key));
        Pending<T> task = new Pending<>(key, computation, retainResult);
        failures.remove(key);
        inFlight.put(key, task.future);
        pending.put(key, task);
        task.future.whenComplete(
                (value, error) -> {
                    synchronized (AnalysisTaskCache.this) {
                        inFlight.remove(key, task.future);
                    }
                });
        try {
            executor.execute(task);
        } catch (RejectedExecutionException error) {
            pending.remove(key);
            inFlight.remove(key);
            task.future.completeExceptionally(error);
            failures.put(key, new Failure(System.currentTimeMillis(), error.toString()));
        }
        return task.future;
    }

    /** Shares an asynchronous computation by key, including the completion future itself. */
    synchronized <T> CompletableFuture<T> submitAsync(
            Key key, Supplier<CompletableFuture<T>> computation) {
        CompletableFuture<?> existing = inFlight.get(key);
        if (existing != null) {
            @SuppressWarnings("unchecked")
            CompletableFuture<T> shared = (CompletableFuture<T>) existing;
            return shared;
        }
        if (results.containsKey(key)) {
            @SuppressWarnings("unchecked")
            T result = (T) results.get(key);
            return CompletableFuture.completedFuture(result);
        }
        CompletableFuture<T> shared = new CompletableFuture<>();
        inFlight.put(key, shared);
        try {
            executor.execute(
                    () -> {
                        try {
                            computation
                                    .get()
                                    .whenComplete(
                                            (value, error) -> {
                                                synchronized (AnalysisTaskCache.this) {
                                                    inFlight.remove(key, shared);
                                                    if (error == null && !closed)
                                                        results.put(key, value);
                                                }
                                                if (error == null) shared.complete(value);
                                                else shared.completeExceptionally(error);
                                            });
                        } catch (Throwable error) {
                            synchronized (AnalysisTaskCache.this) {
                                inFlight.remove(key, shared);
                                failures.put(
                                        key,
                                        new Failure(System.currentTimeMillis(), error.toString()));
                            }
                            shared.completeExceptionally(error);
                        }
                    });
        } catch (RejectedExecutionException error) {
            inFlight.remove(key, shared);
            shared.completeExceptionally(error);
        }
        return shared;
    }

    /** Call only after the request owner has established that no consumer needs this key. */
    synchronized boolean discardQueued(Key key) {
        Pending<?> task = pending.get(key);
        if (task == null || !executor.remove(task)) return false;
        pending.remove(key);
        inFlight.remove(key);
        task.future.completeExceptionally(new CancellationException("Unneeded queued analysis"));
        return true;
    }

    synchronized int inFlightCount() {
        return inFlight.size();
    }

    synchronized int cachedCount() {
        return results.size();
    }

    synchronized Failure failure(Key key) {
        return failures.get(key);
    }

    synchronized AnalysisLifecycle.TaskStatus taskStatus(Key key) {
        if (pending.containsKey(key)) return AnalysisLifecycle.TaskStatus.QUEUED;
        if (inFlight.containsKey(key)) return AnalysisLifecycle.TaskStatus.RUNNING;
        if (failures.containsKey(key)) return AnalysisLifecycle.TaskStatus.FAILED;
        return results.containsKey(key) ? AnalysisLifecycle.TaskStatus.SUCCEEDED : null;
    }

    int queuedCount() {
        return executor.getQueue().size();
    }

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        // Remove waiting work before shutdown; running pure computations finish without
        // interruption.
        for (Key key : new ArrayList<>(pending.keySet())) discardQueued(key);
        executor.shutdown();
        results.clear();
        failures.clear();
        pending.clear();
        inFlight.clear();
    }

    private final class Pending<T> implements Runnable {
        final Key key;
        final Supplier<T> computation;
        final CompletableFuture<T> future = new CompletableFuture<>();
        final boolean retainResult;

        Pending(Key key, Supplier<T> computation, boolean retainResult) {
            this.key = key;
            this.computation = computation;
            this.retainResult = retainResult;
        }

        @Override
        public void run() {
            synchronized (AnalysisTaskCache.this) {
                pending.remove(key);
            }
            T result;
            try {
                result = java.util.Objects.requireNonNull(computation.get(), "analysis result");
            } catch (Throwable error) {
                synchronized (AnalysisTaskCache.this) {
                    failures.put(key, new Failure(System.currentTimeMillis(), error.toString()));
                }
                future.completeExceptionally(error);
                return;
            }
            synchronized (AnalysisTaskCache.this) {
                if (!closed && retainResult) results.put(key, result);
                failures.remove(key);
            }
            future.complete(result);
        }
    }
}
