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
     * literal precision only: {@code "%.*f"} is C syntax and throws {@code
     * UnknownFormatConversionException} here.
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

    /**
     * Reads an exact rational at the display boundary without ever throwing.
     *
     * <p>{@link
     * com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability#finiteDoubleValue}
     * throws when the rational does not fit a double, and every caller here is on the render path —
     * an overflow there is a crash, not a wrong number. The saturating value is the largest finite
     * double rather than infinity, because infinity would poison the sort and print as {@code
     * Infinity} instead of reading as a placeholder.
     */
    static double displayValue(
            com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability expected) {
        double value = expected.doubleValue();
        return Double.isFinite(value) ? value : Double.MAX_VALUE;
    }

    // --- item quantities ---------------------------------------------------

    /**
     * Threshold at which a quantity switches from a plain integer to a suffixed one.
     *
     * <p>Ten thousand, so the four-digit case still prints in full and anything wider gets a unit.
     * The grid cell is eighteen pixels wide and the tooltip is read at a glance; {@code 12345} is
     * three pixels of noise next to {@code 12K} where the only extra information is the last three
     * digits of an expectation nobody acts on.
     */
    private static final double QUANTITY_THRESHOLD = 10_000.0D;

    /**
     * The decimal suffixes, smallest first. Index 0 means "no suffix" and is only reached by values
     * under {@link #QUANTITY_THRESHOLD}, so a thousand-grouped short form never appears.
     */
    private static final String[] QUANTITY_SUFFIXES = {"", "K", "M", "G", "T", "P", "E"};

    /**
     * Formats an expected item count as a short, readable amount.
     *
     * <p>The rules are "four significant digits and zero decimals, rounded", and the concrete ask
     * was that ten thousand reads as {@code 10K}. Those two do not both bite: with no decimals, a
     * mantissa below a thousand carries at most three significant digits, so the significant-digit
     * bound is an upper limit that the decimal rule already enforces. The rounding rule is
     * therefore what actually decides the output, and the unit is chosen as the largest one that
     * leaves at least one integral digit — {@code 9999} prints in full, {@code 10000} becomes
     * {@code 10K}, {@code 123456} becomes {@code 123K}, {@code 1234567} becomes {@code 1M}.
     *
     * <p>The renormalisation loop is not decorative. Rounding happens after the unit is picked, so
     * {@code 999_600} scales to {@code 999.6} and rounds to {@code 1000} — a mantissa that has
     * outgrown its own unit. Stepping up and re-rounding turns that into {@code 1M} instead of the
     * off-by-a-factor-of-a-thousand {@code 1000K}. It terminates because each step divides by a
     * thousand and the loop is bounded by the suffix list.
     *
     * <p>Negative and non-finite inputs cannot arrive from a loot expectation, but the formatter is
     * shared and has to stay total: a non-finite value falls back to the plain reading rather than
     * printing {@code Infinity} inside a unit suffix.
     */
    static String quantity(double value) {
        if (!Double.isFinite(value)) return reading(value);

        double magnitude = Math.abs(value);
        if (magnitude < QUANTITY_THRESHOLD) {
            // Zero decimals means the plain case is the identity for any whole count, and rounds
            // the fractional part of an expectation.
            return String.format(Locale.ROOT, "%.0f", value);
        }

        int unit = 0;
        double scaled = magnitude;
        while (scaled >= QUANTITY_THRESHOLD) {
            scaled /= 1000.0D;
            unit++;
            if (unit >= QUANTITY_SUFFIXES.length - 1) break;
        }

        /*
         * Halves round up, in floating point rather than through Math.round. Math.round returns a
         * long, so a saturating value would be clamped to Long.MAX_VALUE and then printed with all
         * of that integer's digits — a hundred-quintillion-item expectation would render as a
         * nineteen-digit number followed by a suffix. Flooring the sum keeps the magnitude in a
         * double and loses nothing that the suffix has not already thrown away.
         */
        double rounded = Math.floor(scaled + 0.5D);
        if (rounded >= 1000.0D && unit < QUANTITY_SUFFIXES.length - 1) {
            rounded /= 1000.0D;
            unit++;
        }

        String sign = value < 0.0D ? "-" : "";
        return sign + String.format(Locale.ROOT, "%.0f", rounded) + QUANTITY_SUFFIXES[unit];
    }
}
