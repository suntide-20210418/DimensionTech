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
     * three pixels of noise next to {@code 12.35K} where the only extra information is the last
     * digits of an expectation nobody acts on.
     *
     * <p>The threshold is also where the precision rule changes: below it the value is a count the
     * player recognises and wants exactly, so it prints with no decimals, while above it the value
     * is being rescaled anyway and keeps four significant digits instead.
     */
    private static final double QUANTITY_THRESHOLD = 10_000.0D;

    /**
     * Significant digits kept by the suffixed form.
     *
     * <p>Four, which is what the display was specified with and also the most a mantissa in {@code
     * [1, 1000)} can carry in three decimals. The mantissa's decimal count follows from this rather
     * than being fixed: {@code 1.235K}, {@code 12.35K}, {@code 123.5K} all carry four.
     */
    private static final int QUANTITY_SIGNIFICANT_DIGITS = 4;

    /**
     * The decimal suffixes, smallest first. Index 0 means "no suffix" and is only reached by values
     * under {@link #QUANTITY_THRESHOLD}, so a thousand-grouped short form never appears.
     */
    private static final String[] QUANTITY_SUFFIXES = {"", "K", "M", "G", "T", "P", "E"};

    /**
     * Formats an expected item count as a short, readable amount.
     *
     * <p><b>Two tiers, split at {@link #QUANTITY_THRESHOLD}.</b> Below it the value is printed as
     * an integer — zero decimals, rounded — because it is a count the player can hold in their head
     * and {@code 9999} is more informative than {@code 10.00K}. At or above it the value is
     * rescaled onto a {@link #QUANTITY_SUFFIXES unit} and kept to {@link
     * #QUANTITY_SIGNIFICANT_DIGITS significant digits}, so the decimals come back: {@code 12450}
     * reads {@code 12.45K}, not {@code 12K}. Rounding to an integer before the unit is chosen would
     * throw away the leading digits of every value a structure actually produces.
     *
     * <p>The mantissa's decimal count is derived from the significant-digit budget rather than
     * fixed, so it shrinks as the mantissa grows: four digits means three decimals at {@code
     * 1.235K}, two at {@code 12.35K}, one at {@code 123.5K}, none at {@code 1235K} — which never
     * arises, because that mantissa would have been renormalised onto the next unit.
     *
     * <p>Trailing zeros are trimmed. The digits are still significant — {@code 10.00K} and {@code
     * 10K} denote the same rounded value — but a mantissa that ends in {@code .00} implies a
     * precision the number does not have, and the trimming is why {@code 10000} reads {@code 10K}
     * rather than {@code 10.00K}.
     *
     * <p><b>Renormalisation.</b> Rounding happens after the unit is picked, so a mantissa can round
     * up past its own unit: {@code 999_950} scales to {@code 999.95K} and rounds to {@code 1000K}.
     * Stepping up and re-rounding turns that into {@code 1M} instead of the off-by-a-factor-of-a-
     * thousand {@code 1000K}. It terminates because each step divides by a thousand and the loop is
     * bounded by the suffix list.
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

        int decimals = mantissaDecimals(scaled);
        double rounded = roundTo(scaled, decimals);
        if (rounded >= 1000.0D && unit < QUANTITY_SUFFIXES.length - 1) {
            rounded /= 1000.0D;
            unit++;
            // The mantissa shrank by a factor of a thousand, so it has room for three more
            // decimals; re-deriving rather than reusing `decimals` is what keeps 1M holding four
            // significant digits instead of one.
            decimals = mantissaDecimals(rounded);
        }

        String sign = value < 0.0D ? "-" : "";
        /*
         * The suffix list can run out, which takes an expectation past a quintillion items — more
         * than any loot table produces. The saturating value from ReadingFormat#displayValue does
         * reach here, though, and its mantissa still carries all three hundred of its digits.
         *
         * No further division helps: the loop above already stopped at the largest suffix, so the
         * mantissa is simply too big for this scale. Clamping to the significant-digit budget keeps
         * the result to a handful of characters and reads as the placeholder it is, instead of
         * printing 1.798e308 in full.
         */
        if (rounded >= 1000.0D) {
            rounded = 999.9D;
            decimals = 1;
        }
        return sign + trimTrailingZeros(formatFixed(rounded, decimals)) + QUANTITY_SUFFIXES[unit];
    }

    /**
     * Decimal places that show {@link #QUANTITY_SIGNIFICANT_DIGITS} significant digits in a
     * mantissa of {@code [1, 1000)}: three below ten, two below a hundred, one above.
     *
     * <p>Clamped at zero because a mantissa at or above a thousand has already spent its whole
     * budget on the integer part, and a negative precision is not a format Java accepts.
     */
    private static int mantissaDecimals(double mantissa) {
        int magnitudeDigits = (int) Math.floor(Math.log10(Math.max(1.0D, mantissa))) + 1;
        return Math.max(0, QUANTITY_SIGNIFICANT_DIGITS - magnitudeDigits);
    }

    /**
     * Rounds to {@code decimals} places in floating point.
     *
     * <p>Halves round up. Not via {@code Math.round}, which returns a long: a saturating magnitude
     * would be clamped to {@code Long.MAX_VALUE} and then printed with all nineteen of that
     * integer's digits. Flooring the sum keeps the magnitude in a double and loses nothing that the
     * suffix has not already thrown away.
     */
    private static double roundTo(double value, int decimals) {
        double scale = Math.pow(10.0D, decimals);
        return Math.floor(value * scale + 0.5D) / scale;
    }

    /** Fixed-point format at a runtime precision, for a mantissa rather than the whole reading. */
    private static String formatFixed(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /**
     * Drops the fraction and any trailing zeros from a formatted mantissa.
     *
     * <p>{@code 10.00} becomes {@code 10} and {@code 12.50} becomes {@code 12.5} — the digits stay
     * significant, but trailing zeros advertise a precision the value does not carry.
     */
    private static String trimTrailingZeros(String formatted) {
        if (formatted.indexOf('.') < 0) return formatted;
        int end = formatted.length();
        while (end > 0 && formatted.charAt(end - 1) == '0') end--;
        if (end > 0 && formatted.charAt(end - 1) == '.') end--;
        return formatted.substring(0, end);
    }
}
