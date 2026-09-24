package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared pixel geometry for the Structure Marker texture and screen.
 *
 * <p><b>Every number here was measured off {@code guis/structure_marker.png}, not copied from the
 * layout proposal.</b> The texture is 300x176 and the scan results it encodes are:
 *
 * <ul>
 *   <li>Frame: a 1px {@code #3F5444} contour, then a 1px {@code #F2F2F2} inner rule, then the
 *       {@code #CBD4CC} face from {@code (2,2)} to {@code (297,171)}. The bottom carries two extra
 *       rows of {@code #8AA587} above the contour — decorative, baked in, nothing draws there.
 *   <li>Viewport: white rule at {@code x=106}/{@code x=293} and {@code y=39}/{@code y=144}, with a
 *       one pixel {@code #9BB49A} shadow row at {@code y=40} and the fill {@code #ADC4AE}. That is
 *       the reactor's recess tone, <b>not</b> the operator's {@code #97B6A4} — the marker's window
 *       is the reactor's wide-flat shape, so it borrows that family. The scissor target is
 *       therefore {@code (107,40)} sized {@code 186x104}.
 * </ul>
 *
 * <p>The texture carries no title band, and that is not an omission: the reactor and the operator
 * console both draw their caption straight onto the face in ink rather than reserving a dark strip
 * for it. Only the miner, whose texture is a different shape entirely, has a band.
 */
public final class StructMarkerLayout {
    public static final ResourceLocation TEXTURE =
            ResourceLocationHelper.modLoc("guis/structure_marker.png");

    /**
     * Real file size. {@code blit} normalises UVs by whatever it is handed, so these must match.
     */
    public static final int TEXTURE_WIDTH = 300;

    public static final int TEXTURE_HEIGHT = 176;
    public static final int WIDTH = TEXTURE_WIDTH;
    public static final int HEIGHT = TEXTURE_HEIGHT;

    // --- caption -----------------------------------------------------------
    /** Caption origin. The face starts at x=2, so this leaves six pixels of margin. */
    public static final int TITLE_X = 8;

    public static final int TITLE_Y = 6;

    /**
     * The analysis-state chip, right-aligned against the face margin.
     *
     * <p>{@link #CHIP_W} is a floor rather than a size. "Approximate" — the longest of the four
     * state names — measures about 62 pixels in the vanilla font, so a fixed 40 pixel chip would
     * centre it two pixels past each of its own edges. This is the one control on the face whose
     * width depends on the language, so the screen computes it at draw time and this constant only
     * stops the shorter labels from making the chip look starved.
     */
    public static final int CHIP_W = 40;

    /** Blank pixels between the chip's label and either of its edges. */
    public static final int CHIP_PAD = 6;

    /** Blank face pixels between the contour and anything pinned to the panel's right edge. */
    public static final int FACE_MARGIN = 6;

    public static final int CHIP_Y = 5;

    // --- left column: the four readings, then the marker state --------------
    public static final int LEFT_X = 8;

    /**
     * The viewport's left rule sits at {@code x=106}. The column stops ten pixels short of that,
     * which is what keeps the widest reading ({@code 348.20}, 32px) clear of the viewport edge.
     */
    public static final int LEFT_W = 88;

    public static final int READING_Y = 22;

    /**
     * Row pitch of the readings.
     *
     * <p>The operator console stacks the same four readings at 13, but there they sit in a 152px
     * column beside a table. Here the column owns the whole left half of a 176px-tall face, so the
     * looser 18 is what stops the block from reading as cramped.
     */
    public static final int READING_ROW_H = 18;

    /** Rule separating the readings from the state block underneath them. */
    public static final int STATE_RULE_Y = 96;

    public static final int STATE_LINE_1_Y = 104;
    public static final int STATE_LINE_2_Y = 120;
    public static final int STATE_LINE_3_Y = 136;

    // --- right column: the expectation viewport -----------------------------
    /** Viewport origin and size, i.e. the recessed list area *inside* its white rule. */
    public static final int VIEWPORT_X = 107;

    public static final int VIEWPORT_Y = 40;

    public static final int VIEWPORT_W = 186;

    public static final int VIEWPORT_H = 104;

    /** Scissor target: the recessed viewport. The list draws in viewport-local coordinates. */
    public static final GuiRect VIEWPORT =
            new GuiRect(VIEWPORT_X, VIEWPORT_Y, VIEWPORT_W, VIEWPORT_H);

    /** The section caption sits above the viewport and is left-aligned with its rule. */
    public static final int SECTION_X = VIEWPORT_X;

    /**
     * Origin handed to {@code sectionHeader} — deliberately not the caption's own y.
     *
     * <p>That primitive draws its text at {@code y + 1} and underlines it at {@code y + 11}, so 21
     * puts the caption on 22 and the rule on 32. The texture's viewport rule is at 39, which leaves
     * six blank rows between the two. Passing 22 here instead would push both two rows down and
     * land the rule on the viewport's own contour.
     */
    public static final int SECTION_Y = 21;

    /** Section width: the viewport plus its own two rules. */
    public static final int SECTION_W = VIEWPORT_W;

    // --- expectation rows ---------------------------------------------------
    /** Row pitch. The viewport shows five whole rows and fourteen pixels of a sixth. */
    public static final int ROW_H = 18;

    /** Item icon size, and therefore the leading inset of every row. */
    public static final int ROW_ICON = 16;

    /** Left inset of a row's content inside the viewport. */
    public static final int ROW_PAD = 4;

    /** The name starts past the icon and a six pixel gutter. */
    public static final int ROW_NAME_X = ROW_PAD + ROW_ICON + 6;

    /** Gap between the content column and the scrollbar's grab zone. */
    public static final int SCROLLBAR_GAP = 2;

    /** Drawn width of the scrollbar. */
    public static final int SCROLLBAR_W = 3;

    /** Scrollbar x, in viewport-local coordinates. */
    public static final int SCROLLBAR_X = VIEWPORT_W - ROW_PAD - SCROLLBAR_W;

    /**
     * Right edge the expected-count column aligns to, in viewport-local coordinates.
     *
     * <p>Derived rather than typed so it follows the scrollbar if either number moves: values are
     * right-aligned against it with {@code x = right - font.width(value)}.
     */
    public static final int CONTENT_RIGHT = SCROLLBAR_X - SCROLLBAR_GAP;

    // --- action row ---------------------------------------------------------
    /**
     * Action row y.
     *
     * <p>The face ends at y=171 and the viewport at y=144, leaving 27 rows. Two 16px buttons are
     * centred in that band, which puts five blank rows above and below them.
     */
    public static final int ACTION_Y = 150;

    /** The 48x16 long-button sprite. Its visible face is x+1..x+46, i.e. 46 pixels. */
    public static final int ACTION_W = 48;

    public static final int ACTION_H = 16;

    /** Four pixels of face between the two buttons. */
    public static final int ACTION_GAP = 8;

    /** Left edge of the pair, centred on the face. */
    public static final int ACTION_FIRST_X = (WIDTH - (ACTION_W * 2 + ACTION_GAP)) / 2;

    public static final int ACTION_SECOND_X = ACTION_FIRST_X + ACTION_W + ACTION_GAP;

    private StructMarkerLayout() {}

    /** How many expectation rows the viewport shows whole. */
    public static int visibleRows() {
        return Math.max(1, VIEWPORT_H / ROW_H);
    }

    /** Y of the {@code index}-th expectation row, in viewport-local coordinates. */
    public static int rowY(int index) {
        return index * ROW_H;
    }
}
