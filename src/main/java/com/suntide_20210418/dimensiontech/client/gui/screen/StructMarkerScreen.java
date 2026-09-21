package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructMarkerLayout;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The structure marker terminal: the handheld console that reads and writes one marker's dossier.
 *
 * <p><b>Fixed frame, one viewport.</b> The texture carries the whole frame — contour, inner rule,
 * face and the recessed expectation viewport — so this screen only blits it and then draws text,
 * icons and controls on top. Every coordinate comes from {@link StructMarkerLayout}, which was
 * measured off the texture. Nothing here re-derives geometry from the panel size, because a marker
 * screen that resized itself would need a second set of slot coordinates and the authored art does
 * not support one.
 *
 * <p><b>Two states, one overlay.</b> Normally the screen shows the marker's readings down the left
 * column and its loot expectation in the right viewport. When the server answers a mark request
 * with several overlapping structures, a choice overlay covers the screen instead of opening a
 * second window — the choices arrive asynchronously, and a modal would have to keep two selections
 * in sync where an overlay needs only one index.
 *
 * <p><b>No container.</b> This is a {@link Screen}, not an {@code AbstractContainerScreen}: the
 * marker is held in the player's hand and the terminal has no slots, so there is no menu to attach.
 */
public final class StructMarkerScreen extends Screen {
    /**
     * Row hover tone, sampled off the texture as {@code #9BB49A} — the viewport's own top shadow
     * row. A hovered expectation row is pressed in with it rather than lifted with a lighter tone,
     * because every lighter candidate in the recess family is within a couple of points of the
     * {@code #ADC4AE} fill and would not read at all.
     */
    private static final int ROW_HOVER = 0xFF9BB49A;

    /** Rows the choice overlay will show before it starts scrolling. */
    private static final int CHOICE_MAX_VISIBLE = 8;

    private static final int CHOICE_ROW_H = 20;
    private static final int CHOICE_BOX_W = 224;

    /** Blank face rows between the overlay's title band and its first choice. */
    private static final int CHOICE_TOP_PAD = 6;

    /** Blank face rows below the last choice. */
    private static final int CHOICE_BOTTOM_PAD = 8;

    private final InteractionHand hand;

    private ItemStack marker;
    private final List<Row> rows = new ArrayList<>();

    private List<StructMarkerItem.MarkedStructure> choices = List.of();
    private BlockPos choicePosition = BlockPos.ZERO;
    private int choiceScroll;

    private int left;
    private int top;
    private int scroll;
    private int hoveredRow = -1;

    public StructMarkerScreen(ItemStack marker, InteractionHand hand) {
        super(Component.translatable("screen.dimension_tech.struct_marker.title"));
        this.marker = marker;
        this.hand = hand;
    }

    /** True when this screen is already showing the given hand, i.e. it can be reused in place. */
    public boolean holds(InteractionHand requestedHand) {
        return requestedHand == hand;
    }

    /**
     * Re-reads a marker the server has just pushed.
     *
     * <p>Refreshing beats replacing the screen: marking a structure round-trips through the server
     * and comes back as a fresh stack, and rebuilding the screen for it would throw away the scroll
     * position and flash the background on every press of the action button.
     */
    public void refresh(ItemStack updated) {
        marker = updated;
        rebuildRows();
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    @Override
    protected void init() {
        left = (width - StructMarkerLayout.WIDTH) / 2;
        top = (height - StructMarkerLayout.HEIGHT) / 2;
        rebuildRows();
    }

    /**
     * The expectation rows, heaviest first.
     *
     * <p>This screen has no sortable header — the design puts the viewport straight under its
     * caption, with no room for a column row — so the order it ships with is the only order the
     * player will see. Expected yield descending is the useful one: the question being asked of a
     * structure is what it drops most of, and answer is the first line rather than a hunt.
     */
    private void rebuildRows() {
        rows.clear();
        for (Map.Entry<ResourceLocation, ExactProbability> entry :
                StructMarkerItem.getExpectedItemCounts(marker).entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item != null) rows.add(new Row(item, entry.getValue()));
        }
        rows.sort(
                Comparator.comparingDouble((Row row) -> row.expected.finiteDoubleValue())
                        .reversed()
                        .thenComparing(row -> BuiltInRegistries.ITEM.getKey(row.item).toString()));
    }

