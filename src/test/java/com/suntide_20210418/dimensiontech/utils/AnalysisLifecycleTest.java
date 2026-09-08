package com.suntide_20210418.dimensiontech.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisLifecycleTest {
    @Test
    void discoveryStateExposesCacheAndTaskLifecyclesSeparately() {
        assertEquals(AnalysisLifecycle.CacheStatus.MISSING,
                StructureAnalysisService.State.missing().cacheStatus());
        DiscoveryResult result = new DiscoveryResult(
                AnalysisStatus.UNSUPPORTED, List.of(), List.of());
        var stale = new StructureAnalysisService.State(
                null, 0, 1, result, List.of(), false,
                AnalysisLifecycle.TaskStatus.FAILED, 123L, "failure");
        assertEquals(AnalysisLifecycle.CacheStatus.STALE, stale.cacheStatus());
        assertEquals(AnalysisLifecycle.TaskStatus.FAILED, stale.taskStatus());
        assertEquals(123L, stale.failedAtMillis());
        var complete = new StructureAnalysisService.State(
                null, 1, 1, result, List.of(), true,
                AnalysisLifecycle.TaskStatus.SUCCEEDED, 0L, null);
        assertEquals(AnalysisLifecycle.CacheStatus.COMPLETE, complete.cacheStatus());
        var carried = complete.staleFor(null);
        assertSame(result, carried.result());
        assertNull(carried.key());
        assertEquals(AnalysisLifecycle.CacheStatus.STALE, carried.cacheStatus());
        assertNull(carried.taskStatus());
    }

    @Test
    void generationControlsCommitButNeverMathematicalValidity() {
        var entry = AnalysisLifecycle.CacheEntry.complete(42, "input", "config", 3);
        var first = new AnalysisLifecycle.CommitToken(1, "input", "config");
        var second = new AnalysisLifecycle.CommitToken(2, "input", "config");
        assertTrue(entry.mathematicallyMatches("input", "config", 3));
        assertFalse(first.matches(2, "input", "config"));
        assertTrue(second.matches(2, "input", "config"));
        assertEquals(42, entry.result());
    }

    @Test
    void oldFingerprintCannotCommitOverNewRequest() {
        var old = new AnalysisLifecycle.CommitToken(7, "old", "config");
        assertFalse(old.matches(8, "new", "config"));
        assertFalse(old.matches(7, "new", "config"));
    }

    @Test
    void taskFailureLeavesOldResultUsableAsStale() {
        var old = AnalysisLifecycle.CacheEntry.complete("old result", "old", "config", 1);
        var stale = old.stale();
        assertEquals(AnalysisLifecycle.CacheStatus.STALE, stale.status());
        assertEquals("old result", stale.result());
        assertFalse(stale.mathematicallyMatches("new", "config", 1));
        assertEquals(AnalysisLifecycle.CacheStatus.MISSING,
                AnalysisLifecycle.CacheEntry.missing().stale().status());
    }
}
