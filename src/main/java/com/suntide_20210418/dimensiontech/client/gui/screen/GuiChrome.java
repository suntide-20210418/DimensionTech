package com.suntide_20210418.dimensiontech.client.gui.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The shared bevelled-console primitives. Everything here is drawn with plain {@code fill} calls so
 * the chrome stays programmatic: no GUI atlas, no generated resources, and every edge stays crisp
 * at any UI scale.
 *
 * <p>The visual grammar is always the same: a raised surface carries a 1px black outline, a
 * top-left highlight, a bottom-right shadow, and — for windows — a dark title band with a 2px
 * accent rail that tells the player which data axis the panel belongs to.
 */
final class GuiChrome {
    /** Height of the dark title band across the top of a window. */
    static final int BAND_HEIGHT = 14;

    /** Width of the accent rail inside the title band. */
    static final int RAIL_WIDTH = 2;

    /** Thickness of the drop shadow on the bottom and right of a window. */
    static final int SHADOW = 2;

    /** Stride and run of one tick on a notched progress scale. */
    static final int TICK_STRIDE = 6;

    static final int TICK_RUN = 4;

    private GuiChrome() {}

    /**
     * A raised window: outline, face, drop shadow, dark title band and accent rail. Returns the
     * first pixel y of the content area below the band so callers can lay out without recomputing
     * it.
     */
    static int window(GuiGraphics g, int x, int y, int width, int height, int accent) {
        fill(g, x, y, width, height, GuiPalette.HAIRLINE);
        fill(g, x + 1, y + 1, width - 2, height - 2, GuiPalette.FACE);
        // Drop shadow on the two edges the player reads as "far".
        fill(g, x + SHADOW, y + height, width, SHADOW, GuiPalette.SHADE);
        fill(g, x + width, y + SHADOW * 2, SHADOW, height - SHADOW, GuiPalette.SHADE);
        return band(g, x + 1, y + 1, width - 2, accent);
    }

    /** The dark title band plus its accent rail. Returns the first content pixel y. */
    static int band(GuiGraphics g, int x, int y, int width, int accent) {
        fill(g, x, y, width, BAND_HEIGHT, GuiPalette.BAND);
        fill(g, x, y, RAIL_WIDTH, BAND_HEIGHT, accent);
        fill(g, x, y + BAND_HEIGHT, width - 1, 1, GuiPalette.HAIRLINE);
        return y + BAND_HEIGHT + 1;
    }

    /**
     * The dark title band with a left-aligned light title, for screens that need the machine name
     * inside the band rather than beside the tabs. Returns the first content pixel y.
     */
    static int titleBar(GuiGraphics g, Font font, int x, int y, int width, Component title) {
        fill(g, x, y, width, BAND_HEIGHT, GuiPalette.BAND);
        fill(g, x, y + BAND_HEIGHT, width - 1, 1, GuiPalette.HAIRLINE);
        g.drawString(font, title, x + 4, y + 3, GuiPalette.TEXT, false);
        return y + BAND_HEIGHT + 1;
    }

    /** A raised sub-panel inside a window: outline, face and one pixel of bevel. */
    static void subPanel(GuiGraphics g, int x, int y, int width, int height, int accent) {
        fill(g, x, y, width, height, GuiPalette.HAIRLINE);
        fill(g, x + 1, y + 1, width - 2, height - 2, GuiPalette.FACE);
        fill(g, x + 1, y + 1, width - 2, 1, GuiPalette.HILIGHT);
        fill(g, x + 1, y + 1, 1, height - 2, GuiPalette.HILIGHT);
        fill(g, x + 1, y + height - 2, width - 2, 1, GuiPalette.SHADE);
        fill(g, x + width - 2, y + 1, 1, height - 2, GuiPalette.SHADE);
        if (accent != 0) fill(g, x + 1, y + 1, 1, height - 2, accent);
    }

