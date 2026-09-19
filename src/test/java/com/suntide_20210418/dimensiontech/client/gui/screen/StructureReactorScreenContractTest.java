package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StructureReactorScreenContractTest {
    private static final Path SCREEN =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen/StructureReactorScreen.java");

    @Test
    void usesTextureBackedDimensionsAndSharedLayout() throws Exception {
        String source = Files.readString(SCREEN);
        assertTrue(source.contains("StructureReactorLayout.WIDTH"));
        assertTrue(source.contains("StructureReactorLayout.HEIGHT"));
        assertTrue(source.contains("StructureReactorLayout.TEXTURE"));
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
                "StructureMinerTheme.panel", "StructureMinerTheme.subPanel", "StructureMinerTheme.slot",
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
    void statusAndDetailPanelsReadLocalizedKeysFromLayoutRegions() throws Exception {
        String source = Files.readString(SCREEN);
        // Reward window on the machine face is driven by the merged status-reward region.
        for (String token : new String[] {"KEY_REWARD_WINDOW", "KEY_REWARD_IDLE",
                "STATUS_REWARD_AREA", "renderStatusReward", "inWindow", "REWARD_TEXT"}) {
            assertTrue(source.contains(token), token);
        }
        // The scrollable detail panel reads from the viewport region with a scissor clip and the
        // new detail.* key family.
        for (String token : new String[] {"KEY_DETAIL_SEQUENCE", "KEY_DETAIL_NEEDS",
                "KEY_DETAIL_CHANGES", "KEY_DETAIL_PREVIOUS", "KEY_DETAIL_FLUID_REQUIRED",
                "VIEWPORT", "renderDetail", "buildDetailLines", "enableScissor",
                "disableScissor", "mouseScrolled", "mouseDragged", "keyPressed"}) {
            assertTrue(source.contains(token), token);
        }
        // State coloring helpers survive the refactor.
        for (String token : new String[] {"previousStatusText", "sequenceColor", "progressColor"}) {
            assertTrue(source.contains(token), token);
        }
    }

    @Test
    void tankInteractionsUseNewLayoutAndKeepShiftClear() throws Exception {
        String source = Files.readString(SCREEN);
        assertTrue(source.contains("StructureReactorLayout.INPUT_TANK.contains"));
        assertTrue(source.contains("StructureReactorLayout.OUTPUT_TANK.contains"));
        assertTrue(source.contains("hasShiftDown()"));
        assertTrue(source.contains("BUTTON_CLEAR_INPUT_TANK"));
        assertTrue(source.contains("BUTTON_CLEAR_OUTPUT_TANK"));
        assertTrue(source.contains("BUTTON_TOGGLE_INPUT_LOCK"));
    }
}
