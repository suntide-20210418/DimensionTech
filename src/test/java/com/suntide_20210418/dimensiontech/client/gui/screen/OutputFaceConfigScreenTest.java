package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/**
 * Source-level contract for {@link OutputFaceConfigScreen}, which cannot be instantiated outside a
 * running client. The assertions lock the wiring that has broken before: which button id each click
 * sends, and which screen state each control mutates.
 */
class OutputFaceConfigScreenTest {
    private static final Path SCREEN =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen/OutputFaceConfigScreen.java");
    private static final Path MENU =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/client/gui/menu/MythicMinerMenu.java");

    private static String screenSource() throws Exception {
        return Files.readString(SCREEN);
    }

    private static String menuSource() throws Exception {
        return Files.readString(MENU);
    }

    /**
     * Faces are toggled by enum identity, not by layout position: the button at array index {@code
     * i} must send {@code 10 + DIRECTIONS[i].ordinal()}, because the menu decodes the id back with
     * {@code Direction.values()[id - 10]}. The cross layout deliberately orders its array up, west,
     * centre, east, south, down, which does not match the enum's own order — so an index-based id
     * would silently toggle the wrong face.
     */
    @Test
    void faceClicksAreAddressedByDirectionOrdinalNotLayoutIndex() throws Exception {
        assertTrue(screenSource().contains("click(10 + DIRECTIONS[i].ordinal())"));
        assertTrue(
                menuSource().contains("toggleOutputFace(net.minecraft.core.Direction.values()[id - 10])"));

        // The reason the ordinal arithmetic is load-bearing: the cross layout's east and west slots
        // sit at indices 1 and 3, while the enum places them at 5 and 4.
        assertNotEquals(1, Direction.EAST.ordinal());
        assertNotEquals(3, Direction.WEST.ordinal());
    }

    /** Every face must stay reachable through the same id arithmetic, in both directions. */
    @Test
    void everyDirectionRoundTripsThroughTheButtonIds() {
        for (Direction direction : Direction.values()) {
            int id = 10 + direction.ordinal();
            assertEquals(direction, Direction.values()[id - 10]);
        }
    }

    /** The AE toggle goes through the menu's named method; the fluid pull uses button id 26. */
    @Test
    void theTwoControlsMapToTheirMenuActions() throws Exception {
        assertTrue(screenSource().contains("menu.toggleAeOutputMode()"));
        assertTrue(screenSource().contains("click(26)"));
        assertTrue(menuSource().contains("blockEntity.toggleAutoExtractFluid()"));
    }

    /**
     * The screen paints its own opaque backdrop and never draws the parent behind it: rendering the
     * parent would put that screen's item stacks and tooltips behind the backdrop, where they stay
     * readable.
     */
    @Test
    void theParentScreenIsNotDrawnBehindThePanel() throws Exception {
        String source = screenSource();

        assertTrue(source.contains("g.fill(0, 0, width, height, MythicMinerTheme.BACKDROP)"));
        assertFalse(source.contains("parent.render("));
        assertFalse(source.contains("parent.resize("));
    }

    /** Closing returns to the miner screen instead of dropping the player to the HUD. */
    @Test
    void closingReturnsToTheParentScreen() throws Exception {
        assertTrue(screenSource().contains("Minecraft.getInstance().setScreen(parent)"));
    }
}