    /** A recessed well that keeps body text readable: medium grey, lit from the top-left. */
    static void well(GuiGraphics g, int x, int y, int width, int height) {
        fill(g, x, y, width, height, GuiPalette.WELL);
        fill(g, x, y, width, 1, GuiPalette.SHADE);
        fill(g, x, y, 1, height, GuiPalette.SHADE);
        fill(g, x, y + height - 1, width, 1, GuiPalette.HILIGHT);
        fill(g, x + width - 1, y, 1, height, GuiPalette.HILIGHT);
    }

    /**
     * A recessed well painted in the machine canvas' own green family, so it belongs to the recess
     * instead of reading as a grey panel dropped onto it.
     *
     * <p>The bevel is the same inverted one as {@link #well}: a sunken fill with its top-left edge in
     * shade and its bottom-right edge lit. The tones come from {@link GuiPalette#RECESS_PRESSED},
     * {@link GuiPalette#RECESS_EDGE} and {@link GuiPalette#RECESS_LIT} — the same three the catalogue
     * rows use — so an input field and the tree it filters look like one surface.
     */
    static void recessWell(GuiGraphics g, int x, int y, int width, int height) {
        fill(g, x, y, width, height, GuiPalette.RECESS_PRESSED);
        fill(g, x, y, width, 1, GuiPalette.RECESS_EDGE);
        fill(g, x, y, 1, height, GuiPalette.RECESS_EDGE);
        fill(g, x, y + height - 1, width, 1, GuiPalette.RECESS_LIT);
        fill(g, x + width - 1, y, 1, height, GuiPalette.RECESS_LIT);
    }

    /**
     * A 16x16 slot well, drawn entirely inside its own bounds so it can never bleed into a
     * neighbour's row or column. A selected slot lights its top-left edges instead of growing an
     * outer ring, because the marker grid has no room for one.
     */
    static void slotFace(GuiGraphics g, int x, int y, boolean selected, boolean disabled) {
        fill(g, x, y, 16, 16, GuiPalette.HAIRLINE);
        fill(g, x + 1, y + 1, 14, 14, GuiPalette.SLOT_FACE);
        int lit = selected ? GuiPalette.FLUIX : GuiPalette.SLOT_HILIGHT;
        fill(g, x + 1, y + 1, 14, 1, lit);
        fill(g, x + 1, y + 1, 1, 14, lit);
        fill(g, x + 1, y + 14, 14, 1, GuiPalette.SLOT_SHADE);
        fill(g, x + 14, y + 1, 1, 14, GuiPalette.SLOT_SHADE);
        if (disabled) fill(g, x, y, 16, 16, GuiPalette.DISABLED_OVERLAY);
    }

    /** A selectable tab: raised and accented when selected, flat and shaded otherwise. */
    static void tab(
            GuiGraphics g, Font font, int x, int y, int width, Component label, boolean selected) {
        if (selected) {
            fill(g, x, y, width, 13, GuiPalette.FACE);
            fill(g, x, y, 1, 13, GuiPalette.HILIGHT);
            fill(g, x + width - 1, y, 1, 13, GuiPalette.SHADE);
            fill(g, x + 1, y, width - 2, 2, GuiPalette.FLUIX);
            GuiText.centered(g, font, label, x + width / 2, y + 3, GuiPalette.INK);
            return;
        }
        fill(g, x, y, width, 13, GuiPalette.STRIPE_INK);
        fill(g, x, y, width, 1, GuiPalette.SHADE);
        GuiText.centered(g, font, label, x + width / 2, y + 3, GuiPalette.DIM);
    }

