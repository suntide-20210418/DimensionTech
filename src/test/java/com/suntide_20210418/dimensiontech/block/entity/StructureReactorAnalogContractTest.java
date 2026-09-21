package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards the ways the comparator interface can break without a compiler noticing.
 *
 * <p>Both files are read as text, so nothing here loads a block entity, a level or a client class.
 * Assertions use full signatures rather than bare method names, because the javadoc around the
 * overrides deliberately names the methods that must stay absent.
 */
class StructureReactorAnalogContractTest {
    private static final Path BLOCK =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/block/StructureReactorBlock.java");
    private static final Path BLOCK_ENTITY =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/block/entity/StructureReactorBlockEntity.java");

    @Test
    void theBlockAnswersComparators() throws Exception {
        String source = Files.readString(BLOCK);

        assertTrue(source.contains("hasAnalogOutputSignal(BlockState state)"));
        assertTrue(
                source.contains(
                        "getAnalogOutputSignal(BlockState state, Level level, BlockPos pos)"));
        assertTrue(source.contains("reactor.analogSignal()"));
    }

    /**
     * Redstone control reads {@code Level#getBestNeighborSignal}, which asks each neighbour for the
     * signal it points back with. A reactor that emitted power would light the dust beside it, read
     * that dust back and latch itself on permanently, so only the analog channel may be implemented.
     */
    @Test
    void theBlockNeverEmitsRedstonePower() throws Exception {
        String source = Files.readString(BLOCK);

        assertFalse(source.contains("int getSignal("), "the reactor must not emit redstone power");
        assertFalse(source.contains("int getDirectSignal("), "the reactor must not emit strong power");
        assertFalse(
                source.contains("getWeakChanges("), "the reactor must not widen neighbour updates");
    }

    @Test
    void theLevelIsPublishedOnlyWhenItChanges() throws Exception {
        String announce = methodBody(Files.readString(BLOCK_ENTITY), "private void announceAnalogSignal()");

        assertTrue(announce.contains("updateNeighbourForOutputSignal"));
        assertTrue(
                announce.contains("signal == announcedAnalogSignal"),
                "an unchanged level must not notify its neighbours");
        assertTrue(
                announce.contains("announcedAnalogSignal = signal"),
                "the published level has to be remembered for the next comparison");
    }

    /**
     * A refinement whose resources are in place commits in the same server tick that ends it, so a
     * publish placed before the commit would flash the blocked level on a healthy reactor.
     */
    @Test
    void theTickBodyPublishesAfterItAdvancesTheCycle() throws Exception {
        String tick = methodBody(Files.readString(BLOCK_ENTITY), "public static void serverTick(");

        assertTrue(tick.contains("announceAnalogSignal()"));
        assertTrue(
                tick.indexOf("be.tryCommit()") < tick.indexOf("announceAnalogSignal()"),
                "publishing before the commit would report a healthy reactor as blocked");
    }

    @Test
    void theTickBodyPublishesEvenWhileRedstoneControlIsClosed() throws Exception {
        String tick = methodBody(Files.readString(BLOCK_ENTITY), "public static void serverTick(");

        assertFalse(
                tick.contains("if (be.redstoneControl && level.getBestNeighborSignal(pos) == 0)"),
                "an early return would leave a frozen reactor reporting a stale level");
        assertTrue(tick.contains("boolean cycleRuns"));
    }

    /**
     * The redstone report and the start behaviour have to read one verdict, or a reactor is reported
     * as blocked while it is happily running a cycle.
     */
    @Test
    void theStartVerdictHasASingleOwner() throws Exception {
        String tryStart = methodBody(Files.readString(BLOCK_ENTITY), "private void tryStart()");

        assertTrue(tryStart.contains("startCheck()"));
        assertFalse(tryStart.contains("firstMatching"), "tryStart must not recheck the material");
        assertFalse(tryStart.contains("getStackInSlot"), "tryStart must not recheck the fragments");
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, signature + " must exist in the source");
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return source.substring(open, i + 1);
            }
        }
        throw new AssertionError("unbalanced braces after " + signature);
    }
}
