package com.suntide_20210418.dimensiontech.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Sprite-sheet primitives used by the fixed-size miner console. */
final class StructureMinerSpriteRenderer {
    static final ResourceLocation SPRITESHEET =
            ResourceLocation.fromNamespaceAndPath("dimension_tech", "guis/spritesheet.png");
    static final int BUTTON_U = 0, BUTTON_V = 48, BUTTON_W = 32, BUTTON_H = 16;
    static final int PRESSED_U = 32, PRESSED_V = 48;
    static final int MARKER_U = 0, MARKER_V = 80, MARKER_W = 18, MARKER_H = 18;

    /**
     * Spritesheet (18,80)-(35,97): the same slot face with its outline lifted from {@code ADB0C4}
     * to {@code D0D1DE}. Selection is therefore a sprite swap, not something painted over the slot.
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

    /**
     * A rectangular region of a texture, tagged with the file's real size.
     *
     * <p>The size rides along because {@code blit} normalises UVs by whatever it is handed: hand it
     * a width two pixels off the file and every region silently samples about a percent off, which
     * is how {@code StructureMinerScreen}'s energy strip lost its last column to the panel and its
     * fluid overlay crept into the strip.
     */
    record Fragment(
            ResourceLocation texture,
            int textureWidth,
            int textureHeight,
            int u,
            int v,
            int width,
            int height) {}

    /**
     * The two directions {@link #stitch} can lay copies out: left to right (左右) or top to bottom
     * (上下).
     */
    enum StitchDirection {
        HORIZONTAL(true),
        VERTICAL(false);

        private final boolean alongX;

        StitchDirection(boolean alongX) {
            this.alongX = alongX;
        }

        /** True when advancing copies step along x rather than y. */
        boolean alongX() {
            return alongX;
        }

        /** Pixels one whole copy occupies along this direction. */
        int span(Fragment fragment) {
            return alongX ? fragment.width() : fragment.height();
        }
    }

    /**
     * A cover is committed one z-step above whatever it covers, so its placement does not depend on
     * the background landing in the same batch in the order the two were issued.
     */
    private static final int COVER_BLIT_OFFSET = 1;

    /** The drained track and its energised cover, packaged for {@link #stitch}. */
    static final Fragment PROGRESS_TRACK_FRAGMENT = fragment(PROGRESS_TRACK_U, PROGRESS_TRACK_V);

    static final Fragment PROGRESS_FILL_FRAGMENT = fragment(PROGRESS_FILL_U, PROGRESS_FILL_V);

    /**
     * Lays {@code copies} whole copies of {@code fragment} end to end starting at {@code (x, y)},
     * first copy at the rectangle's leading edge.
     *
     * <p>Nothing is clipped, so the run is exactly {@code copies * span} pixels long. Use this when
     * the caller thinks in tiles — a lane of nine cells, a column of four wells. Use {@link
     * #stitchTo} when the caller thinks in pixels, which is what every caller here does.
     *
     * @param overlay true when the run covers something already drawn this frame; the copies are
     *     then raised one z-step so the cover cannot end up behind what it covers. Background and
     *     cover differ only in that: both are anchored at the leading edge and both stop where the
     *     caller says.
     */
    static void stitch(
            GuiGraphics g,
            Fragment fragment,
            int x,
            int y,
            StitchDirection direction,
            int copies,
            boolean overlay) {
        drawCopies(g, fragment, x, y, direction, Math.max(0, copies), 0, overlay);
    }

    /**
     * The same tiling, covering exactly {@code length} pixels: the trailing copy is clipped to the
     * remainder, so the run ends flush on {@code length} instead of overshooting it.
     *
     * <p>Clipping rather than scaling is the whole point. {@code blit}'s {@code width} argument is
     * simultaneously how much texture is taken from {@code u} and how much screen is covered, so
     * drawing a shorter leading run of the same columns is a crop — the fragment keeps its own
     * pixel pitch. The strips are a 2px-period pattern; any non-integer scaling would smear them.
     * Vanilla ships {@code GuiGraphics#blitRepeating} for the "fill this rectangle" case, but it
     * takes no repeat count, no direction, and no cover layer.
     */
    static void stitchTo(
            GuiGraphics g,
            Fragment fragment,
            int x,
            int y,
            StitchDirection direction,
            int length,
            boolean overlay) {
        int span = direction.span(fragment);
        if (length <= 0 || span <= 0) return;
        drawCopies(g, fragment, x, y, direction, length / span, length % span, overlay);
    }

