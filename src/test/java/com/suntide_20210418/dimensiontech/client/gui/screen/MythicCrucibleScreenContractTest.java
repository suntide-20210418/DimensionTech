package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MythicCrucibleScreenContractTest {
    private static final Path SCREEN =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen/MythicCrucibleScreen.java");

    @Test
    void usesTextureBackedDimensionsAndSharedLayout() throws Exception {
        String source = Files.readString(SCREEN);
        assertTrue(source.contains("MythicCrucibleLayout.WIDTH"));
        assertTrue(source.contains("MythicCrucibleLayout.HEIGHT"));
        assertTrue(source.contains("MythicCrucibleLayout.TEXTURE"));
        assertTrue(source.contains("leftPos"));
        assertTrue(source.contains("topPos"));
    }

    @Test
    void rendersMachineAndInventoryLabelsAtStableLocalCoordinates() throws Exception {
        String source = Files.readString(SCREEN);
        assertTrue(source.contains("KEY_TITLE"));
        assertTrue(source.contains("KEY_INVENTORY_LABEL"));
        assertTrue(source.contains("TITLE_LABEL_X = 7"));
        assertTrue(source.contains("TITLE_LABEL_Y = 6"));
        assertTrue(source.contains("PLAYER_INVENTORY.x()"));
        assertTrue(source.contains("PLAYER_INVENTORY.y() - 10"));
        assertTrue(source.contains("Component.translatable(KEY_TITLE)"));
        assertTrue(source.contains("Component.translatable(KEY_INVENTORY_LABEL)"));
    }

    @Test
    void removesLegacyProgrammaticChrome() throws Exception {
        String source = Files.readString(SCREEN);
        for (String removed : new String[] {"drawRitualTrack", "renderReadout", "drawControls",
                "MythicMinerTheme.panel", "MythicMinerTheme.subPanel", "MythicMinerTheme.slot",
                "RITUAL_TRACK_X", "FACE_CONTROL_X"}) {
            assertFalse(source.contains(removed), removed);
        }
    }

    @Test
    void dynamicDisplaysReadSynchronizedMenuData() throws Exception {
        String source = Files.readString(SCREEN);
        for (String accessor : new String[] {"menu.inputFluid()", "menu.outputFluid()",
                "menu.inputAmount()", "menu.outputAmount()", "menu.stateTicks()",
                "menu.refiningTicks()", "menu.resultTimeTicks()", "menu.timeReduction()",
                "menu.timePenalty()", "menu.extraFragments()", "menu.fluidReductionBp()",
                "menu.outputBonusBp()"}) {
            assertTrue(source.contains(accessor), accessor);
        }
    }

    @Test
    void statusDisplayUsesLocalizedRowsAndStateColors() throws Exception {
        String source = Files.readString(SCREEN);
        for (String key : new String[] {"KEY_STATUS_CURRENT", "KEY_STATUS_SEQUENCE",
                "KEY_STATUS_PROGRESS", "KEY_STATUS_REFINING_PROGRESS", "KEY_STATUS_NEEDS",
                "KEY_STATUS_PREVIOUS", "previousStatusText", "sequenceColor", "progressColor",
                "STATUS_DISPLAY"}) {
            assertTrue(source.contains(key), key);
        }
        // The two draw-call triples are asserted on a whitespace-collapsed view: the contract is
        // about which tokens form one draw call, not about where the formatter puts line breaks.
        String collapsed = source.replaceAll("\\s+", "");
        assertTrue(collapsed.contains("STATUS_X,STATUS_Y,SEQUENCE_NEXT_COLOR"));
        assertTrue(
                collapsed.contains("STATUS_X,STATUS_Y+STATUS_LINE_HEIGHT*2,SEQUENCE_NEXT_COLOR"));
    }

    @Test
    void tankInteractionsUseNewLayoutAndKeepShiftClear() throws Exception {
        String source = Files.readString(SCREEN);
        assertTrue(source.contains("MythicCrucibleLayout.INPUT_TANK.contains"));
        assertTrue(source.contains("MythicCrucibleLayout.OUTPUT_TANK.contains"));
        assertTrue(source.contains("hasShiftDown()"));
        assertTrue(source.contains("BUTTON_CLEAR_INPUT_TANK"));
        assertTrue(source.contains("BUTTON_CLEAR_OUTPUT_TANK"));
        assertTrue(source.contains("BUTTON_TOGGLE_INPUT_LOCK"));
    }
}
