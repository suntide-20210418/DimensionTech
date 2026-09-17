package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guards the two contracts the restyle rests on: the light face must stay clearly lighter than the
 * dark band and the dark slot well, and every text token must stay readable on the surface it was
 * chosen for. Both are pure colour maths, so they need no client to run.
 */
class GuiChromeTest {
    /** Text that is drawn on the raised light face. */
    private static final int[] FACE_INK = {
        GuiPalette.INK,
        GuiPalette.DIM,
        GuiPalette.AMBER,
        GuiPalette.FLUIX,
        GuiPalette.SUCCESS,
        GuiPalette.ERROR,
    };

    @Test
    void theLightFaceCannotBeConfusedWithTheDarkSurfaces() {
        assertTrue(
                GuiPalette.relativeLuminance(GuiPalette.FACE)
                        > GuiPalette.relativeLuminance(GuiPalette.BAND) + 0.35D);
        assertTrue(
                GuiPalette.relativeLuminance(GuiPalette.FACE)
                        > GuiPalette.relativeLuminance(GuiPalette.SLOT_FACE) + 0.35D);
        assertTrue(
                GuiPalette.relativeLuminance(GuiPalette.WELL)
                        > GuiPalette.relativeLuminance(GuiPalette.SLOT_FACE));
        assertTrue(
                GuiPalette.relativeLuminance(GuiPalette.FACE)
                        > GuiPalette.relativeLuminance(GuiPalette.WELL));
    }

    @Test
    void everyFaceTokenStaysReadableOnTheFace() {
        for (int ink : FACE_INK) {
            assertTrue(
                    GuiPalette.contrastRatio(ink, GuiPalette.FACE)
                            >= GuiPalette.MIN_TEXT_CONTRAST,
                    "token " + Integer.toHexString(ink) + " is too faint on the face");
        }
    }

    @Test
    void lightInkIsReservedForDarkSurfaces() {
        for (int ink : new int[] {GuiPalette.TEXT, GuiPalette.MUTED}) {
            assertTrue(
                    GuiPalette.contrastRatio(ink, GuiPalette.BAND)
                            >= GuiPalette.MIN_TEXT_CONTRAST,
                    "token " + Integer.toHexString(ink) + " is too faint on the title band");
        }
        assertTrue(
                GuiPalette.contrastRatio(GuiPalette.TEXT, GuiPalette.WELL)
                        >= GuiPalette.MIN_TEXT_CONTRAST,
                "TEXT on WELL measured "
                        + GuiPalette.contrastRatio(GuiPalette.TEXT, GuiPalette.WELL)
                        + " (well="
                        + Integer.toHexString(GuiPalette.WELL)
                        + ", text="
                        + Integer.toHexString(GuiPalette.TEXT)
                        + ", independent="
                        + independentContrast(GuiPalette.TEXT, GuiPalette.WELL)
                        + ")");
        // The dark ink family has to be measurably stronger on the face than the light one, so the
        // two can never be swapped by accident.
        for (int light : new int[] {GuiPalette.TEXT, GuiPalette.MUTED}) {
            for (int dark : new int[] {GuiPalette.INK, GuiPalette.DIM}) {
                assertTrue(
                        GuiPalette.contrastRatio(dark, GuiPalette.FACE)
                                > GuiPalette.contrastRatio(light, GuiPalette.FACE) + 1.0D,
                        "dark ink must clearly beat light ink on the face");
            }
        }
    }

    @Test
    void wellAccentsStayVisibleOnDarkSurfaces() {
        for (int accent :
                new int[] {
                    GuiPalette.WELL_AMBER, GuiPalette.WELL_FLUIX, GuiPalette.WELL_SUCCESS
                }) {
            assertTrue(
                    GuiPalette.contrastRatio(accent, GuiPalette.BAND)
                            >= GuiPalette.MIN_TEXT_CONTRAST);
        }
    }

