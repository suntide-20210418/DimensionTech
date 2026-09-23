package com.suntide_20210418.dimensiontech.client.gui.screen;

import java.util.Locale;

/**
 * Number formatting shared by every numeric reading on the analysis screens.
 *
 * <p>The same numbers are printed from four places — the miner's information page, the marker
 * analysis screen, the operator's operate page and its structure pages. Each carried its own format
 * string: the expectation column agreed at four decimals, the value readings at three, and nothing
 * held any of them together. The precision is defined once here instead.
 *
 * <p>Two decimals is the width the multiplier column already used, and four was more precision than
 * anyone reads in a column that is a long list of drops. Values came down from three for the same
 * reason: alignment between the columns, and one less thing to eyeball.
 *
 * <p>The catch with any coarser precision is that rounding turns small values into zero. At four
 * decimals anything under {@code 0.0001} printed as {@code 0.0000}, and the miner already answered
 * that by falling back to scientific notation. That behaviour is kept for every reading, re-tuned
 * to the coarser precision, so a rare drop still reads as a drop rather than as {@code 0.00}.
 */
final class ReadingFormat {
    /** Decimals shown for a reading. */
    private static final int DECIMALS = 2;

    /**
     * The smallest value the fixed format can show without collapsing to zero: half of the last
     * digit it prints. Derived from {@link #DECIMALS} so the two cannot drift apart.
     */
    private static final double SMALLEST_VISIBLE = 0.5D / Math.pow(10.0D, DECIMALS);

    /**
     * Fixed-point format for a reading — {@code %.2f} at the current precision.
     *
     * <p>Assembled from {@link #DECIMALS} rather than written out, because Java's formatter takes a
     * literal precision only: {@code "%.*f"} is C syntax and throws
     * {@code UnknownFormatConversionException} here.
     */
    private static final String FIXED_FORMAT = "%." + DECIMALS + "f";

    /**
     * Scientific format used by the fallback below. Keeps the same number of decimals in the
     * mantissa as the fixed format, so a value does not appear to gain precision by being small.
     */
    private static final String SCIENTIFIC_FORMAT = "%." + DECIMALS + "e";

    private ReadingFormat() {}

    /**
     * Formats one reading — an expected item count, a dimension value, or a structure value.
     *
     * <p>Values at or below {@link #SMALLEST_VISIBLE} would print as {@code 0.00} while being
     * non-zero, so they are shown in scientific notation instead. Zero itself is not special cased:
     * it legitimately prints as {@code 0.00}.
     */
    static String reading(double value) {
        if (value > 0.0D && value < SMALLEST_VISIBLE) {
            return String.format(Locale.ROOT, SCIENTIFIC_FORMAT, value);
        }
        return String.format(Locale.ROOT, FIXED_FORMAT, value);
    }
}