    /**
     * A raised control. {@code accent} becomes a one pixel rail down the control's left edge, so
     * buttons silently announce which data axis they belong to.
     */
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
        int face = hovered && enabled ? GuiPalette.HOVER : GuiPalette.FACE;
        fill(g, x, y, width, height, GuiPalette.HAIRLINE);
        fill(g, x + 1, y + 1, width - 2, height - 2, face);
        fill(g, x + 1, y + 1, width - 2, 2, enabled ? accent : GuiPalette.SHADE);
        fill(g, x + 2, y + 2, 1, height - 3, GuiPalette.HILIGHT);
        fill(g, x + 1, y + height - 2, width - 2, 1, GuiPalette.SHADE);
        fill(g, x + width - 2, y + 1, 1, height - 2, GuiPalette.SHADE);
        if (!enabled) fill(g, x, y, width, height, GuiPalette.DISABLED_OVERLAY);
        GuiText.centered(
                g,
                font,
                label,
                x + width / 2,
                y + (height - 8) / 2,
                enabled ? GuiPalette.INK : GuiPalette.DIM);
    }

    /**
     * A notched progress scale: alternating filled and empty ticks rather than a smooth gradient.
     * The tick immediately after the filled run is drawn in the "recess" tone so the leading edge
     * reads as a notch instead of a colour change.
     */
    static void progress(GuiGraphics g, int x, int y, int width, long value, long max, int accent) {
        fill(g, x, y, width, 3, GuiPalette.SHADE);
        int filled = max <= 0 ? 0 : (int) Math.max(0, Math.min(width, width * value / max));
        for (int offset = 0; offset < width; offset += TICK_STRIDE) {
            int tickWidth = Math.min(TICK_RUN, width - offset);
            if (offset < filled) {
                fill(g, x + offset, y + 1, tickWidth, 2, accent);
            } else if (offset < filled + TICK_STRIDE) {
                fill(g, x + offset, y, tickWidth, 3, GuiPalette.RECESS_ACTIVE);
            }
        }
    }

    /** A horizontal fine scale, used to anchor fluid columns and energy rails. */
    static void scale(GuiGraphics g, int x, int y, int length, int color) {
        fill(g, x, y, length, 1, color);
        for (int offset = 0; offset < length; offset += 4) fill(g, x + offset, y - 1, 1, 2, color);
    }

    /** A compact state chip for the title band. The text remains readable at every GUI scale. */
    static void statusChip(
            GuiGraphics g, Font font, int x, int y, int width, Component label, int accent) {
        fill(g, x, y, width, 12, GuiPalette.SLOT_FACE);
        fill(g, x, y, 2, 12, accent);
        fill(g, x + 2, y, width - 2, 1, GuiPalette.SHADE);
        fill(g, x + 2, y + 11, width - 2, 1, GuiPalette.HILIGHT);
        GuiText.centered(g, font, label, x + width / 2, y + 2, GuiPalette.TEXT);
    }

    /** A section label with the same one-pixel rule used by the reference terminal screens. */
    static void sectionHeader(
            GuiGraphics g, Font font, int x, int y, int width, Component label, int accent) {
        fill(g, x, y + 11, width, 1, GuiPalette.SHADE);
        fill(g, x, y + 11, Math.min(width, 18), 1, accent);
        g.drawString(font, label, x, y + 1, GuiPalette.INK, false);
    }

    /** A 14px catalogue row. Selection and hover never alter its geometry. */
    /**
     * A row of the structure tree, drawn straight onto the machine canvas.
     *
     * <p>An unselected row carries no fill at all. The canvas is already a recess, so filling every
     * row with a second one turned the list into a grey slab that did not belong to the panel.
     * Hovering lifts the row to the raised face and selecting presses it to the shaded edge, so the
     * three states read as one surface moving rather than as three unrelated colours — and the ink
     * stays dark across all of them, because light text only belonged to the old near-black fill.
     *
     * <p>The height is the caller's row pitch. It used to be a literal, which did not match the pitch
     * and left each row's fill overlapping the next one by a pixel.
     */
    static void listRow(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component label,
            boolean selected,
            boolean hovered,
            int accent) {
        if (selected) {
            fill(g, x, y, width, height, GuiPalette.RECESS_PRESSED);
            fill(g, x, y, 2, height, accent);
        } else if (hovered) {
            fill(g, x, y, width, height, GuiPalette.RECESS_LIT);
            fill(g, x, y, 2, height, GuiPalette.RECESS_EDGE);
        }
        int textY = y + (height - font.lineHeight) / 2;
        g.drawString(font, label, x + 6, textY, GuiPalette.INK, false);
    }

    /** A narrow recessed scrollbar with a deterministic thumb position. */
    static void scrollbar(
            GuiGraphics g,
            int x,
            int y,
            int height,
            int contentHeight,
            int viewportHeight,
            int scroll) {
        fill(g, x, y, 3, height, GuiPalette.SLOT_FACE);
        if (contentHeight <= viewportHeight || height <= 4) {
            fill(g, x, y, 3, height, GuiPalette.SHADE);
            return;
        }
        int thumb = Math.max(8, height * viewportHeight / contentHeight);
        int maxScroll = Math.max(1, contentHeight - viewportHeight);
        int thumbY = y + (height - thumb) * Math.max(0, Math.min(maxScroll, scroll)) / maxScroll;
        fill(g, x, thumbY, 3, thumb, GuiPalette.HILIGHT);
        fill(g, x + 1, thumbY + 1, 1, Math.max(1, thumb - 2), GuiPalette.SELECT);
    }

    /** A vertical fluid tank. The fill is clipped to the well and grows from the bottom. */
    static void tank(
            GuiGraphics g,
            int x,
            int y,
            int width,
            int height,
            long amount,
            long capacity,
            int fluidColor) {
        well(g, x, y, width, height);
        long clamped = capacity <= 0 ? 0 : Math.max(0, Math.min(capacity, amount));
        int fillHeight = capacity <= 0 ? 0 : (int) ((height - 4L) * clamped / capacity);
        if (fillHeight > 0)
            fill(g, x + 2, y + height - 2 - fillHeight, width - 4, fillHeight, fluidColor);
        scale(g, x - 2, y + height + 4, width + 4, GuiPalette.DIM);
    }

    /**
     * Draws the shared notched phase rail. The current node is highlighted without changing the
     * position of any neighbouring node, which keeps tooltips and click targets stable.
     */
    static void phaseRail(
            GuiGraphics g,
            int x,
            int y,
            int width,
            int nodeCount,
            int currentNode,
            int completedNode,
            int accent) {
        if (nodeCount <= 0) return;
        int lineY = y + 18;
        fill(g, x, lineY, width, 2, GuiPalette.SHADE);
        int spacing = nodeCount == 1 ? 0 : Math.max(1, (width - 8) / (nodeCount - 1));
        for (int index = 0; index < nodeCount; index++) {
            int nodeX = x + (nodeCount == 1 ? width / 2 : 4 + index * spacing);
            int color = index <= completedNode ? GuiPalette.SUCCESS : GuiPalette.SLOT_FACE;
            if (index == currentNode) color = accent;
            fill(g, nodeX - 4, lineY - 4, 9, 9, GuiPalette.HAIRLINE);
            fill(g, nodeX - 2, lineY - 2, 5, 5, color);
            if (index == currentNode) fill(g, nodeX - 3, lineY - 3, 7, 1, GuiPalette.HILIGHT);
        }
        for (int offset = 0; offset < width; offset += TICK_STRIDE)
            fill(g, x + offset, lineY + 4, Math.min(TICK_RUN, width - offset), 1, GuiPalette.DIM);
    }

    /** A neutral empty-state message centered in a viewport. */
    static void emptyState(
            GuiGraphics g, Font font, int x, int y, int width, int height, Component label) {
        GuiText.centered(
                g,
                font,
                label,
                x + width / 2,
                y + Math.max(0, height / 2 - 4),
                GuiPalette.DIM);
    }

    /** A modal confirmation panel with fixed button geometry. */
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
        subPanel(g, x, y, width, height, accent);
        GuiText.centered(g, font, title, x + width / 2, y + 5, GuiPalette.INK);
        GuiText.centered(g, font, message, x + width / 2, y + 25, GuiPalette.DIM);
        int buttonY = y + height - 22;
        button(g, font, x + 8, buttonY, 64, 18, cancel, false, true, GuiPalette.ERROR);
        button(g, font, x + width - 72, buttonY, 64, 18, confirm, confirmHovered, true, accent);
    }

    private static void fill(GuiGraphics g, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        g.fill(x, y, x + width, y + height, color);
    }
}
