package com.suntide_20210418.dimensiontech.client.gui.screen;

/**
 * The shared instrument-panel palette: a light, bevelled, AE2-style console face with a dark
 * recessed well and dark ink. It is deliberately free of any Minecraft dependency so the colour
 * contract it encodes — every text token must stay legible on the face it is drawn on — can be
 * asserted by a plain unit test.
 *
 * <p>Three surfaces exist, and each one has its own readable set of tokens:
 *
 * <ul>
 *   <li>{@link #FACE} — the raised panel. Carries {@link #INK}, {@link #DIM} and the data accents.
 *   <li>{@link #WELL} — a recessed viewport. Carries legacy {@link #TEXT}/{@link #MUTED}, which are
 *       light and therefore reserved for recessed or banded surfaces only.
 *   <li>{@link #BAND} — the dark title band. Carries {@link #TEXT}/{@link #MUTED} and the {@code
 *       WELL_*} accents.
 * </ul>
 */
final class GuiPalette {
    // --- structure ---------------------------------------------------------
    /** Outer outline of every raised surface. */
    static final int HAIRLINE = 0xFF25292B;

    /** The panel face. */
    static final int FACE = 0xFFE6DECD;

    /**
     * A recessed viewport that keeps body text readable inside it. It is a touch darker than the
     * face so the recess reads at a glance, and dark enough for the light {@link #TEXT}.
     */
    static final int WELL = 0xFF4C4C46;

    /** The dark title band across the top of a window. */
    static final int BAND = 0xFF25292B;

    /** Bottom and right drop shadow of a raised surface. */
    static final int SHADE = 0xFF8C8170;

    /** Top and left highlight of a raised surface. */
    static final int HILIGHT = 0xFFFAF4E8;

    /** Secondary highlight, used one pixel inside {@link #HILIGHT}. */
    static final int BEVEL_MID = 0xFFF1E9DA;

    /** The face of a hovered control. */
    static final int HOVER = 0xFFF0E6D4;

    /** The dark well a slot sits in. */
    static final int SLOT_FACE = 0xFF373A38;

    /** One pixel highlight inside a slot well. */
    static final int SLOT_HILIGHT = 0xFF686A60;

    /** One pixel shadow inside a slot well. */
    static final int SLOT_SHADE = 0xFF202624;

    /** Selected row in the catalogue. */
    static final int SELECT = 0xFFD5B199;

    /** Zebra striping on data rows that sit on the light face. */
    static final int STRIPE_INK = 0xFFDCD0B9;

    /** Zebra striping on data rows that sit inside a recessed well. */
    static final int STRIPE_WELL = 0xFF55564E;

    // --- the machine canvas itself ----------------------------------------
    /*
     * These four match the RED WEDGE artwork: warm ivory and taupe recess tones.
     * Keep these in sync with the editable SVG masters under art-source/svg/. Anything drawn *on* the canvas has to come from this family, or it reads
     * as a foreign panel dropped onto the recess.
     */

    /** The recessed canvas face — what a page is painted on. */
    static final int RECESS = 0xFFCFC5B1;

    /** One step up from the canvas: the textures' raised slot face. Used to lift a hovered row. */
    static final int RECESS_LIT = 0xFFE3DACA;

    /** One step down: the textures' shaded recess edge. Used to press a selected row in. */
    static final int RECESS_PRESSED = 0xFFBCAF98;

    /** The textures' outer contour — rails and outlines drawn on the recess family. */
    static final int RECESS_EDGE = 0xFF746956;

    /** The empty track of a horizontal progress bar laid on the face. */
    static final int PROGRESS_TRACK = 0xFF6E6556;

    /** Overlay that dims a disabled control or slot. */
    static final int DISABLED_OVERLAY = 0x80000000;

    /** The canvas behind a machine window. */
    static final int BACKDROP = 0xFF141A1C;

    // --- ink ---------------------------------------------------------------
    /** Body text on {@link #FACE}. Deliberately pure black: all UI text is black. */
    static final int INK = 0xFF141A1C;

    /** Labels and units on {@link #FACE}, black for the same reason as {@link #INK}. */
    static final int DIM = 0xFF25292B;

    /** Legacy body text, light: for {@link #WELL} and {@link #BAND} only. */
    static final int TEXT = 0xFFFAF4E8;

    /** Legacy muted text, light: for {@link #WELL} and {@link #BAND} only. */
    static final int MUTED = 0xFFCFC5B1;

    // --- data accents on FACE ---------------------------------------------
    /** Amber axis: progress, elapsed time, reward windows. */
    static final int AMBER = 0xFF76501D;

    /** Steel-blue axis: structure data, primary values. */
    static final int FLUIX = 0xFF315E60;

    /** Settled successfully, or available. */
    static final int SUCCESS = 0xFF355E48;

