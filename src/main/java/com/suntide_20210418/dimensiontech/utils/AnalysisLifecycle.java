package com.suntide_20210418.dimensiontech.utils;

import java.util.Objects;

/** Separates mathematical cache validity from one request's execution lifecycle. */
public final class AnalysisLifecycle {
    private AnalysisLifecycle() {}

    public enum CacheStatus { MISSING, COMPLETE, STALE }
    public enum TaskStatus { QUEUED, RUNNING, SUCCEEDED, FAILED }

    public record CommitToken(long generation, String inputFingerprint, String configFingerprint) {
        public CommitToken {
            Objects.requireNonNull(inputFingerprint, "inputFingerprint");
            Objects.requireNonNull(configFingerprint, "configFingerprint");
        }

        public boolean matches(long currentGeneration, String currentInput, String currentConfig) {
            return generation == currentGeneration
                    && inputFingerprint.equals(currentInput)
                    && configFingerprint.equals(currentConfig);
        }
    }

    public record CacheEntry<T>(CacheStatus status, T result, String inputFingerprint,
            String configFingerprint, int algorithmVersion) {
        public CacheEntry {
            Objects.requireNonNull(status, "status");
            inputFingerprint = inputFingerprint == null ? "" : inputFingerprint;
            configFingerprint = configFingerprint == null ? "" : configFingerprint;
            if (status == CacheStatus.MISSING && result != null) {
                throw new IllegalArgumentException("Missing cache entry cannot contain a result");
            }
            if (status != CacheStatus.MISSING && result == null) {
                throw new IllegalArgumentException("Usable cache entry requires a result");
            }
        }

        public static <T> CacheEntry<T> missing() {
            return new CacheEntry<>(CacheStatus.MISSING, null, "", "", 0);
        }

        public static <T> CacheEntry<T> complete(T result, String input, String config, int version) {
            return new CacheEntry<>(CacheStatus.COMPLETE, result, input, config, version);
        }

        public CacheEntry<T> stale() {
            return status == CacheStatus.MISSING ? this
                    : new CacheEntry<>(CacheStatus.STALE, result, inputFingerprint,
                            configFingerprint, algorithmVersion);
        }

        public boolean mathematicallyMatches(String input, String config, int version) {
            return status != CacheStatus.MISSING && algorithmVersion == version
                    && inputFingerprint.equals(input) && configFingerprint.equals(config);
        }
    }
}
