package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The structure data operator console.
 *
 * <p><b>Fixed frame, one canvas.</b> The texture carries the whole frame — panel, recessed page
 * canvas, every slot face — so this screen only blits it and then draws text and icons inside the
 * canvas rectangle. There is no window resizing and no per-page slot geometry: an earlier revision
 * swapped between a 176x282 operate window and a 320x340 catalogue window with two sets of slot
 * coordinates, which the authored art does not support.
 *
 * <p><b>Two coordinate spaces.</b> Chrome (tabs, action buttons, the search box) is positioned in
 * window-local pixels exactly as {@link StructureDataOperatorLayout} records them. Everything a page
 * draws is canvas-local: {@link #drawPage} translates by the canvas origin and clips to it before
 * handing control to a page, so a page never needs to know where the canvas sits.
 */
public final class StructureDataOperatorScreen
        extends AbstractContainerScreen<StructureDataOperatorMenu> {
    enum Page {
        OPERATION,
        DATA_INTEGRATOR,
        STRUCTURE_INTERPRETER
    }

    static final int WIDTH = StructureDataOperatorLayout.WIDTH;
    static final int HEIGHT = StructureDataOperatorLayout.HEIGHT;

    /** Canvas-local columns shared by the catalogue pages. */
    static final int LIST_X = 4;
    static final int LIST_W = 150;
    static final int DETAIL_X = 158;
    static final int DETAIL_W = 181;

    /** Height of a page's title strip, above everything else on the page. */
    static final int TITLE_H = 15;

    /**
     * The search field takes the canvas-top strip the page title used to occupy, spanning the
     * catalogue column.
     *
     * <p>It used to sit under a separate title strip, which wasted a row and put the field's text
     * over translatable titles of unpredictable width. The tab already names the page, so the title
     * is gone: the field pins to the top padding and the list below it moves up to fill the gap.
     */
    static final int SEARCH_X = LIST_X;

    static final int SEARCH_W = LIST_W;
    static final int SEARCH_Y = 4;
    static final int SEARCH_H = GuiSearchField.frameHeight();

    /** First row of the catalogue list: below the title strip and the search field. */
    static final int LIST_Y = SEARCH_Y + SEARCH_H + 2;

    static final int LIST_ROW_H = 13;
    static final int TABLE_ROW_H = 18;

    /**
     * The action pair sits on the empty panel face right of the tabs and left of the machine slots.
     * Sized from the sprite rather than from the tab row: these are the 32x16 buttons, the tabs are
     * the 48x16 ones.
     */
    static final int ACTION_Y = StructureDataOperatorLayout.TAB_BUTTON_Y;
    static final int ACTION_W = StructureMinerSpriteRenderer.BUTTON_W;
    static final int ACTION_H = StructureMinerSpriteRenderer.BUTTON_H;
    static final int ACTION_FIRST_X = 176;
    static final int ACTION_SECOND_X = 214;

    /*
     * The operate page shares the action row's box; only the x positions differ, so these name the
     * two actions rather than restating the shared geometry.
     */
    static final int OPERATE_COPY_X = ACTION_FIRST_X;
    static final int OPERATE_CLEAR_X = ACTION_SECOND_X;

    private Page page = Page.OPERATION;
    private List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> catalogue = List.of();
    /*
     * Lowercased display text per catalogue entry. The tree is rebuilt every frame, and resolving a
     * translation means building a Component and running it through the language — cheap once, not
     * cheap a few hundred times a frame. Cleared whenever a new catalogue arrives.
     */
    private final Map<StructureDataOperatorBlockEntity.StructureCatalogueEntry, String>
            searchLabels = new HashMap<>();
    private final Set<ResourceLocation> expandedDimensions = new HashSet<>();
    private StructureDataOperatorBlockEntity.StructureCatalogueEntry selected;
    private ItemStack detailMarker = ItemStack.EMPTY;
    private GuiSearchField search;
    private int listScroll;
    private int detailScroll;
    private int statusTicks;
    private int analysisPollTicks;
    private String statusKey;

    public StructureDataOperatorScreen(
            StructureDataOperatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        /* The texture has no caption slot of its own; the vanilla label would land on the frame. */
        inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        updateGeometry();
        search =
                GuiSearchField.framed(
                        font,
                        canvasX(SEARCH_X),
                        canvasY(SEARCH_Y),
                        SEARCH_W,
                        Component.translatable("screen.dimension_tech.structure_operator.search"));
        search.setVisible(false);
        addRenderableWidget(search);
    }

    StructureDataOperatorMenu menu() {
        return menu;
    }

    Page page() {
        return page;
    }

    boolean hasIntegrator() {
        return !menu.getSlot(StructureDataOperatorBlockEntity.INTEGRATOR).getItem().isEmpty();
    }

    boolean hasInterpreter() {
        return !menu.getSlot(StructureDataOperatorBlockEntity.INTERPRETER).getItem().isEmpty();
    }

    boolean interpreterAvailable() {
        return hasIntegrator() && hasInterpreter();
    }

    boolean hasOperands() {
        for (int index = StructureDataOperatorBlockEntity.OPERAND_START;
                index
                        < StructureDataOperatorBlockEntity.OPERAND_START
                                + StructureDataOperatorBlockEntity.OPERAND_COUNT;
                index++) if (!menu.getSlot(index).getItem().isEmpty()) return true;
        return false;
    }

    boolean hasWriteMarker() {
        return !menu.getSlot(StructureDataOperatorBlockEntity.TARGET).getItem().isEmpty();
    }

    /** The marker in the read slot, or empty when the slot holds something else. */
    ItemStack readMarker() {
        ItemStack stack = menu.getSlot(StructureDataOperatorBlockEntity.TARGET).getItem();
        return stack.is(ModItems.STRUCTURE_MARKER.get()) ? stack : ItemStack.EMPTY;
    }

    List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> catalogue() {
        return catalogue;
    }

    StructureDataOperatorBlockEntity.StructureCatalogueEntry selected() {
        return selected;
    }

    ItemStack detailMarker() {
        return detailMarker;
    }

    int listScroll() {
        return listScroll;
    }

    void listScroll(int value) {
        listScroll = Math.max(0, Math.min(Math.max(0, catalogueRows().size() - listRows()), value));
    }

    int detailScroll() {
        return detailScroll;
    }

    void detailScroll(int value) {
        detailScroll =
                Math.max(
                        0,
                        Math.min(
                                Math.max(
                                        0,
                                        StructMarkerItem.getExpectedItemCounts(detailMarker).size()
                                                - tableRows()),
                                value));
    }

    /** How many catalogue rows the list region can show. */
    static int listRows() {
        return Math.max(
                1, (StructureDataOperatorLayout.CANVAS.height() - LIST_Y - 4) / LIST_ROW_H);
    }

    /** How many expectation rows the detail table can show. */
    static int tableRows() {
        /*
         * Measured from where the table actually starts rather than from a fixed inset, because the
         * readings block above it is what sets that offset. Rows are allowed to reach the canvas
         * bottom: the block already accounts for the padding a page would otherwise reserve.
         */
        return Math.max(
                1,
                (StructureDataOperatorLayout.CANVAS.height()
                                - StructureDataIntegratorPage.tableTop())
                        / TABLE_ROW_H);
    }

    String searchText() {
        return search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
    }

    List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> filteredCatalogue() {
        String query = searchText();
        return catalogue.stream()
                .filter(entry -> matches(entry, query))
                .sorted(
                        Comparator.comparing(
                                        StructureDataOperatorBlockEntity.StructureCatalogueEntry
                                                ::dimension)
                                .thenComparing(
                                        StructureDataOperatorBlockEntity.StructureCatalogueEntry
                                                ::structure))
                .toList();
    }

    /**
     * Whether an entry survives the search box.
     *
     * <p>The match runs against what the tree actually shows: the structure's translated name, and the
     * name of the dimension grouping it, since that header is part of what the player reads for the
     * row. It used to run against the resource key alone, which meant nothing typed in the player's
     * own language could ever hit — {@code minecraft:desert_pyramid} reads the same in every language,
     * while the row next to the cursor said 沙漠神殿.
     *
     * <p>The key is still accepted, but deliberately last: it is the only handle on a structure this
     * mod has no translation for, and it can match text the row never displays — searching
     * {@code minecraft} would otherwise light up the entire list.
     */
    private boolean matches(
            StructureDataOperatorBlockEntity.StructureCatalogueEntry entry, String query) {
        if (query.isEmpty()) return true;
        return searchLabel(entry).contains(query)
                || entry.structure().toString().toLowerCase(Locale.ROOT).contains(query);
    }

    /** Lowercased translated names of a structure and its dimension, resolved once per entry. */
    private String searchLabel(
            StructureDataOperatorBlockEntity.StructureCatalogueEntry entry) {
        return searchLabels.computeIfAbsent(
                entry,
                e ->
                        (TranslateHelper.structureName(e.structure()).getString()
                                        + ' '
                                        + TranslateHelper.dimensionName(e.dimension()).getString())
                                .toLowerCase(Locale.ROOT));
    }

    List<CatalogueRow> catalogueRows() {
        List<CatalogueRow> rows = new ArrayList<>();
        String query = searchText();
        ResourceLocation currentDimension = null;
        for (StructureDataOperatorBlockEntity.StructureCatalogueEntry entry : filteredCatalogue()) {
            if (!entry.dimension().equals(currentDimension)) {
                currentDimension = entry.dimension();
                rows.add(new CatalogueRow(currentDimension, null));
            }
            if (!query.isEmpty() || expandedDimensions.contains(entry.dimension()))
                rows.add(new CatalogueRow(entry.dimension(), entry));
        }
        return rows;
    }

    public void receiveCatalogue(
            List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> entries) {
        catalogue =
                entries.stream()
                        .sorted(
                                Comparator.comparing(
                                                StructureDataOperatorBlockEntity
                                                                .StructureCatalogueEntry
                                                        ::dimension)
                                        .thenComparing(
                                                StructureDataOperatorBlockEntity
                                                                .StructureCatalogueEntry
                                                        ::structure))
                        .toList();
        /* A new catalogue invalidates the names cached for the old one. */
        searchLabels.clear();
        selected = null;
        detailMarker = ItemStack.EMPTY;
        listScroll = 0;
        detailScroll = 0;
        expandedDimensions.clear();
        entries.stream()
                .map(StructureDataOperatorBlockEntity.StructureCatalogueEntry::dimension)
                .findFirst()
                .ifPresent(expandedDimensions::add);
    }

    public void receiveDetail(
            ResourceLocation dimension, ResourceLocation structure, ItemStack marker) {
        // Detail responses can arrive out of order when the user changes selection quickly.
        // Never let an older response overwrite the detail view of the current selection.
        if (selected == null
                || !selected.dimension().equals(dimension)
                || !selected.structure().equals(structure)) {
            return;
        }
        catalogue.stream()
                .filter(
                        entry ->
                                entry.dimension().equals(dimension)
                                        && entry.structure().equals(structure))
                .findFirst()
                .ifPresent(
                        entry -> {
                            selected = entry;
                            detailMarker = marker.copy();
                            detailScroll = 0;
                        });
    }

    void select(StructureDataOperatorBlockEntity.StructureCatalogueEntry entry) {
        selected = entry;
        detailMarker = ItemStack.EMPTY;
        analysisPollTicks = 0;
        ModNetwork.structureOperatorRequestDetail(
                menu.containerId, entry.dimension(), entry.structure());
    }

    void toggleDimension(ResourceLocation dimension) {
        if (!expandedDimensions.add(dimension)) expandedDimensions.remove(dimension);
        listScroll(listScroll);
    }

    boolean isDimensionExpanded(ResourceLocation dimension) {
        return expandedDimensions.contains(dimension);
    }

    void refreshPageData() {
        if (page == Page.DATA_INTEGRATOR && hasIntegrator())
            ModNetwork.structureOperatorLoadCatalogue(menu.containerId, false);
        if (page == Page.STRUCTURE_INTERPRETER && interpreterAvailable())
            ModNetwork.structureOperatorLoadCatalogue(menu.containerId, true);
    }

    void refreshSelectedAnalysis() {
        if (selected == null) return;
        detailMarker = ItemStack.EMPTY;
        analysisPollTicks = 0;
        ModNetwork.structureOperatorRefreshDetail(
                menu.containerId, selected.dimension(), selected.structure());
    }

    /**
     * Applies the read marker's data to every filled write slot.
     *
     * <p>There is no confirmation step: the button is already disabled unless the read slot holds a
     * marked structure and at least one write slot is filled, so a modal could only repeat the guard
     * back at the player. The status line reports what happened instead.
     */
    void copyToOperands() {
        ModNetwork.structureOperatorCopy(menu.containerId);
        statusKey = "screen.dimension_tech.structure_operator.status.copied";
        statusTicks = 0;
    }

    /** Strips marker data from every write slot. */
    void clearOperands() {
        ModNetwork.structureOperatorClearOperands(menu.containerId);
        statusKey = "screen.dimension_tech.structure_operator.status.cleared";
        statusTicks = 0;
    }

    /** Writes the selected catalogue entry's snapshot onto the read slot's marker. */
    void writeToReadSlot() {
        if (selected == null) return;
        ModNetwork.structureOperatorWrite(
                menu.containerId, selected.dimension(), selected.structure());
        statusKey = "screen.dimension_tech.structure_operator.status.written";
        statusTicks = 0;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if ((page == Page.DATA_INTEGRATOR && !hasIntegrator())
                || (page == Page.STRUCTURE_INTERPRETER && !interpreterAvailable()))
            setPage(Page.OPERATION);
        if (selected != null && detailMarker.isEmpty() && ++analysisPollTicks >= 10) {
            analysisPollTicks = 0;
            ModNetwork.structureOperatorRequestDetail(
                    menu.containerId, selected.dimension(), selected.structure());
        }
        if (statusKey != null && ++statusTicks >= 60) statusKey = null;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        search.setVisible(page != Page.OPERATION);
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        if (statusKey != null)
            GuiText.centered(
                    graphics,
                    font,
                    Component.translatable(statusKey),
                    leftPos + WIDTH / 2,
                    topPos + HEIGHT - 11,
                    StructureMinerTheme.SUCCESS);
        renderSpecialSlotTooltip(graphics, mouseX, mouseY);
        /*
         * The hover pass runs after super.render, so the pose is back to identity and the canvas
         * scissor has been closed. That is what renderTooltip needs: it draws under the current
         * pose and leaves the scissor alone, so calling it from inside the page draw would place
         * the box one canvas origin away from the cursor and crop it at the canvas edge.
         */
        if (page == Page.OPERATION) {
            StructureDataOperatorOperationPage.renderTooltip(
                    this,
                    graphics,
                    canvasMouseX(mouseX),
                    canvasMouseY(mouseY),
                    mouseX,
                    mouseY);
        } else {
            StructureDataIntegratorPage.renderTooltip(
                    this,
                    graphics,
                    canvasMouseX(mouseX),
                    canvasMouseY(mouseY),
                    mouseX,
                    mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(leftPos, topPos, 0.0D);
        g.blit(
                StructureDataOperatorLayout.TEXTURE,
                0,
                0,
                0,
                0,
                WIDTH,
                HEIGHT,
                StructureDataOperatorLayout.TEXTURE_WIDTH,
                StructureDataOperatorLayout.TEXTURE_HEIGHT);
        drawTabs(g);
        drawActions(g, mouseX - leftPos, mouseY - topPos);
        drawSectionLabels(g);
        drawPage(g, mouseX - leftPos, mouseY - topPos);
        g.pose().popPose();
    }

    /** Clips to the canvas, translates to its origin, and lets the current page draw. */
    private void drawPage(GuiGraphics g, int windowMouseX, int windowMouseY) {
        /*
         * enableScissor takes GUI-logical coordinates and applies the window's GUI scale itself
         * (GuiGraphics.applyScissor multiplies the rectangle by it), so the canvas rectangle is
         * handed over as-is. This screen never scales its pose: the frame is a fixed 357x245, which
         * already fits the 480x270 minimum, so there is no second scale to fold in. The miner passes
         * its own pose scale there for exactly that reason, and reading that argument as a unit
         * conversion multiplied the rectangle twice — the bottom edge landed past the window, the
         * height clamped to zero, and every page was clipped away.
         */
        g.enableScissor(
                leftPos + StructureDataOperatorLayout.CANVAS.x(),
                topPos + StructureDataOperatorLayout.CANVAS.y(),
                leftPos + StructureDataOperatorLayout.CANVAS.right(),
                topPos + StructureDataOperatorLayout.CANVAS.bottom());
        g.pose().pushPose();
        g.pose()
                .translate(
                        StructureDataOperatorLayout.CANVAS.x(),
                        StructureDataOperatorLayout.CANVAS.y(),
                        0.0D);
        int canvasMouseX = windowMouseX - StructureDataOperatorLayout.CANVAS.x();
        int canvasMouseY = windowMouseY - StructureDataOperatorLayout.CANVAS.y();
        if (page == Page.OPERATION) {
            StructureDataOperatorOperationPage.render(this, g, canvasMouseX, canvasMouseY);
        } else if (page == Page.DATA_INTEGRATOR) {
            StructureDataIntegratorPage.renderSource(
                    this, g, canvasMouseX, canvasMouseY, StructureMinerTheme.FLUID_ACCENT);
        } else {
            StructureInterpreterPage.render(this, g, canvasMouseX, canvasMouseY);
        }
        g.pose().popPose();
        g.disableScissor();
    }

    /** The visible tabs, which depend on which plugins are fitted. */
    Page[] visiblePages() {
        return interpreterAvailable()
                ? new Page[] {Page.OPERATION, Page.DATA_INTEGRATOR, Page.STRUCTURE_INTERPRETER}
                : hasIntegrator()
                        ? new Page[] {Page.OPERATION, Page.DATA_INTEGRATOR}
                        : new Page[] {Page.OPERATION};
    }

    private void drawTabs(GuiGraphics g) {
        Page[] pages = visiblePages();
        for (int index = 0; index < pages.length; index++) {
            int x = StructureDataOperatorLayout.tabX(index);
            int y = StructureDataOperatorLayout.TAB_BUTTON_Y;
            boolean selected = page == pages[index];
            StructureMinerSpriteRenderer.longButton(g, x, y, selected);
            /*
             * Clipped to the sprite's face, which is 46px wide once its 1px outline is discounted.
             * The budget has to stay at 46: "已探索结构" measures 45px, and a 44px budget would drop
             * its last glyph rather than shrink it.
             */
            String label =
                    font.plainSubstrByWidth(
                            Component.translatable(tabKey(pages[index])).getString(),
                            StructureDataOperatorLayout.TAB_BUTTON_WIDTH - 2);
            g.drawString(
                    font,
                    label,
                    x + StructureDataOperatorLayout.TAB_BUTTON_WIDTH / 2 - font.width(label) / 2,
                    y + 4,
                    selected ? StructureMinerTheme.INK : StructureMinerTheme.DIM,
                    false);
        }
    }

    /**
     * Names the two slot blocks that sit under the canvas, so the write array cannot be mistaken for
     * the player's own inventory at a glance — both are 9x4 grids of the same 16px faces.
     *
     * <p>Each label is measured from the first face of the block it names and placed
     * {@link StructureDataOperatorLayout#SECTION_LABEL_GAP} blank panel rows above it, less the
     * inventory's one-row nudge. The boxes land at y=152 (write array) and y=149 (inventory). They are
     * deliberately not on a shared baseline: the two blocks are not either — the write array starts at
     * y=164 and the inventory at y=160, so the captions sit the same distance from their own blocks
     * and follow them if either ever moves.
     *
     * <p>These are chrome, not page content: they are painted outside the canvas scissor, in the strip
     * between it and the wells. Nothing here may cross into the canvas — the labels would be clipped
     * there and would read as a stray word inside a page.
     */
    private void drawSectionLabels(GuiGraphics g) {
        sectionLabel(
                g,
                "screen.dimension_tech.structure_operator.section.write_slots",
                StructureDataOperatorLayout.OPERAND_X,
                StructureDataOperatorLayout.WRITE_LABEL_Y);
        sectionLabel(
                g,
                "screen.dimension_tech.structure_operator.section.inventory",
                StructureDataOperatorLayout.PLAYER_INVENTORY_X,
                StructureDataOperatorLayout.INVENTORY_LABEL_Y);
    }

    private void sectionLabel(GuiGraphics g, String key, int x, int y) {
        /* Clipped to the block it names, so a longer translation cannot run into the other block. */
        String text =
                font.plainSubstrByWidth(
                        Component.translatable(key).getString(),
                        StructureDataOperatorLayout.BLOCK_WIDTH);
        g.drawString(font, text, x, y, StructureMinerTheme.INK, false);
    }

    private static String tabKey(Page page) {
        return switch (page) {
            case OPERATION -> "screen.dimension_tech.structure_operator.tab.operation";
            case DATA_INTEGRATOR -> "screen.dimension_tech.structure_operator.tab.integrator";
            case STRUCTURE_INTERPRETER -> "screen.dimension_tech.structure_operator.tab.interpreter";
        };
    }

    /** Draws the page's action pair, in window-local pixels. */
    private void drawActions(GuiGraphics g, int windowMouseX, int windowMouseY) {
        if (page == Page.OPERATION) {
            actionButton(
                    g,
                    OPERATE_COPY_X,
                    "screen.dimension_tech.structure_operator.copy",
                    StructureDataOperatorOperationPage.canCopy(this),
                    windowMouseX,
                    windowMouseY);
            actionButton(
                    g,
                    OPERATE_CLEAR_X,
                    "screen.dimension_tech.structure_operator.clear",
                    hasOperands(),
                    windowMouseX,
                    windowMouseY);
            return;
        }
        boolean ready = selected != null && !detailMarker.isEmpty();
        actionButton(
                g,
                ACTION_FIRST_X,
                "screen.dimension_tech.structure_operator.refresh_short",
                selected != null,
                windowMouseX,
                windowMouseY);
        actionButton(
                g,
                ACTION_SECOND_X,
                "screen.dimension_tech.structure_operator.write_short",
                ready && hasWriteMarker(),
                windowMouseX,
                windowMouseY);
    }

    /**
     * One 32x16 sprite button.
     *
     * <p>The sheet has an idle sprite and a lit one and nothing between, so the pointer lights the
     * button rather than tinting it — the same feedback the 20x20 controls give, which have a third
     * sprite to spare for the hover. A disabled button keeps the idle sprite and takes the overlay,
     * which is how every other disabled control in this mod reads.
     */
    void actionButton(
            GuiGraphics g,
            int x,
            String key,
            boolean enabled,
            int windowMouseX,
            int windowMouseY) {
        boolean hovered = inside(windowMouseX, windowMouseY, x, ACTION_Y, ACTION_W, ACTION_H);
        StructureMinerSpriteRenderer.button(g, x, ACTION_Y, enabled && hovered);
        if (!enabled) {
            g.fill(
                    x,
                    ACTION_Y,
                    x + ACTION_W,
                    ACTION_Y + ACTION_H,
                    StructureMinerTheme.DISABLED_OVERLAY);
        }
        String label = Component.translatable(key).getString();
        g.drawString(
                font,
                label,
                x + ACTION_W / 2 - font.width(label) / 2,
                ACTION_Y + 4,
                StructureMinerTheme.INK,
                false);
    }

    // ----------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int windowX = (int) mouseX - leftPos;
        int windowY = (int) mouseY - topPos;
        Page[] pages = visiblePages();
        for (int index = 0; index < pages.length; index++) {
            if (inside(
                    windowX,
                    windowY,
                    StructureDataOperatorLayout.tabX(index),
                    StructureDataOperatorLayout.TAB_BUTTON_Y,
                    StructureDataOperatorLayout.TAB_BUTTON_WIDTH,
                    StructureDataOperatorLayout.TAB_BUTTON_HEIGHT)) {
                setPage(pages[index]);
                if (page != Page.OPERATION) refreshPageData();
                return true;
            }
        }
        if (page == Page.OPERATION) {
            if (hitAction(windowX, windowY, OPERATE_COPY_X)
                    && StructureDataOperatorOperationPage.canCopy(this)) {
                copyToOperands();
                return true;
            }
            if (hitAction(windowX, windowY, OPERATE_CLEAR_X) && hasOperands()) {
                clearOperands();
                return true;
            }
            /* The expectation table's header is clickable; it sorts by the column it names. */
            if (StructureDataOperatorOperationPage.mouseClicked(
                    this, canvasMouseX(mouseX), canvasMouseY(mouseY))) return true;
        } else {
            if (hitAction(windowX, windowY, ACTION_FIRST_X)) {
                if (selected == null) refreshPageData();
                else refreshSelectedAnalysis();
                return true;
            }
            if (hitAction(windowX, windowY, ACTION_SECOND_X)
                    && selected != null
                    && !detailMarker.isEmpty()
                    && hasWriteMarker()) {
                writeToReadSlot();
                return true;
            }
            if (StructureDataIntegratorPage.mouseClicked(
                    this, canvasMouseX(mouseX), canvasMouseY(mouseY))) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean hitAction(int windowX, int windowY, int x) {
        return inside(windowX, windowY, x, ACTION_Y, ACTION_W, ACTION_H);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int step = (int) Math.signum(delta);
        if (page == Page.OPERATION) {
            // No position gate on the operate page, matching the miner's wheel behaviour.
            StructureDataOperatorOperationPage.scroll(this, step);
            return true;
        }
        int x = canvasMouseX(mouseX);
        int y = canvasMouseY(mouseY);
        if (inside(
                x,
                y,
                LIST_X,
                LIST_Y,
                LIST_W,
                StructureDataOperatorLayout.CANVAS.height() - LIST_Y)) {
            listScroll(listScroll - step);
            return true;
        }
        if (inside(
                x,
                y,
                DETAIL_X,
                TITLE_H,
                DETAIL_W,
                StructureDataOperatorLayout.CANVAS.height() - TITLE_H)) {
            detailScroll(detailScroll - step);
            return true;
        }
        return true;
    }

    private void setPage(Page next) {
        page = next;
        catalogue = List.of();
        expandedDimensions.clear();
        selected = null;
        detailMarker = ItemStack.EMPTY;
        listScroll = 0;
        detailScroll = 0;
    }

    private void updateGeometry() {
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        leftPos = (width - WIDTH) / 2;
        topPos = (height - HEIGHT) / 2;
        if (search != null) search.moveFrame(canvasX(SEARCH_X), canvasY(SEARCH_Y));
    }

    int canvasX(int canvasLocal) {
        return leftPos + StructureDataOperatorLayout.CANVAS.x() + canvasLocal;
    }

    int canvasY(int canvasLocal) {
        return topPos + StructureDataOperatorLayout.CANVAS.y() + canvasLocal;
    }

    private int canvasMouseX(double screenX) {
        return (int) screenX - leftPos - StructureDataOperatorLayout.CANVAS.x();
    }

    private int canvasMouseY(double screenY) {
        return (int) screenY - topPos - StructureDataOperatorLayout.CANVAS.y();
    }

    /**
     * The three function slots (read marker, data integrator, structure interpreter) describe
     * themselves with a stable two-line hint — what belongs there and what it unlocks — rather than
     * the bare item name a filled slot would otherwise show. The vanilla hovered-slot tooltip is
     * therefore suppressed for them in {@link #renderTooltip}; the write array and the player
     * inventory keep the normal item tooltip, drawn by the super call there.
     */
    private void renderSpecialSlotTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        renderSlotHint(
                graphics,
                mouseX,
                mouseY,
                StructureDataOperatorBlockEntity.TARGET,
                "screen.dimension_tech.structure_operator.slot.target",
                "screen.dimension_tech.structure_operator.slot.target.func");
        renderSlotHint(
                graphics,
                mouseX,
                mouseY,
                StructureDataOperatorBlockEntity.INTEGRATOR,
                "screen.dimension_tech.structure_operator.slot.integrator",
                "screen.dimension_tech.structure_operator.slot.integrator.func");
        renderSlotHint(
                graphics,
                mouseX,
                mouseY,
                StructureDataOperatorBlockEntity.INTERPRETER,
                "screen.dimension_tech.structure_operator.slot.interpreter",
                "screen.dimension_tech.structure_operator.slot.interpreter.func");
    }

    private void renderSlotHint(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int slotIndex,
            String headKey,
            String funcKey) {
        Slot slot = menu.getSlot(slotIndex);
        if (slot == null || !slot.isActive()) return;
        if (!inside(mouseX, mouseY, leftPos + slot.x, topPos + slot.y, 16, 16)) return;
        graphics.renderTooltip(
                font,
                List.of(
                        Component.translatable(headKey).withStyle(ChatFormatting.BOLD),
                        Component.translatable(funcKey).withStyle(ChatFormatting.GRAY)),
                Optional.empty(),
                mouseX,
                mouseY);
    }

    /**
     * Routes the hovered-slot tooltip. The three function slots own their hint (drawn by
     * {@link #renderSpecialSlotTooltip} after this), so the default item tooltip is skipped for them;
     * every other slot falls through to the vanilla behaviour.
     */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Slot hovered = this.hoveredSlot;
        if (hovered != null
                && (hovered.index == StructureDataOperatorBlockEntity.TARGET
                        || hovered.index == StructureDataOperatorBlockEntity.INTEGRATOR
                        || hovered.index == StructureDataOperatorBlockEntity.INTERPRETER)) {
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                Component.translatable("screen.dimension_tech.structure_operator.title"),
                8,
                6,
                StructureMinerTheme.INK,
                false);
    }

    record CatalogueRow(
            ResourceLocation dimension,
            StructureDataOperatorBlockEntity.StructureCatalogueEntry entry) {
        boolean isDimension() {
            return entry == null;
        }
    }

    static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }
}