    /** Blocked, penalised, or conflicting. */
    static final int ERROR = 0xFF9D3027;

    /** The notch at the leading edge of an incomplete progress scale. */
    static final int RECESS_ACTIVE = 0xFF852E29;

    // --- data accents on dark surfaces ------------------------------------
    /** Amber axis on the title band, a dark well, or a dark slot fill. */
    static final int WELL_AMBER = 0xFFE7B96D;

    /** Steel-blue axis on the title band, a dark well, or a dark slot fill. */
    static final int WELL_FLUIX = 0xFF8AC2BD;

    /** Settled-successfully accent on the title band or a dark well. */
    static final int WELL_SUCCESS = 0xFF9BC59D;

    /** Blocked or penalised accent on the title band or a dark well. */
    static final int WELL_ERROR = 0xFFF3A18B;

    // --- instruments -------------------------------------------------------
    /** Fallback tint for an empty fluid tank. */
    static final int FLUID = 0xFF557F80;

    /** The base steel-blue accent drawn as a band rail or a title mark on the light face. */
    static final int FLUID_ACCENT = 0xFF315E60;

    /** Border of a striped energy rail. */
    static final int ENERGY_BORDER = 0xFFE6DECD;

    /** Edge ticks that separate the striped rows of the recessed energy rail. */
    static final int ENERGY_TICK = 0xFF8C8170;

    /** Bright row of the empty half of an energy rail. */
    static final int ENERGY_BASE_LIGHT = 0xFF887C68;

    /** Dark row of the empty half of an energy rail: a two-tone stripe, matching AE2's meter. */
    static final int ENERGY_BASE_DARK = 0xFF514C42;

    /** The mid tone of the empty half of an energy rail. */
    static final int ENERGY_BASE_MID = 0xFF3A3932;

    /** Fill row of an energised rail. */
    static final int ENERGY_FILL_LIGHT = 0xFFC54332;

    /** Bright fill row of an energised rail. */
    static final int ENERGY_FILL_BRIGHT = 0xFFE16448;

    /** Hot centre row of an energised rail. */
    static final int ENERGY_FILL_HOT = 0xFFF4AA77;

    /** Dark fill row of an energised rail. */
    static final int ENERGY_FILL_DARK = 0xFF852E29;

    /** Mid fill row of an energised rail. */
    static final int ENERGY_FILL_MID = 0xFFAA3B2E;

    /** Contrast ratio a text token must clear against the surface it lands on. */
    static final double MIN_TEXT_CONTRAST = 3.0D;

    private GuiPalette() {}

    /** WCAG relative luminance of an ARGB colour, in {@code [0, 1]}. */
    static double relativeLuminance(int argb) {
        double red = linear((argb >> 16) & 0xFF);
        double green = linear((argb >> 8) & 0xFF);
        double blue = linear(argb & 0xFF);
        return 0.2126D * red + 0.7152D * green + 0.0722D * blue;
    }

    /** WCAG contrast ratio between two ARGB colours, in {@code [1, 21]}. */
    static double contrastRatio(int first, int second) {
        double a = relativeLuminance(first);
        double b = relativeLuminance(second);
        double lighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (lighter + 0.05D) / (darker + 0.05D);
    }

    /** Moves a colour towards white by {@code amount} while preserving its alpha. */
    static int lighten(int argb, double amount) {
        return mix(argb, 0xFFFFFF, amount);
    }

    /** Moves a colour towards black by {@code amount} while preserving its alpha. */
    static int darken(int argb, double amount) {
        return mix(argb, 0x000000, amount);
    }

    /** Blends {@code argb} towards {@code target} by {@code amount}, keeping the source alpha. */
    static int mix(int argb, int target, double amount) {
        double ratio = Math.max(0.0D, Math.min(1.0D, amount));
        return withAlpha(blend(argb, target, ratio), argb >>> 24);
    }

    /** Replaces the alpha channel of {@code argb}. */
    static int withAlpha(int argb, int alpha) {
        return (alpha & 0xFF) << 24 | (argb & 0x00FFFFFF);
    }

    private static int blend(int argb, int target, double ratio) {
        int red = channel(argb, 16, target, ratio);
        int green = channel(argb, 8, target, ratio);
        int blue = channel(argb, 0, target, ratio);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int channel(int argb, int shift, int target, double ratio) {
        int from = (argb >> shift) & 0xFF;
        int to = (target >> shift) & 0xFF;
        return (int) Math.round(from + (to - from) * ratio);
    }

    private static double linear(int channel) {
        double normalized = channel / 255.0D;
        return normalized <= 0.03928D
                ? normalized / 12.92D
                : Math.pow((normalized + 0.055D) / 1.055D, 2.4D);
    }
}
