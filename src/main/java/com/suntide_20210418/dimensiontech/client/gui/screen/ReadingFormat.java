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
     * Hard cap on how many characters a quantity may occupy.
     *
     * <p><b>Four, and this is a geometric constraint rather than a preference.</b> The quantity is
     * drawn as a slot count in the bottom-right corner of an 18px cell. Vanilla metrics, measured
     * off {@code ascii.png} through {@code BitmapProvider}'s own advance rule, are 6px per digit
     * and per letter, 2px for the decimal point. Four characters therefore come to 24px, which
     * already overhangs the cell by 6px; five would be 30px and six — which is what "four
     * significant digits" produces, {@code 12.35K} — would be 32px, covering most of the
     * neighbouring cell's icon. Every rule below exists to hold this ceiling.
     */
    private static final int QUANTITY_MAX_CHARS = 4;

    /**
     * Threshold at which a quantity switches from a plain integer to a suffixed one.
     *
     * <p>Ten thousand. Below it the value is a count the player recognises and wants exactly, so it
     * prints as an integer — which is also within the {@link #QUANTITY_MAX_CHARS character cap},
     * since the widest such value is {@code 9999}. Above it the digits would overflow, so the value
     * moves onto a unit.
     */
    private static final double QUANTITY_THRESHOLD = 10_000.0D;

    /**
     * The decimal suffixes, smallest first. Index 0 means "no suffix" and is only reached by values
     * under {@link #QUANTITY_THRESHOLD}, so a thousand-grouped short form never appears.
     */
    private static final String[] QUANTITY_SUFFIXES = {"", "K", "M", "G", "T", "P", "E"};

    /**
     * Formats an expected item count as a short amount for a slot corner.
     *
     * <p><b>Two tiers, split at {@link #QUANTITY_THRESHOLD}, and both land under {@link
     * #QUANTITY_MAX_CHARS} characters.</b> Below it the value prints as a rounded integer with no
     * decimals: {@code 2400} stays {@code 2400}, because that is a count the player can compare
     * directly. At or above it the value is rescaled onto a {@link #QUANTITY_SUFFIXES unit} and the
     * mantissa keeps as many decimals as still leave room for the suffix — {@code 10000} reads
     * {@code 10K}, {@code 12345} reads {@code 12K}, {@code 1234567} reads {@code 1.2M}.
     *
     * <p><b>What this costs.</b> "Four significant digits" was the earlier specification and it
     * cannot be honoured here: {@code 12.35K} is six characters and 32px wide. Capping at four
     * characters leaves two significant digits for a mantissa in {@code [1, 100)} and three for one
     * in {@code [100, 1000)}. The exact figure is not lost — the tooltip's own {@code Expected}
     * line still carries the reading at two decimals.
     *
     * <p>Trailing zeros are trimmed, which is why ten thousand reads {@code 10K} and not {@code
     * 10.0K}: the digits are still significant, but a mantissa ending in {@code .0} advertises a
     * precision the number does not carry.
     *
     * <p><b>Renormalisation.</b> Rounding happens after the unit is picked, so a mantissa can round
     * up past its own unit: {@code 999_500} scales to {@code 1000K}. Stepping up and re-deriving
     * the decimals turns that into {@code 1M} instead of an off-by-a-factor-of-a-thousand {@code
     * 1000K}. It terminates because each step divides by a thousand and the loop is bounded by the
     * suffix list.
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
            // The mantissa shrank by a factor of a thousand, so its decimal budget grew;
            // re-deriving
            // rather than reusing `decimals` is what lets 1M carry a decimal at all.
            decimals = mantissaDecimals(rounded);
        }

        String sign = value < 0.0D ? "-" : "";
        /*
         * The suffix list can run out, which takes an expectation past a quintillion items — more
         * than any loot table produces. The saturating value from ReadingFormat#displayValue does
         * reach here, though, and its mantissa still carries all three hundred of its digits.
         *
         * No further division helps: the loop above already stopped at the largest suffix, so the
         * mantissa is simply too big for this scale. Clamping keeps the result inside the character
         * cap and reads as the placeholder it is, instead of printing 1.798e308 in full.
         */
        if (rounded >= 1000.0D) {
            rounded = 999.0D;
            decimals = 0;
        }
        return sign + trimTrailingZeros(formatFixed(rounded, decimals)) + QUANTITY_SUFFIXES[unit];
    }

    /**
     * Decimal places that still leave room for the mantissa's digits, its decimal point and the
     * suffix inside {@link #QUANTITY_MAX_CHARS}.
     *
     * <p>One for a single-digit mantissa ({@code 1.2K}), none for two or three ({@code 12K}, {@code
     * 123K}). Clamped at zero because a mantissa at or above a thousand has spent the whole budget
     * on its integer part, and a negative precision is not a format Java accepts.
     */
    private static int mantissaDecimals(double mantissa) {
        int magnitudeDigits = (int) Math.floor(Math.log10(Math.max(1.0D, mantissa))) + 1;
        // Two characters are spoken for by the decimal point and the suffix; the rest are digits.
        return Math.max(0, QUANTITY_MAX_CHARS - magnitudeDigits - 2);
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