    /**
     * This screen does not pause the game.
     *
     * <p>It reads the marker in the player's hand and writes back to it, so it is a console rather
     * than a menu — the same stance the miner and the operator console take, both of which are
     * container screens and therefore never paused either.
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        /*
         * Screen#render only walks the child widgets, it does not paint a background — that is
         * AbstractContainerScreen#render's job, not this one's. So the dim has to be asked for
         * explicitly here; without it this screen stayed fully transparent and the world showed
         * straight through the panel. It is drawn before super.render so the widgets land on top
         * of it, and the panel is drawn after so it lands on top of both.
         */
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        int windowX = mouseX - left;
        int windowY = mouseY - top;
        hoveredRow = choices.isEmpty() ? rowAt(mouseX, mouseY) : -1;

        g.pose().pushPose();
        g.pose().translate(left, top, 0.0D);
        drawFace(g);
        drawCaption(g);
        drawLeftColumn(g);
        drawExpectationSection(g, windowX, windowY);
        drawActions(g, windowX, windowY);
        g.pose().popPose();

        /*
         * The hover pass runs here, after the pose is back to identity and every scissor has been
         * closed, because renderTooltip draws under the current pose and leaves the scissor alone:
         * calling it from inside the viewport draw would offset the box by the canvas origin and
         * crop it at the viewport edge.
         */
        if (choices.isEmpty() && hoveredRow >= 0) {
            g.renderTooltip(font, new ItemStack(rows.get(hoveredRow).item), mouseX, mouseY);
        }
        if (!choices.isEmpty()) drawChoices(g, mouseX, mouseY);
    }

    private void drawFace(GuiGraphics g) {
        g.blit(
                StructMarkerLayout.TEXTURE,
                0,
                0,
                0,
                0,
                StructMarkerLayout.WIDTH,
                StructMarkerLayout.HEIGHT,
                StructMarkerLayout.TEXTURE_WIDTH,
                StructMarkerLayout.TEXTURE_HEIGHT);
    }

    /**
     * The caption and the analysis-state chip.
     *
     * <p>The caption is ink on the face, not light text on a band: this texture has no band, which
     * is the same arrangement the reactor and the operator console use.
     */
    private void drawCaption(GuiGraphics g) {
        g.drawString(
                font,
                Component.translatable("screen.dimension_tech.struct_marker.title"),
                StructMarkerLayout.TITLE_X,
                StructMarkerLayout.TITLE_Y,
                StructureMinerTheme.INK,
                false);

        /*
         * The chip's width follows its label: the state names are not the same length in every
         * language, and a fixed width would push the longest one past the chip's own edges.
         */
        AnalysisStatus status = StructMarkerItem.getAnalysisStatus(marker);
        Component label = statusLabel(status);
        int chipWidth =
                Math.max(
                        StructMarkerLayout.CHIP_W,
                        font.width(label) + 2 * StructMarkerLayout.CHIP_PAD);
        StructureMinerTheme.statusChip(
                g,
                font,
                StructMarkerLayout.WIDTH
                        - 2
                        - StructMarkerLayout.FACE_MARGIN
                        - chipWidth,
                StructMarkerLayout.CHIP_Y,
                chipWidth,
                label,
                stateAccent(status));
    }

    /** The four readings, the rule under them, and the three state lines below it. */
    private void drawLeftColumn(GuiGraphics g) {
        StructMarkerItem.MarkerInfo info = StructMarkerItem.getMarkerInfo(marker).orElse(null);
        StructureDataOperatorReadings.draw(
                g,
                font,
                StructMarkerLayout.LEFT_X,
                StructMarkerLayout.READING_Y,
                StructMarkerLayout.LEFT_W,
                StructMarkerLayout.READING_ROW_H,
                marker,
                info == null ? null : info.dimension(),
                info == null ? null : info.structure().id());

        g.fill(
                StructMarkerLayout.LEFT_X,
                StructMarkerLayout.STATE_RULE_Y,
                StructMarkerLayout.LEFT_X + StructMarkerLayout.LEFT_W,
                StructMarkerLayout.STATE_RULE_Y + 1,
                StructureMinerTheme.SHADE);

        AnalysisStatus status = StructMarkerItem.getAnalysisStatus(marker);
        g.drawString(
                font,
                Component.translatable(
                        info == null
                                ? "screen.dimension_tech.struct_marker.selection.empty"
                                : "screen.dimension_tech.struct_marker.selection.selected"),
                StructMarkerLayout.LEFT_X,
                StructMarkerLayout.STATE_LINE_1_Y,
                info == null ? StructureMinerTheme.DIM : StructureMinerTheme.SUCCESS,
                false);

        g.drawString(
                font,
                Component.translatable(
                        "screen.dimension_tech.struct_marker.calculation_method",
                        statusLabel(status)),
                StructMarkerLayout.LEFT_X,
                StructMarkerLayout.STATE_LINE_2_Y,
                stateAccent(status),
                false);

        /*
         * The coordinates are printed raw. There is no translation for an "x, y, z" triplet because
         * there is nothing to translate: the separator and the sign are the whole format, and the
         * player reads them the same way in every language.
         */
        if (info != null) {
            BlockPos position = info.position();
            g.drawString(
                    font,
                    position.getX() + ", " + position.getY() + ", " + position.getZ(),
                    StructMarkerLayout.LEFT_X,
                    StructMarkerLayout.STATE_LINE_3_Y,
                    StructureMinerTheme.DIM,
                    false);
        }
    }

    /**
     * The section caption, its rule, and the scrolling expectation viewport.
     *
     * <p>{@code sectionHeader} draws the caption and underlines it in a single call — the text lands
     * on {@code SECTION_Y + 1} and the rule on {@code SECTION_Y + 11} — so nothing here paints a
     * second rule one row under the first.
     */
    private void drawExpectationSection(GuiGraphics g, int windowX, int windowY) {
        StructureMinerTheme.sectionHeader(
                g,
                font,
                StructMarkerLayout.SECTION_X,
                StructMarkerLayout.SECTION_Y,
                StructMarkerLayout.SECTION_W,
                Component.translatable("screen.dimension_tech.struct_marker.items_heading"),
                StructureMinerTheme.FLUIX);

        /* The count shares the caption's own line, one row below the header's origin. */
        String count =
                Component.translatable(
                                "screen.dimension_tech.struct_marker.item_count", rows.size())
                        .getString();
        g.drawString(
                font,
                count,
                StructMarkerLayout.SECTION_X + StructMarkerLayout.SECTION_W - font.width(count),
                StructMarkerLayout.SECTION_Y + 1,
                StructureMinerTheme.INK,
                false);

        drawViewport(g);
    }

    /**
     * Clips to the viewport, translates to its origin, and draws the rows.
     *
     * <p>{@code enableScissor} takes GUI-logical coordinates and applies the window's GUI scale
     * itself, so the rectangle is handed over in absolute logical pixels — the panel origin plus the
     * viewport's own offset — and never multiplied by a scale first. This screen does not scale its
     * pose at all: the frame is a fixed 300x176, which fits the 480x270 minimum, so there is no
     * second scale to fold in.
     */
    private void drawViewport(GuiGraphics g) {
        g.enableScissor(
                left + StructMarkerLayout.VIEWPORT.x(),
                top + StructMarkerLayout.VIEWPORT.y(),
                left + StructMarkerLayout.VIEWPORT.right(),
                top + StructMarkerLayout.VIEWPORT.bottom());
        g.pose().pushPose();
        g.pose()
                .translate(
                        StructMarkerLayout.VIEWPORT.x(), StructMarkerLayout.VIEWPORT.y(), 0.0D);

        if (rows.isEmpty()) {
            StructureMinerTheme.emptyState(
                    g,
                    font,
                    0,
                    0,
                    StructMarkerLayout.VIEWPORT_W,
                    StructMarkerLayout.VIEWPORT_H,
                    Component.translatable("screen.dimension_tech.struct_marker.no_items"));
        } else {
            int visible = Math.min(scroll, Math.max(0, rows.size() - StructMarkerLayout.visibleRows()));
            for (int index = visible; index < rows.size(); index++) {
                int y = StructMarkerLayout.rowY(index - visible);
                if (y >= StructMarkerLayout.VIEWPORT_H) break;
                drawRow(g, index, y);
            }
        }

        g.pose().popPose();
        g.disableScissor();

        StructureMinerTheme.scrollbar(
                g,
                StructMarkerLayout.VIEWPORT.x() + StructMarkerLayout.SCROLLBAR_X,
                StructMarkerLayout.VIEWPORT.y(),
                StructMarkerLayout.VIEWPORT_H,
                rows.size() * StructMarkerLayout.ROW_H,
                StructMarkerLayout.VIEWPORT_H,
                scroll * StructMarkerLayout.ROW_H);
    }

    /** One expectation row: item icon, name, and the expected count right-aligned against it. */
    private void drawRow(GuiGraphics g, int index, int y) {
        Row row = rows.get(index);
        if (index == hoveredRow) {
            g.fill(0, y, StructMarkerLayout.VIEWPORT_W, y + StructMarkerLayout.ROW_H, ROW_HOVER);
        }

        ItemStack stack = new ItemStack(row.item);
        g.renderItem(stack, StructMarkerLayout.ROW_PAD, y + 1);

        String expected = ReadingFormat.reading(row.expected.finiteDoubleValue());
        int expectedWidth = font.width(expected);
        String name =
                font.plainSubstrByWidth(
                        stack.getHoverName().getString(),
                        Math.max(
                                1,
                                StructMarkerLayout.CONTENT_RIGHT
                                        - StructMarkerLayout.ROW_NAME_X
                                        - expectedWidth
                                        - 6));
        g.drawString(
                font,
                name,
                StructMarkerLayout.ROW_NAME_X,
                y + 4,
                StructureMinerTheme.INK,
                false);
        g.drawString(
                font,
                expected,
                StructMarkerLayout.CONTENT_RIGHT - expectedWidth,
                y + 4,
                StructureMinerTheme.FLUIX,
                false);
    }

    /**
     * The action pair, centred across the bottom of the face.
     *
     * <p>Both labels are four characters, which is why these are the 48x16 long buttons rather than
     * the 32x16 ones: the short sprite's face is 32 pixels wide and "标记结构" measures 36, so
     * {@code plainSubstrByWidth} would drop the last glyph instead of shrinking it.
     */
    private void drawActions(GuiGraphics g, int windowX, int windowY) {
        boolean marked = StructMarkerItem.getMarkerInfo(marker).isPresent();
        actionButton(
                g,
                StructMarkerLayout.ACTION_FIRST_X,
                windowX,
                windowY,
                true,
                Component.translatable("screen.dimension_tech.struct_marker.mark"));
        actionButton(
                g,
                StructMarkerLayout.ACTION_SECOND_X,
                windowX,
                windowY,
                marked,
                Component.translatable("screen.dimension_tech.struct_marker.clear"));
    }

    /**
     * One 48x16 sprite button.
     *
     * <p>The sheet has an idle sprite and a lit one and nothing between, so the pointer lights the
     * button rather than tinting it. A disabled button keeps the idle sprite and takes the overlay,
     * which is how every other disabled control in this mod reads.
     */
    private void actionButton(
            GuiGraphics g, int x, int windowX, int windowY, boolean enabled, Component label) {
        boolean hovered =
                inside(
                        windowX,
                        windowY,
                        x,
                        StructMarkerLayout.ACTION_Y,
                        StructMarkerLayout.ACTION_W,
                        StructMarkerLayout.ACTION_H);
        StructureMinerSpriteRenderer.longButton(
                g, x, StructMarkerLayout.ACTION_Y, enabled && hovered);
        if (!enabled) {
            g.fill(
                    x,
                    StructMarkerLayout.ACTION_Y,
                    x + StructMarkerLayout.ACTION_W,
                    StructMarkerLayout.ACTION_Y + StructMarkerLayout.ACTION_H,
                    StructureMinerTheme.DISABLED_OVERLAY);
        }
        GuiText.centered(
                g,
                font,
                label,
                x + StructMarkerLayout.ACTION_W / 2,
                StructMarkerLayout.ACTION_Y + 4,
                StructureMinerTheme.INK);
    }

    // ---------------------------------------------------------- choice overlay

    /**
     * The overlapping-structure picker, painted over everything else.
     *
     * <p>It is a window in its own right — same contour, same dark band, same accent rail — so the
     * player reads it as the terminal asking a question rather than as a second interface.
     */
    private void drawChoices(GuiGraphics g, int mouseX, int mouseY) {
        ChoiceBox box = choiceBox();
        g.fill(0, 0, width, height, 0x99000000);
        StructureMinerTheme.panel(g, box.x(), box.y(), box.width(), box.height(), GuiPalette.FLUIX);
        g.drawString(
                font,
                Component.translatable("screen.dimension_tech.struct_marker.select_prompt"),
                box.x() + 2 + 6,
                box.y() + 1 + 3,
                StructureMinerTheme.TEXT,
                false);

        int visible = Math.min(CHOICE_MAX_VISIBLE, choices.size());
        int first = Math.min(choiceScroll, Math.max(0, choices.size() - visible));
        for (int row = 0; row < visible; row++) {
            int index = first + row;
            int y = box.firstRowY() + row * CHOICE_ROW_H;
            /*
             * The listRow primitive is told this row is "selected" when the pointer is on it: its
             * three states are pressed-in / lifted / untouched, and the overlay sits on a light face
             * where only the pressed-in tone is far enough from the fill to read as feedback.
             */
            StructureMinerTheme.listRow(
                    g,
                    font,
                    box.rowX(),
                    y,
                    box.rowWidth(),
                    CHOICE_ROW_H,
                    Component.literal(choiceLabel(choices.get(index))),
                    inside(mouseX, mouseY, box.rowX(), y, box.rowWidth(), CHOICE_ROW_H),
                    false,
                    StructureMinerTheme.FLUIX);
        }
    }

    private ChoiceBox choiceBox() {
        int visible = Math.min(CHOICE_MAX_VISIBLE, choices.size());
        /*
         * The band is 14 rows and carries the prompt; GuiChrome#window returns the first row below
         * it, so the padding terms are measured from there rather than from the box top.
         */
        int bandHeight = GuiChrome.BAND_HEIGHT + 1;
        int height =
                bandHeight + CHOICE_TOP_PAD + visible * CHOICE_ROW_H + CHOICE_BOTTOM_PAD;
        int x = (width - CHOICE_BOX_W) / 2;
        int y = (this.height - height) / 2;
        return new ChoiceBox(x, y, CHOICE_BOX_W, height, bandHeight + CHOICE_TOP_PAD);
    }

    /** Shows the structures the server found under the player, replacing any previous list. */
    public void showStructureChoices(
            InteractionHand requestedHand,
            BlockPos position,
            List<StructMarkerItem.MarkedStructure> structures) {
        if (!holds(requestedHand) || structures.isEmpty()) return;
        choices = List.copyOf(structures);
        choicePosition = position.immutable();
        choiceScroll = 0;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (!choices.isEmpty()) {
            ChoiceBox box = choiceBox();
            int visible = Math.min(CHOICE_MAX_VISIBLE, choices.size());
            int first = Math.min(choiceScroll, Math.max(0, choices.size() - visible));
            for (int row = 0; row < visible; row++) {
                int y = box.firstRowY() + row * CHOICE_ROW_H;
                if (inside(mouseX, mouseY, box.rowX(), y, box.rowWidth(), CHOICE_ROW_H)) {
                    ModNetwork.selectStructure(hand, choicePosition, first + row);
                    choices = List.of();
                    return true;
                }
            }
            /* The overlay is modal: a click that misses a row must not fall through to the panel. */
            return true;
        }

        int windowX = (int) mouseX - left;
        int windowY = (int) mouseY - top;
        if (hitAction(windowX, windowY, StructMarkerLayout.ACTION_FIRST_X)) {
            ModNetwork.requestStructureSelection(hand);
            return true;
        }
        if (hitAction(windowX, windowY, StructMarkerLayout.ACTION_SECOND_X)
                && StructMarkerItem.getMarkerInfo(marker).isPresent()) {
            ModNetwork.clear(hand);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int step = (int) Math.signum(delta);
        if (!choices.isEmpty()) {
            int visible = Math.min(CHOICE_MAX_VISIBLE, choices.size());
            choiceScroll =
                    Math.max(
                            0,
                            Math.min(
                                    Math.max(0, choices.size() - visible), choiceScroll - step));
            return true;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll - step));
        return true;
    }

    private boolean hitAction(int windowX, int windowY, int x) {
        return inside(
                windowX,
                windowY,
                x,
                StructMarkerLayout.ACTION_Y,
                StructMarkerLayout.ACTION_W,
                StructMarkerLayout.ACTION_H);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - StructMarkerLayout.visibleRows());
    }

    /** Expectation-row index under a screen point, or {@code -1}. */
    private int rowAt(double mouseX, double mouseY) {
        int viewportX = (int) mouseX - left - StructMarkerLayout.VIEWPORT.x();
        int viewportY = (int) mouseY - top - StructMarkerLayout.VIEWPORT.y();
        if (viewportX < 0 || viewportX >= StructMarkerLayout.VIEWPORT_W) return -1;
        if (viewportY < 0 || viewportY >= StructMarkerLayout.VIEWPORT_H) return -1;
        int index = scroll + viewportY / StructMarkerLayout.ROW_H;
        return index >= 0 && index < rows.size() ? index : -1;
    }

    // ----------------------------------------------------------------- helpers

    private static Component statusLabel(AnalysisStatus status) {
        return Component.translatable(
                "screen.dimension_tech.struct_marker.analysis_status."
                        + status.name().toLowerCase(Locale.ROOT));
    }

    /** The accent a state announces with, on the chip and on the calculation-method line. */
    private static int stateAccent(AnalysisStatus status) {
        return switch (status) {
            case EXACT -> StructureMinerTheme.SUCCESS;
            case APPROXIMATE -> StructureMinerTheme.AMBER;
            case UNSUPPORTED -> StructureMinerTheme.ERROR;
            case LEGACY -> StructureMinerTheme.MUTED;
        };
    }

    /**
     * A choice's display text: the structure's translated name, then where its box starts.
     *
     * <p>Two overlapping structures of the same kind differ only by position, so the name alone
     * could not tell them apart.
     */
    private static String choiceLabel(StructMarkerItem.MarkedStructure structure) {
        return TranslateHelper.structureName(structure.id()).getString()
                + " ["
                + structure.bounds().minX()
                + ", "
                + structure.bounds().minY()
                + ", "
                + structure.bounds().minZ()
                + "]";
    }

    private static boolean inside(
            double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private record Row(Item item, ExactProbability expected) {}

    /** Geometry of the choice overlay, derived once per frame from how many choices there are. */
    private record ChoiceBox(int x, int y, int width, int height, int firstRowOffset) {
        int firstRowY() {
            return y + firstRowOffset;
        }

        int rowX() {
            return x + 6;
        }

        int rowWidth() {
            return width - 12;
        }
    }
}
