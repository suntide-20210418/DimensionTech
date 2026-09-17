package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The crucible's screen buttons share one integer channel, and the menu reads anything at or above
 * {@link MythicCrucibleBlockEntity#BUTTON_CYCLE_FLUID_FACE_BASE} as a fluid-face cycle. These are
 * compile-time constants, so nothing here loads the block entity.
 */
class MythicCrucibleButtonIdContractTest {
    private static final List<Integer> BUTTONS =
            List.of(
                    MythicCrucibleBlockEntity.BUTTON_TOGGLE_AUTO_PULL,
                    MythicCrucibleBlockEntity.BUTTON_TOGGLE_AUTO_PUSH,
                    MythicCrucibleBlockEntity.BUTTON_TOGGLE_ME_NETWORK,
                    MythicCrucibleBlockEntity.BUTTON_TOGGLE_INPUT_LOCK,
                    MythicCrucibleBlockEntity.BUTTON_CLEAR_INPUT_TANK,
                    MythicCrucibleBlockEntity.BUTTON_CLEAR_OUTPUT_TANK);

    @Test
    void screenButtonsStayBelowTheFluidFaceCycleBase() {
        for (int id : BUTTONS) {
            assertTrue(
                    id < MythicCrucibleBlockEntity.BUTTON_CYCLE_FLUID_FACE_BASE,
                    "button " + id + " would be read as a fluid-face cycle");
        }
    }

    @Test
    void screenButtonsAreDistinct() {
        assertEquals(BUTTONS.size(), BUTTONS.stream().distinct().count(), "button ids must be unique");
    }
}
