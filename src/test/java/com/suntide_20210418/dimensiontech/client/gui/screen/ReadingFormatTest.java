package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Holds every numeric reading to two decimals, on every screen that prints one.
 *
 * <p>The rule lives in {@link ReadingFormat} precisely because several screens print the same
 * numbers; the source-scan half of this test is what stops one of them growing its own format
 * string again.
 */
class ReadingFormatTest {
    private static final Path SCREEN_DIR =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen");

    @Test
    void aReadingShowsAtMostTwoDecimals() {
        assertEquals("0.60", ReadingFormat.reading(0.6013));
        assertEquals("1.00", ReadingFormat.reading(1.0));
        assertEquals("12.35", ReadingFormat.reading(12.3456));
        assertEquals("4.00", ReadingFormat.reading(3.9999));
        assertEquals("0.00", ReadingFormat.reading(0.0));

        for (double value : new double[] {0.01, 0.5, 1.0, 2.75, 12.3456, 1234.5678}) {
            String text = ReadingFormat.reading(value);
            assertTrue(
                    text.matches("\\d+\\.\\d{2}"),
                    "a plain reading must be digits with exactly two decimals: " + text);
        }
    }

    /**
     * The dimension and structure values used to be three decimals while the expectation column was
     * four. Both are now two, and they share the fallback below — a value column that behaved
     * differently from the expectation column beside it is exactly the drift this helper exists to
     * prevent.
     */
    @Test
    void aValueReadingUsesTheSameRuleAsAnExpectation() {
        assertEquals("1.23", ReadingFormat.reading(1.234));
        assertEquals("0.57", ReadingFormat.reading(0.567));
        assertEquals("2.30e-03", ReadingFormat.reading(0.0023));
    }

    /**
     * The catch with two decimals: everything below half a hundredth rounds to {@code 0.00}, which
     * would show a real drop as nothing at all. Those fall back to scientific notation — the same
     * escape the miner screen already used when the column was four decimals wide.
     */
    @Test
    void aDropTooSmallToShowDoesNotReadAsZero() {
        assertEquals("4.00e-03", ReadingFormat.reading(0.004));
        assertEquals("1.00e-04", ReadingFormat.reading(0.0001));

        for (double value : new double[] {0.0001, 0.001, 0.0023, 0.004, 0.0049}) {
            assertFalse(
                    "0.00".equals(ReadingFormat.reading(value)),
                    "a non-zero reading must never print as zero: " + value);
        }
    }

    /** The derived threshold must sit where two decimals can no longer show the value at all. */
    @Test
    void theFallbackStartsExactlyWhereTwoDecimalsWouldCollapse() {
        assertFalse(
                ReadingFormat.reading(0.005).contains("e"),
                "at the threshold the fixed format can still show the value");
        assertEquals("0.01", ReadingFormat.reading(0.005));
        assertTrue(
                ReadingFormat.reading(0.0049999).contains("e"),
                "just below it the value would collapse to 0.00, so the fallback takes over");
    }

    @Test
    void everyScreenDrawsItsReadingsThroughThisHelper() throws Exception {
        List<String> screens =
                List.of(
                        "StructMarkerScreen.java",
                        "StructureDataIntegratorPage.java",
                        "StructureDataOperatorOperationPage.java",
                        "StructureDataOperatorReadings.java",
                        "StructureMinerInfoPage.java");

        for (String name : screens) {
            String source = Files.readString(SCREEN_DIR.resolve(name));

            assertTrue(
                    source.contains("ReadingFormat.reading("),
                    name + " prints numeric readings, so it must use the shared format");
            for (String stale : new String[] {"%.4f", "%.3f"}) {
                assertFalse(
                        source.contains(stale),
                        name
                                + " still carries its own "
                                + stale
                                + " format, which is how the screens drifted apart before");
            }
        }
    }
}
