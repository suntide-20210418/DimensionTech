package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the two ways the crucible's tick loop can silently stop reporting progress.
 *
 * <p>The container-data indices are compile-time constants, so this test never loads the block
 * entity or any client class.
 */
class MythicCrucibleTickContractTest {
    private static final Path BLOCK_ENTITY =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/block/entity/MythicCrucibleBlockEntity.java");

    @Test
    void anEmptyOperationSlotIsReportedAsAnEmptyInput() throws Exception {
        String source = Files.readString(BLOCK_ENTITY);

        assertTrue(
                source.contains("resolve(operation, operation.isEmpty())"),
                "the tick loop must state emptiness explicitly");
        assertFalse(
                source.contains("cycle.resolve(operation)"),
                "the single-argument overload infers emptiness from a null stack, which an ItemStack never is");
    }

    /**
     * Adding a telemetry value without widening the channel would leave the screen reading a stale
     * array entry, which is exactly the kind of silent failure this file exists to prevent.
     */
    @Test
    void everyDataIndexFitsTheChannel() {
        List<Integer> indices =
                List.of(
                        MythicCrucibleBlockEntity.DATA_STATUS,
                        MythicCrucibleBlockEntity.DATA_STATE_TICKS,
                        MythicCrucibleBlockEntity.DATA_STATE_INDEX,
                        MythicCrucibleBlockEntity.DATA_INPUT_AMOUNT,
                        MythicCrucibleBlockEntity.DATA_OUTPUT_AMOUNT,
                        MythicCrucibleBlockEntity.DATA_CURRENT_STATE,
                        MythicCrucibleBlockEntity.DATA_INPUT_FLUID,
                        MythicCrucibleBlockEntity.DATA_OUTPUT_FLUID,
                        MythicCrucibleBlockEntity.DATA_SEQUENCE_LENGTH,
                        MythicCrucibleBlockEntity.DATA_SEQUENCE_WORD_0,
                        MythicCrucibleBlockEntity.DATA_SEQUENCE_WORD_1,
                        MythicCrucibleBlockEntity.DATA_LAST_RESOLUTION,
                        MythicCrucibleBlockEntity.DATA_EVENT_STATE,
                        MythicCrucibleBlockEntity.DATA_TIME_REDUCTION,
                        MythicCrucibleBlockEntity.DATA_TIME_PENALTY,
                        MythicCrucibleBlockEntity.DATA_FLUID_REDUCTION_BP,
                        MythicCrucibleBlockEntity.DATA_FLUID_PENALTY_BP,
                        MythicCrucibleBlockEntity.DATA_OUTPUT_BONUS_BP,
                        MythicCrucibleBlockEntity.DATA_OUTPUT_PENALTY_BP,
                        MythicCrucibleBlockEntity.DATA_EXTRA_FRAGMENTS,
                        MythicCrucibleBlockEntity.DATA_RESULT_TIME,
                        MythicCrucibleBlockEntity.DATA_RESULT_FLUID,
                        MythicCrucibleBlockEntity.DATA_RESULT_OUTPUT,
                        MythicCrucibleBlockEntity.DATA_ELAPSED_TICKS,
                        MythicCrucibleBlockEntity.DATA_REFINING_TICKS,
                        MythicCrucibleBlockEntity.DATA_AUTO_PULL,
                        MythicCrucibleBlockEntity.DATA_AUTO_PUSH,
                        MythicCrucibleBlockEntity.DATA_ME_NETWORK,
                        MythicCrucibleBlockEntity.DATA_FLUID_FACE_MODES,
                        MythicCrucibleBlockEntity.DATA_INPUT_FLUID_LOCKED);

        assertEquals(
                MythicCrucibleBlockEntity.DATA_SLOT_COUNT,
                indices.size(),
                "every index must be listed here once");
        for (int index = 0; index < indices.size(); index++) {
            assertEquals(index, indices.get(index), "indices must be contiguous from zero");
        }
    }

    @Test
    void inputLockIsEnforcedByTheInputTankAndPersisted() throws Exception {
        String source = Files.readString(BLOCK_ENTITY);

        assertTrue(source.contains("private boolean inputFluidLocked"));
        assertTrue(source.contains("lockedInputFluid.isSame(stack.getFluid())"));
        assertTrue(source.contains("InputFluidLocked"));
        assertTrue(source.contains("LockedInputFluid"));
    }
}