    @Test
    void theEnergyRailKeepsItsStripedColours() {
        // Both rows of the striped base, and the fill, must be readable against the dark rail.
        for (int row :
                new int[] {
                    GuiPalette.ENERGY_BASE_LIGHT,
                    GuiPalette.ENERGY_BASE_DARK,
                    GuiPalette.ENERGY_FILL_LIGHT,
                    GuiPalette.ENERGY_FILL_BRIGHT,
                    GuiPalette.ENERGY_FILL_HOT,
                    GuiPalette.ENERGY_FILL_DARK,
                    GuiPalette.ENERGY_FILL_MID
                }) {
            assertTrue(
                    GuiPalette.contrastRatio(row, GuiPalette.SLOT_FACE) >= 1.1D,
                    "energy row " + Integer.toHexString(row) + " disappears into the rail");
        }
        assertTrue(
                GuiPalette.contrastRatio(
                                GuiPalette.ENERGY_BASE_LIGHT, GuiPalette.ENERGY_BASE_DARK)
                        >= 1.3D,
                "the two base rows must stay distinguishable as a stripe");
    }

    @Test
    void theBackdropStaysDistinctFromAPureBlackVeil() {
        assertEquals(
                0x00141618,
                GuiPalette.BACKDROP & 0xFFFFFF,
                "the machine backdrop must not be pure black, or a black veil blends into itself");
    }

    @Test
    void theFacadeExposesThePaletteVerbatim() {
        assertEquals(GuiPalette.FACE, MythicMinerTheme.PANEL);
        assertEquals(GuiPalette.BAND, MythicMinerTheme.FRAME);
        assertEquals(GuiPalette.WELL, MythicMinerTheme.INSET);
        assertEquals(GuiPalette.HAIRLINE, MythicMinerTheme.EDGE);
        assertEquals(GuiPalette.SLOT_FACE, MythicMinerTheme.SLOT_FACE);
        assertEquals(GuiPalette.INK, MythicMinerTheme.INK);
        assertEquals(GuiPalette.TEXT, MythicMinerTheme.TEXT);
        assertEquals(GuiPalette.HILIGHT, MythicMinerTheme.SLOT_HIGHLIGHT);
        assertEquals(GuiChrome.BAND_HEIGHT, MythicMinerTheme.BAND_HEIGHT);
    }

    @Test
    void theTitleBandFitsInsideAWindowWithItsAccentRail() {
        assertEquals(14, GuiChrome.BAND_HEIGHT);
        assertEquals(2, GuiChrome.RAIL_WIDTH);
        assertEquals(2, GuiChrome.SHADOW);
        assertTrue(
                GuiChrome.BAND_HEIGHT + GuiChrome.RAIL_WIDTH < 24,
                "the band must leave the title row readable without eating content space");
    }

    /** An independent, textbook WCAG implementation used only to cross-check the palette's own. */
    private static double independentContrast(int first, int second) {
        double a = independentLuminance(first);
        double b = independentLuminance(second);
        double lighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (lighter + 0.05D) / (darker + 0.05D);
    }

    private static double independentLuminance(int argb) {
        return 0.2126D * independentChannel((argb >> 16) & 0xFF)
                + 0.7152D * independentChannel((argb >> 8) & 0xFF)
                + 0.0722D * independentChannel(argb & 0xFF);
    }

    private static double independentChannel(int channel) {
        double normalized = channel / 255.0D;
        return normalized <= 0.03928D
                ? normalized / 12.92D
                : Math.pow((normalized + 0.055D) / 1.055D, 2.4D);
    }

    @Test
    void contrastMathIsSymmetricAndBounded() {        assertEquals(
                GuiPalette.contrastRatio(GuiPalette.FACE, GuiPalette.BAND),
                GuiPalette.contrastRatio(GuiPalette.BAND, GuiPalette.FACE));
        assertEquals(21.0D, GuiPalette.contrastRatio(0xFFFFFFFF, 0xFF000000), 0.01D);
        assertEquals(1.0D, GuiPalette.contrastRatio(GuiPalette.FACE, GuiPalette.FACE), 0.001D);
    }
}
