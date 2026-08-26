package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class OutputFaceScreenTest {
    @Test
    void itemCommandsUseWorldDirectionWhileFluidCommandsKeepLogicalDirection() {
        assertEquals(
                Direction.WEST,
                OutputFaceScreen.commandDirection(
                        OutputFaceScreen.Mode.ITEM_OUTPUT, Direction.NORTH, Direction.WEST));
        assertEquals(
                Direction.NORTH,
                OutputFaceScreen.commandDirection(
                        OutputFaceScreen.Mode.FLUID, Direction.NORTH, Direction.WEST));
    }
}
