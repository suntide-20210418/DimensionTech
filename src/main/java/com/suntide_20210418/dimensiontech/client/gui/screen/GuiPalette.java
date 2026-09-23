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
    static final int HAIRLINE = 0xFF000000;

    /** The panel face. */
    static final int FACE = 0xFFC6C6C6;

    /**
     * A recessed viewport that keeps body text readable inside it. It is a touch darker than the
     * face so the recess reads at a glance, and dark enough for the light {@link #TEXT}.
     */
    static final int WELL = 0xFF808080;

    /** The dark title band across the top of a window. */
    static final int BAND = 0xFF2B2B2B;

    /** Bottom and right drop shadow of a raised surface. */
    static final int SHADE = 0xFF555555;

    /** Top and left highlight of a raised surface. */
    static final int HILIGHT = 0xFFFFFFFF;

    /** Secondary highlight, used one pixel inside {@link #HILIGHT}. */
    static final int BEVEL_MID = 0xFFE0E0E0;

    /** The face of a hovered control. */
    static final int HOVER = 0xFFDCDCDC;

    /** The dark well a slot sits in. */
    static final int SLOT_FACE = 0xFF3F3F3F;

    /** One pixel highlight inside a slot well. */
    static final int SLOT_HILIGHT = 0xFF565656;

    /** One pixel shadow inside a slot well. */
    static final int SLOT_SHADE = 0xFF2F2F2F;

    /** Selected row in the catalogue. */
    static final int SELECT = 0xFF77909F;

    /** Zebra striping on data rows that sit on the light face. */
    static final int STRIPE_INK = 0xFFB4B4B4;

    /** Zebra striping on data rows that sit inside a recessed well. */
    static final int STRIPE_WELL = 0xFF969696;

    // --- the machine canvas itself ----------------------------------------
    /*
     * These four are sampled off the machine textures, not chosen: they are the greens the artwork is
     * actually painted in. Anything drawn *on* the canvas has to come from this family, or it reads
     * as a foreign panel dropped onto the recess.
     */

    /** The recessed canvas face — what a page is painted on. */
    static final int RECESS = 0xFF97B6A4;

    /** One step up from the canvas: the textures' raised slot face. Used to lift a hovered row. */
    static final int RECESS_LIT = 0xFFACC5B6;

    /** One step down: the textures' shaded recess edge. Used to press a selected row in. */
    static final int RECESS_PRESSED = 0xFF83A893;

    /** The textures' outer contour — rails and outlines drawn on the recess family. */
    static final int RECESS_EDGE = 0xFF3C5647;

    /** The empty track of a horizontal progress bar laid on the face. */
    static final int PROGRESS_TRACK = 0xFF555555;

    /** Overlay that dims a disabled control or slot. */
    static final int DISABLED_OVERLAY = 0x80000000;

    /** The canvas behind a machine window. */
    static final int BACKDROP = 0xFF141618;

    // --- ink ---------------------------------------------------------------
    /** Body text on {@link #FACE}. Deliberately pure black: all UI text is black. */
    static final int INK = 0xFF000000;

    /** Labels and units on {@link #FACE}, black for the same reason as {@link #INK}. */
    static final int DIM = 0xFF000000;

    /** Legacy body text, light: for {@link #WELL} and {@link #BAND} only. */
    static final int TEXT = 0xFFE6E6E6;

    /** Legacy muted text, light: for {@link #WELL} and {@link #BAND} only. */
    static final int MUTED = 0xFFA9A9A9;

    // --- data accents on FACE ---------------------------------------------
    /** Amber axis: progress, elapsed time, reward windows. */
    static final int AMBER = 0xFF8A5A14;

    /** Steel-blue axis: structure data, primary values. */
    static final int FLUIX = 0xFF2A6180;

    /** Settled successfully, or available. */
    static final int SUCCESS = 0xFF2E6B3E;

    /** Blocked, penalised, or conflicting. */
    static final int ERROR = 0xFFA6442F;

    /** The notch at the leading edge of an incomplete progress scale. */
    static final int RECESS_ACTIVE = 0xFF3F5B4A;

    // --- data accents on dark surfaces ------------------------------------
    /** Amber axis on the title band, a dark well, or a dark slot fill. */
    static final int WELL_AMBER = 0xFFC8943C;

    /** Steel-blue axis on the title band, a dark well, or a dark slot fill. */
    static final int WELL_FLUIX = 0xFF70C9E4;

    /** Settled-successfully accent on the title band or a dark well. */
    static final int WELL_SUCCESS = 0xFF6EA27F;

    /** Blocked or penalised accent on the title band or a dark well. */
    static final int WELL_ERROR = 0xFFE08A7A;

    // --- instruments -------------------------------------------------------
    /** Fallback tint for an empty fluid tank. */
    static final int FLUID = 0xFF5DAFC8;

    /** The base steel-blue accent drawn as a band rail or a title mark on the light face. */
    static final int FLUID_ACCENT = 0xFF2A6180;

    /** Border of a striped energy rail. */
    static final int ENERGY_BORDER = 0xFFC7C9D3;

    /** Edge ticks that separate the striped rows of the recessed energy rail. */
    static final int ENERGY_TICK = 0xFF8888A0;

    /** Bright row of the empty half of an energy rail. */
    static final int ENERGY_BASE_LIGHT = 0xFF8E92B0;

    /** Dark row of the empty half of an energy rail: a two-tone stripe, matching AE2's meter. */
    static final int ENERGY_BASE_DARK = 0xFF5E5E86;

    /** The mid tone of the empty half of an energy rail. */
    static final int ENERGY_BASE_MID = 0xFF4D4D67;

    /** Fill row of an energised rail. */
    static final int ENERGY_FILL_LIGHT = 0xFF915DCD;

    /** Bright fill row of an energised rail. */
    static final int ENERGY_FILL_BRIGHT = 0xFFB06FDD;

    /** Hot centre row of an energised rail. */
    static final int ENERGY_FILL_HOT = 0xFFFF80D7;

    /** Dark fill row of an energised rail. */
    static final int ENERGY_FILL_DARK = 0xFF5257A0;

    /** Mid fill row of an energised rail. */
    static final int ENERGY_FILL_MID = 0xFF6054A6;

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