    /**
     * Draws {@code wholeCopies} full tiles, then one further copy trimmed to {@code trailingPixels}
     * along the stitch axis — skipped entirely when that is zero.
     */
    private static void drawCopies(
            GuiGraphics g,
            Fragment fragment,
            int x,
            int y,
            StitchDirection direction,
            int wholeCopies,
            int trailingPixels,
            boolean overlay) {
        int z = overlay ? COVER_BLIT_OFFSET : 0;
        int span = direction.span(fragment);
        for (int index = 0; index < wholeCopies; index++) {
            int step = index * span;
            int tileX = direction.alongX() ? x + step : x;
            int tileY = direction.alongX() ? y : y + step;
            blitTile(g, fragment, tileX, tileY, fragment.width(), fragment.height(), z);
        }
        if (trailingPixels <= 0) return;
        int step = wholeCopies * span;
        int tileX = direction.alongX() ? x + step : x;
        int tileY = direction.alongX() ? y : y + step;
        // The trailing copy is cropped along the stitch axis only; the off axis stays full size.
        int width = direction.alongX() ? trailingPixels : fragment.width();
        int height = direction.alongX() ? fragment.height() : trailingPixels;
        blitTile(g, fragment, tileX, tileY, width, height, z);
    }

    private static void blitTile(
            GuiGraphics g, Fragment fragment, int x, int y, int width, int height, int z) {
        g.blit(
                fragment.texture(),
                x,
                y,
                z,
                fragment.u(),
                fragment.v(),
                width,
                height,
                fragment.textureWidth(),
                fragment.textureHeight());
    }

    /**
     * One strip fragment off {@link #SPRITESHEET}, at its shared {@link #PROGRESS_W}x{@link
     * #PROGRESS_H} size.
     */
    private static Fragment fragment(int u, int v) {
        return new Fragment(SPRITESHEET, TEXTURE_SIZE, TEXTURE_SIZE, u, v, PROGRESS_W, PROGRESS_H);
    }

    /**
     * The 32x16 action button, idle or lit.
     *
     * <p>The sheet draws only these two states, so a hovered control passes {@code lit} — the same
     * pointer feedback the 20x20 controls give, which have a third sprite to spare for it.
     */
    static void button(GuiGraphics g, int x, int y, boolean lit) {
        g.blit(
                SPRITESHEET,
                x,
                y,
                0,
                lit ? PRESSED_U : BUTTON_U,
                BUTTON_V,
                BUTTON_W,
                BUTTON_H,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
    }

    /** Spritesheet (0,144)-(47,159) and (48,144)-(95,159). */
    static final int LONG_BUTTON_U = 0, LONG_BUTTON_V = 144, LONG_BUTTON_W = 48, LONG_BUTTON_H = 16;

    static final int LONG_PRESSED_U = 48;

    /**
     * The 48x16 tab button, in the same two states as {@link #button}.
     *
     * <p>Decoded from the sheet, the idle sprite is a {@code #3F4054} outline over a {@code
     * #9EAFAA} face with a {@code #B0C0BF} top row, and the lit sprite drops that row to a
     * transparent pixel and lifts the face to {@code #B5E6C6} — byte for byte the relationship the
     * 32x16 pair has, which is what lets the two sizes read as one family.
     */
    static void longButton(GuiGraphics g, int x, int y, boolean lit) {
        g.blit(
                SPRITESHEET,
                x,
                y,
                0,
                lit ? LONG_PRESSED_U : LONG_BUTTON_U,
                LONG_BUTTON_V,
                LONG_BUTTON_W,
                LONG_BUTTON_H,
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
     * with the energised cover over the first {@code filled} pixels.
     *
     * <p>Both halves come from one 16x4 fragment each, so length is a parameter rather than a
     * second asset — see {@link #stitchTo}. The cover is laid left to right and nothing else: a bar
     * that grew from the right would read as progress running backwards.
     *
     * <p>Do not reach for {@link GuiChrome#progress}: that primitive scales {@code value / max}
     * over an arbitrary width and overlays its own notch pattern, which would draw something other
     * than this sprite at a height the sprite does not have.
     */
    static void progressStrip(GuiGraphics g, int x, int y, int length, int filled) {
        int bounded = Math.max(0, length);
        if (bounded == 0) return;
        stitchTo(g, PROGRESS_TRACK_FRAGMENT, x, y, StitchDirection.HORIZONTAL, bounded, false);
        int clipped = Math.max(0, Math.min(bounded, filled));
        if (clipped == 0) return;
        stitchTo(g, PROGRESS_FILL_FRAGMENT, x, y, StitchDirection.HORIZONTAL, clipped, true);
    }
}
