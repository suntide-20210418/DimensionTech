package com.suntide_20210418.dimensiontech.mythiccrucible;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class CrucibleSequenceTelemetryTest {
    @Test
    void preservesConcreteSequenceAndRepeatedStates() {
        List<StateId> sequence =
                List.of(
                        StateId.BRANCH,
                        StateId.RECURSE,
                        StateId.RECURSE,
                        StateId.CONVERGE,
                        StateId.STABILIZE);

        int[] packed = CrucibleSequenceTelemetry.pack(sequence);

        for (int index = 0; index < sequence.size(); index++) {
            assertEquals(
                    sequence.get(index),
                    CrucibleSequenceTelemetry.stateAt(packed[0], packed[1], sequence.size(), index),
                    "state at index " + index);
        }
        assertNull(CrucibleSequenceTelemetry.stateAt(packed[0], packed[1], sequence.size(), sequence.size()));
    }
}
