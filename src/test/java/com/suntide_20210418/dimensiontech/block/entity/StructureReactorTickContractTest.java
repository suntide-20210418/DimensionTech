package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the two ways the reactor's tick loop can silently stop reporting progress.
 *
 * <p>The container-data indices are compile-time constants, so this test never loads the block
 * entity or any client class.
 */
class StructureReactorTickContractTest {
    private static final Path BLOCK_ENTITY =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/block/entity/StructureReactorBlockEntity.java");

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
                        StructureReactorBlockEntity.DATA_STATUS,
                        StructureReactorBlockEntity.DATA_STATE_TICKS,
                        StructureReactorBlockEntity.DATA_STATE_INDEX,
                        StructureReactorBlockEntity.DATA_INPUT_AMOUNT,
                        StructureReactorBlockEntity.DATA_OUTPUT_AMOUNT,
                        StructureReactorBlockEntity.DATA_CURRENT_STATE,
                        StructureReactorBlockEntity.DATA_INPUT_FLUID,
                        StructureReactorBlockEntity.DATA_OUTPUT_FLUID,
                        StructureReactorBlockEntity.DATA_SEQUENCE_LENGTH,
                        StructureReactorBlockEntity.DATA_SEQUENCE_WORD_0,
                        StructureReactorBlockEntity.DATA_SEQUENCE_WORD_1,
                        StructureReactorBlockEntity.DATA_LAST_RESOLUTION,
                        StructureReactorBlockEntity.DATA_EVENT_STATE,
                        StructureReactorBlockEntity.DATA_TIME_REDUCTION,
                        StructureReactorBlockEntity.DATA_TIME_PENALTY,
                        StructureReactorBlockEntity.DATA_FLUID_REDUCTION_BP,
                        StructureReactorBlockEntity.DATA_FLUID_PENALTY_BP,
                        StructureReactorBlockEntity.DATA_OUTPUT_BONUS_BP,
                        StructureReactorBlockEntity.DATA_OUTPUT_PENALTY_BP,
                        StructureReactorBlockEntity.DATA_EXTRA_FRAGMENTS,
                        StructureReactorBlockEntity.DATA_RESULT_TIME,
                        StructureReactorBlockEntity.DATA_RESULT_FLUID,
                        StructureReactorBlockEntity.DATA_RESULT_OUTPUT,
                        StructureReactorBlockEntity.DATA_ELAPSED_TICKS,
                        StructureReactorBlockEntity.DATA_REFINING_TICKS,
                        StructureReactorBlockEntity.DATA_AUTO_PULL,
                        StructureReactorBlockEntity.DATA_AUTO_PUSH,
                        StructureReactorBlockEntity.DATA_ME_NETWORK,
                        StructureReactorBlockEntity.DATA_FLUID_FACE_MODES,
                        StructureReactorBlockEntity.DATA_INPUT_FLUID_LOCKED);

        assertEquals(
                StructureReactorBlockEntity.DATA_SLOT_COUNT,
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
