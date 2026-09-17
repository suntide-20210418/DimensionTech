package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Locks the operator console to the geometry its menu ships. The restyle may repaint every panel
 * and well, but the slot coordinates belong to {@code StructureDataOperatorMenu}, so nothing in the
 * render path is allowed to move or to draw a raw rectangle over a slot.
 */
class StructureDataOperatorScreenLayoutTest {
    private static final Path SCREEN =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen/StructureDataOperatorScreen.java");
    private static final Path INTEGRATOR =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen/StructureDataIntegratorPage.java");

    @Test
    void compactAndFullWidthsStayInStep() {
        assertEquals(16, StructureDataOperatorScreen.LEFT_X);
        assertEquals(136, StructureDataOperatorScreen.LEFT_W);
        assertEquals(160, StructureDataOperatorScreen.RIGHT_X);
        assertEquals(144, StructureDataOperatorScreen.RIGHT_W);
        // Both columns end on the same edge, so the twin-pane page has one right margin.
        assertEquals(152, StructureDataOperatorScreen.LEFT_X + StructureDataOperatorScreen.LEFT_W);
        assertEquals(304, StructureDataOperatorScreen.RIGHT_X + StructureDataOperatorScreen.RIGHT_W);
        assertTrue(StructureDataOperatorScreen.RIGHT_X + StructureDataOperatorScreen.RIGHT_W <= 320);
    }

    @Test
    void theOperateActionRowClearsTheTargetSlotAndTheOperandGrid() {
        int targetBottom =
                StructureDataOperatorScreen.OPERATE_TARGET_Y + 16;
        int operandTop = 116;
        assertTrue(
                StructureDataOperatorScreen.OPERATE_ACTION_Y > targetBottom,
                "the action row must sit below the target marker slot");
        assertTrue(
                StructureDataOperatorScreen.OPERATE_ACTION_Y
                                + StructureDataOperatorScreen.OPERATE_ACTION_H
                        < operandTop,
                "the action row must sit above the operand grid");
        assertTrue(
                StructureDataOperatorScreen.OPERATE_COPY_X
                                + StructureDataOperatorScreen.OPERATE_COPY_W
                        < StructureDataOperatorScreen.OPERATE_CLEAR_X);
        assertTrue(
                StructureDataOperatorScreen.OPERATE_CLEAR_X
                                + StructureDataOperatorScreen.OPERATE_CLEAR_W
                        < 176);
    }

    @Test
    void everySlotIsPaintedThroughTheSharedSlotChrome() throws Exception {
        String source = Files.readString(SCREEN);

        assertFalse(
                source.contains("MythicMinerTheme.PANEL);"),
                "a raw panel rectangle would be drawn over a slot face");
        // Two slot groups exist: the device slots and the player inventory.
        assertEquals(
                2,
                count(source, "MythicMinerTheme.slot(g, slot.x, slot.y, false, false);"),
                "both slot groups must go through the recessed slot chrome");
    }

    @Test
    void thePluginHangersStayWhereTheMenuPutsThem() throws Exception {
        String source = Files.readString(SCREEN);

        // The menu pins the integrator and interpreter slots to x = -26, so the label plates have to
        // sit left of that, never on top of the console face.
        assertTrue(source.contains("structure_operator.integrator\","));
        assertTrue(source.contains("structure_operator.interpreter\","));
        assertTrue(source.contains("drawPluginLabel("));
        assertTrue(source.contains("-26 - width"));
    }

    @Test
    void theSearchFieldInkIsDarkEnoughForTheConsoleFace() throws Exception {
        String source = Files.readString(SCREEN);

        assertTrue(source.contains("search.setTextColor(MythicMinerTheme.INK);"));
        assertFalse(source.contains("search.setTextColor(MythicMinerTheme.TEXT);"));
    }

    @Test
    void theIntegratorPanesShareOneRightMarginAndKeepTheCatalogueScrollable() throws Exception {
        String source = Files.readString(INTEGRATOR);

        assertTrue(source.contains("StructureDataOperatorScreen.LEFT_X,"));
        assertTrue(source.contains("StructureDataOperatorScreen.RIGHT_X,"));
        assertTrue(
                source.contains("MythicMinerTheme.SELECT"),
                "the selected catalogue row must use the shared selection tone");
        assertTrue(source.contains("MythicMinerTheme.well(g, x, LIST_Y,"));
        assertFalse(source.contains("0xFF536779"));
    }

    private static int count(String haystack, String needle) {
        int total = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            total++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return total;
    }
}
