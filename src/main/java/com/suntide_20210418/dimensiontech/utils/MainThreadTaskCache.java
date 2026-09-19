package com.suntide_20210418.dimensiontech.utils;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Bounded discovery work pumped by the owning server thread, including completion callbacks. */
public final class MainThreadTaskCache<K, T> implements AutoCloseable {
    private final int capacity;
    private final Map<K, T> results = new HashMap<>();
    private final Map<K, CompletableFuture<T>> inFlight = new HashMap<>();
    private final ArrayDeque<Pending<K, T>> queue = new ArrayDeque<>();
    private boolean closed;

    public MainThreadTaskCache(int capacity) {
        this.capacity = capacity;
    }

    public CompletableFuture<T> submit(K key, Supplier<T> computation) {
        return submit(key, computation, () -> true);
    }

    public CompletableFuture<T> submit(K key, Supplier<T> computation, BooleanSupplier admission) {
        if (closed)
            return CompletableFuture.failedFuture(
                    new RejectedExecutionException("Discovery service is closed"));
        CompletableFuture<T> existing = inFlight.get(key);
        if (existing != null) return existing;
        if (results.containsKey(key)) return CompletableFuture.completedFuture(results.get(key));
        if (!admission.getAsBoolean())
            return CompletableFuture.failedFuture(
                    new RejectedExecutionException(
                            "Per-tick analysis submission budget is exhausted"));
        if (queue.size() >= capacity)
            return CompletableFuture.failedFuture(
                    new RejectedExecutionException("Discovery queue is full"));
        CompletableFuture<T> future = new CompletableFuture<>();
        inFlight.put(key, future);
        queue.addLast(new Pending<>(key, future, computation));
        return future;
    }

    public void invalidate(K key) {
        results.remove(key);
    }

    public int tick(int budget) {
        int executed = 0;
        while (!closed && executed < budget && !queue.isEmpty()) {
            Pending<K, T> task = queue.removeFirst();
            executed++;
            try {
                T value = java.util.Objects.requireNonNull(task.computation().get());
                if (!closed) results.put(task.key(), value);
                task.future().complete(value);
            } catch (Throwable error) {
                task.future().completeExceptionally(error);
            } finally {
                inFlight.remove(task.key(), task.future());
            }
        }
        return executed;
    }

    public int queuedCount() {
        return queue.size();
    }

    @Override
    public void close() {
        closed = true;
        while (!queue.isEmpty())
            queue.removeFirst()
                    .future()
                    .completeExceptionally(
                            new CancellationException("Server stopped before discovery started"));
        inFlight.clear();
        results.clear();
    }

    private record Pending<K, T>(K key, CompletableFuture<T> future, Supplier<T> computation) {}
}
