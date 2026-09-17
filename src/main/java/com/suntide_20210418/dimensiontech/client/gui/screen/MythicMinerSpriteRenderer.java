package com.suntide_20210418.dimensiontech.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Sprite-sheet primitives used by the fixed-size miner console. */
final class MythicMinerSpriteRenderer {
    static final ResourceLocation SPRITESHEET =
            ResourceLocation.fromNamespaceAndPath("dimension_tech", "guis/spritesheet.png");
    static final int BUTTON_U = 0, BUTTON_V = 48, BUTTON_W = 32, BUTTON_H = 16;
    static final int PRESSED_U = 32, PRESSED_V = 48;
    static final int MARKER_U = 0, MARKER_V = 80, MARKER_W = 18, MARKER_H = 18;

    /**
     * Spritesheet (18,80)-(35,97): the same slot face with its outline lifted from {@code ADB0C4} to
     * {@code D0D1DE}. Selection is therefore a sprite swap, not something painted over the slot.
     */
    static final int MARKER_SELECTED_U = 18, MARKER_SELECTED_V = 80;
    static final int SMALL_U = 0, SMALL_HOVER_U = 20, SMALL_V = 98, SMALL_W = 20, SMALL_H = 20;

    /**
     * Spritesheet (0,118)-(19,137): the same 20x20 control in its pressed state, which is also how
     * this project marks "the current one" — the tab buttons use the same convention.
     */
    static final int SMALL_PRESSED_U = 0, SMALL_PRESSED_V = 118;

    static final int TEXTURE_SIZE = 160;

    /** Spritesheet (16,0)-(31,15): the "off" marker stacked onto the redstone torch. */
    static final int REDSTONE_OFF_U = 16, REDSTONE_OFF_V = 0;

    /**
     * Spritesheet (80,0)-(95,3): the energised strip. It is the filled half of the AE2-style meter
     * pair — its colours are byte-for-byte {@code ENERGY_FILL_*} in the palette.
     */
    static final int PROGRESS_FILL_U = 80, PROGRESS_FILL_V = 0;

    /**
     * Spritesheet (96,0)-(111,3): the drained track the fill covers, coloured from the palette's
     * {@code ENERGY_BASE_*} family.
     */
    static final int PROGRESS_TRACK_U = 96, PROGRESS_TRACK_V = 0;

    /** Both halves of the meter are 16x4 and share a vertical stripe phase at u. */
    static final int PROGRESS_W = 16, PROGRESS_H = 4;

    private MythicMinerSpriteRenderer() {}

    static void button(GuiGraphics g, int x, int y, boolean pressed) {
        g.blit(
                SPRITESHEET,
                x,
                y,
                0,
                pressed ? PRESSED_U : BUTTON_U,
                BUTTON_V,
                BUTTON_W,
                BUTTON_H,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
    }

    static void marker(GuiGraphics g, int x, int y) {
        marker(g, x, y, false);
    }

    /** Draws the 18x18 slot face in its normal or selected variant. */
    static void marker(GuiGraphics g, int x, int y, boolean selected) {
        g.blit(
                SPRITESHEET,
                x,
                y,
                0,
                selected ? MARKER_SELECTED_U : MARKER_U,
                MARKER_V,
                MARKER_W,
                MARKER_H,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
    }

    static void smallButton(GuiGraphics g, int x, int y, boolean hovered) {
        smallButton(g, x, y, hovered, false);
    }

    /**
     * A 20x20 control in one of its three drawn states: pressed, hover-lightened, or idle.
     *
     * <p>{@code pressed} wins over {@code hovered}, so the control that is currently selected keeps
     * reading as selected while the pointer is over it.
     */
    static void smallButton(GuiGraphics g, int x, int y, boolean hovered, boolean pressed) {
        int u = pressed ? SMALL_PRESSED_U : (hovered ? SMALL_HOVER_U : SMALL_U);
        int v = pressed ? SMALL_PRESSED_V : SMALL_V;
        g.blit(SPRITESHEET, x, y, 0, u, v, SMALL_W, SMALL_H, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    static void externalIcon(GuiGraphics g, int x, int y, int u, int v) {
        g.blit(SPRITESHEET, x, y, 0, u, v, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /**
     * A marker progress strip: the drained track with the energised half clipped over it from the
     * left, {@code filled} pixels wide.
     *
     * <p>The clipping works because {@code blit}'s {@code width} argument is both how much texture
     * is taken from {@code u} and how much screen is covered, so taking the first N columns and
     * landing them on the first N pixels is a crop, not a scale. That matters here: the strip is a
     * 2px-period stripe pattern, and any non-integer stretching would smear it.
     *
     * <p>Do not reach for {@link GuiChrome#progress}: that primitive scales {@code value / max} over
     * an arbitrary width and overlays its own notch pattern, which would draw something other than
     * this sprite at a height the sprite does not have.
     */
    static void progressStrip(GuiGraphics g, int x, int y, int filled) {
        g.blit(
                SPRITESHEET,
                x,
                y,
                0,
                PROGRESS_TRACK_U,
                PROGRESS_TRACK_V,
                PROGRESS_W,
                PROGRESS_H,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
        int clipped = Math.max(0, Math.min(PROGRESS_W, filled));
        if (clipped == 0) return;
        g.blit(
                SPRITESHEET,
                x,
                y,
                0,
                PROGRESS_FILL_U,
                PROGRESS_FILL_V,
                clipped,
                PROGRESS_H,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
    }
}
