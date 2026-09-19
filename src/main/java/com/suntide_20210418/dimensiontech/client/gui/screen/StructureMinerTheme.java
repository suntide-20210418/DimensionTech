package com.suntide_20210418.dimensiontech.client.gui.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Shared instrument-panel palette and primitives for every machine screen.
 *
 * <p>The colour values live in {@link GuiPalette} and the bevelled drawing lives in {@link
 * GuiChrome}; this class is the single name the screens talk to. Constant names are load-bearing
 * and deliberately stable: {@code FRAME}, {@code PANEL} and {@code INSET} read as "structure",
 * while {@code TEXT} and {@code MUTED} read as "legacy light ink" and are therefore reserved for
 * the dark title band and the recessed {@code WELL} — the light {@code FACE} uses {@link
 * GuiPalette#INK} and {@link GuiPalette#DIM} instead. Mixing those two families is what a contrast
 * test guards against.
 */
final class StructureMinerTheme {
    // --- structure ---------------------------------------------------------
    static final int FRAME = GuiPalette.BAND;
    static final int PANEL = GuiPalette.FACE;
    static final int INSET = GuiPalette.WELL;
    static final int EDGE = GuiPalette.HAIRLINE;
    static final int HAIRLINE = GuiPalette.HAIRLINE;
    static final int SLOT_FACE = GuiPalette.SLOT_FACE;
    static final int SLOT_HILIGHT = GuiPalette.SLOT_HILIGHT;
    static final int SLOT_SHADE = GuiPalette.SLOT_SHADE;
    static final int BAND = GuiPalette.BAND;
    static final int BAND_HEIGHT = GuiChrome.BAND_HEIGHT;
    static final int STRIPE_INK = GuiPalette.STRIPE_INK;
    static final int STRIPE_WELL = GuiPalette.STRIPE_WELL;
    static final int PROGRESS_TRACK = GuiPalette.PROGRESS_TRACK;
    static final int HILIGHT = GuiPalette.HILIGHT;
    static final int SHADE = GuiPalette.SHADE;
    static final int SELECT = GuiPalette.SELECT;

    // --- ink ---------------------------------------------------------------
    static final int INK = GuiPalette.INK;
    static final int DIM = GuiPalette.DIM;
    static final int TEXT = GuiPalette.TEXT;
    static final int MUTED = GuiPalette.MUTED;

    // --- data accents ------------------------------------------------------
    static final int FLUIX = GuiPalette.FLUIX;
    static final int AMBER = GuiPalette.AMBER;
    static final int ERROR = GuiPalette.ERROR;
    static final int SUCCESS = GuiPalette.SUCCESS;
    static final int WELL_AMBER = GuiPalette.WELL_AMBER;
    static final int WELL_FLUIX = GuiPalette.WELL_FLUIX;
    static final int WELL_SUCCESS = GuiPalette.WELL_SUCCESS;
    static final int WELL_ERROR = GuiPalette.WELL_ERROR;

    static final int SLOT_HIGHLIGHT = GuiPalette.HILIGHT;
    static final int FRAME_HIGHLIGHT = GuiPalette.BEVEL_MID;
    static final int HOVER = GuiPalette.HOVER;
    static final int DISABLED_OVERLAY = GuiPalette.DISABLED_OVERLAY;
    static final int BACKDROP = GuiPalette.BACKDROP;
    static final int FLUID = GuiPalette.FLUID;
    static final int FLUID_ACCENT = GuiPalette.FLUID_ACCENT;
    // AE2 energy meters use a repeating two-row pixel pattern, not a gradient.
    static final int ENERGY_BORDER = GuiPalette.ENERGY_BORDER;
    static final int ENERGY_TICK = GuiPalette.ENERGY_TICK;
    static final int ENERGY_BASE_LIGHT = GuiPalette.ENERGY_BASE_LIGHT;
    static final int ENERGY_BASE_DARK = GuiPalette.ENERGY_BASE_DARK;
    static final int ENERGY_BASE_MID = GuiPalette.ENERGY_BASE_MID;
    static final int ENERGY_FILL_LIGHT = GuiPalette.ENERGY_FILL_LIGHT;
    static final int ENERGY_FILL_BRIGHT = GuiPalette.ENERGY_FILL_BRIGHT;
    static final int ENERGY_FILL_HOT = GuiPalette.ENERGY_FILL_HOT;
    static final int ENERGY_FILL_DARK = GuiPalette.ENERGY_FILL_DARK;
    static final int ENERGY_FILL_MID = GuiPalette.ENERGY_FILL_MID;

    private StructureMinerTheme() {}

    /** A raised window with a dark title band. Returns the first content pixel y. */
    static int panel(GuiGraphics g, int x, int y, int width, int height, int accent) {
        return GuiChrome.window(g, x, y, width, height, accent);
    }

    /** The dark title band with a left-aligned title. Returns the first content pixel y. */
    static int titleBar(GuiGraphics g, Font font, int x, int y, int width, Component title) {
        return GuiChrome.titleBar(g, font, x, y, width, title);
    }

    /** A raised sub-panel inside a window. */
    static void subPanel(GuiGraphics g, int x, int y, int width, int height, int accent) {
        GuiChrome.subPanel(g, x, y, width, height, accent);
    }

    /** A recessed well; body text drawn on it is light, so it uses {@link #TEXT}/{@link #MUTED}. */
    static void well(GuiGraphics g, int x, int y, int width, int height) {
        GuiChrome.well(g, x, y, width, height);
    }

    /** A 16x16 slot well, drawn strictly inside its own bounds. */
    static void slot(GuiGraphics g, int x, int y, boolean selected, boolean disabled) {
        GuiChrome.slotFace(g, x, y, selected, disabled);
    }

    /** A value cell on a light face. */
    static void metricCard(GuiGraphics g, int x, int y, int width, int height, int accent) {
        GuiChrome.metricCard(g, x, y, width, height, accent);
    }

    static void tab(
            GuiGraphics g, Font font, int x, int y, int width, Component label, boolean selected) {
        GuiChrome.tab(g, font, x, y, width, label, selected);
    }

    static void button(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component label,
            boolean hovered,
            boolean enabled,
            int accent) {
        GuiChrome.button(g, font, x, y, width, height, label, hovered, enabled, accent);
    }

    static void progress(GuiGraphics g, int x, int y, int width, long value, long max, int accent) {
        GuiChrome.progress(g, x, y, width, value, max, accent);
    }

    /** A fine measurement scale. */
    static void scale(GuiGraphics g, int x, int y, int length, int color) {
        GuiChrome.scale(g, x, y, length, color);
    }

    static void statusChip(
            GuiGraphics g, Font font, int x, int y, int width, Component label, int accent) {
        GuiChrome.statusChip(g, font, x, y, width, label, accent);
    }

    static void sectionHeader(
            GuiGraphics g, Font font, int x, int y, int width, Component label, int accent) {
        GuiChrome.sectionHeader(g, font, x, y, width, label, accent);
    }

    static void listRow(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int width,
            Component label,
            boolean selected,
            boolean hovered,
            int accent) {
        GuiChrome.listRow(g, font, x, y, width, label, selected, hovered, accent);
    }

    static void scrollbar(
            GuiGraphics g,
            int x,
            int y,
            int height,
            int contentHeight,
            int viewportHeight,
            int scroll) {
        GuiChrome.scrollbar(g, x, y, height, contentHeight, viewportHeight, scroll);
    }

    static void tank(
            GuiGraphics g,
            int x,
            int y,
            int width,
            int height,
            long amount,
            long capacity,
            int fluidColor) {
        GuiChrome.tank(g, x, y, width, height, amount, capacity, fluidColor);
    }

    static void phaseRail(
            GuiGraphics g,
            int x,
            int y,
            int width,
            int nodeCount,
            int currentNode,
            int completedNode,
            int accent) {
        GuiChrome.phaseRail(g, x, y, width, nodeCount, currentNode, completedNode, accent);
    }

    static void emptyState(
            GuiGraphics g, Font font, int x, int y, int width, int height, Component label) {
        GuiChrome.emptyState(g, font, x, y, width, height, label);
    }

    static void confirmationModal(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component title,
            Component message,
            Component cancel,
            Component confirm,
            boolean confirmHovered,
            int accent) {
        GuiChrome.confirmationModal(
                g,
                font,
                x,
                y,
                width,
                height,
                title,
                message,
                cancel,
                confirm,
                confirmHovered,
                accent);
    }
}
