package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 320-pixel operator console with a catalogue browser and marker-style detail viewport. */
public final class StructureDataOperatorScreen
        extends AbstractContainerScreen<StructureDataOperatorMenu> {
    enum Page {
        OPERATION,
        DATA_INTEGRATOR,
        STRUCTURE_INTERPRETER
    }

    static final int WIDTH = 320;
    static final int HEIGHT = 340;
    private static final int COMPACT_WIDTH = 176;
    private static final int COMPACT_HEIGHT = 282;
    static final int TAB_Y = 31;
    static final int LEFT_X = 16;
    static final int LEFT_W = 136;
    static final int RIGHT_X = 160;
    static final int RIGHT_W = 144;
    private static final int LEFT_CONTROLS_WIDTH = 30;
    private Page page = Page.OPERATION;
    private List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> catalogue = List.of();
    private final Set<ResourceLocation> expandedDimensions = new HashSet<>();
    private StructureDataOperatorBlockEntity.StructureCatalogueEntry selected;
    private ItemStack detailMarker = ItemStack.EMPTY;
    private EditBox search;
    private int listScroll;
    private int detailScroll;
    private float uiScale = 1.0F;
    private int statusTicks;
    private int analysisPollTicks;
    private int completedAnalysisSamples;
    private int totalAnalysisSamples;
    private String statusKey;
    private boolean confirmCopy;
    private boolean confirmWrite;

    public StructureDataOperatorScreen(
            StructureDataOperatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = COMPACT_WIDTH;
        imageHeight = COMPACT_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        updatePageGeometry();
        search =
                new EditBox(
                        font,
                        leftPos + LEFT_X + 26,
                        topPos + 82,
                        78,
                        16,
                        Component.translatable("screen.dimension_tech.structure_operator.search"));
        search.setBordered(false);
        search.setTextColor(MythicMinerTheme.TEXT);
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

    List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> catalogue() {
        return catalogue;
    }

    StructureDataOperatorBlockEntity.StructureCatalogueEntry selected() {
        return selected;
    }

    ItemStack detailMarker() {
        return detailMarker;
    }

    int completedAnalysisSamples() {
        return completedAnalysisSamples;
    }

    int totalAnalysisSamples() {
        return totalAnalysisSamples;
    }

    int listScroll() {
        return listScroll;
    }

    void listScroll(int value) {
        listScroll = Math.max(0, Math.min(Math.max(0, catalogueRows().size() - 9), value));
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
                                                - 5),
                                value));
    }

    String searchText() {
        return search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
    }

    List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> filteredCatalogue() {
        return catalogue.stream()
                .filter(
                        entry ->
                                entry.structure()
                                        .toString()
                                        .toLowerCase(Locale.ROOT)
                                        .contains(searchText()))
                .sorted(
                        Comparator.comparing(
                                        StructureDataOperatorBlockEntity.StructureCatalogueEntry
                                                ::dimension)
                                .thenComparing(
                                        StructureDataOperatorBlockEntity.StructureCatalogueEntry
                                                ::structure))
                .toList();
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
            ResourceLocation dimension,
            ResourceLocation structure,
            ItemStack marker,
            int completedSamples,
            int totalSamples) {
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
                            completedAnalysisSamples = completedSamples;
                            totalAnalysisSamples = totalSamples;
                        });
    }

    void select(StructureDataOperatorBlockEntity.StructureCatalogueEntry entry) {
        selected = entry;
        detailMarker = ItemStack.EMPTY;
        analysisPollTicks = 0;
        completedAnalysisSamples = 0;
        totalAnalysisSamples = 0;
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
        completedAnalysisSamples = 0;
        totalAnalysisSamples = 0;
        analysisPollTicks = 0;
        ModNetwork.structureOperatorRefreshDetail(
                menu.containerId, selected.dimension(), selected.structure());
    }

    void showCopyConfirmation() {
        confirmCopy = true;
    }

    void showWriteConfirmation() {
        if (selected != null) confirmWrite = true;
    }

    void clearOperands() {
        ModNetwork.structureOperatorClearOperands(menu.containerId);
        statusKey = "screen.dimension_tech.structure_operator.status.cleared";
        statusTicks = 0;
    }

    boolean modalOpen() {
        return confirmCopy || confirmWrite;
    }

    void cancelModal() {
        confirmCopy = false;
        confirmWrite = false;
    }

    void confirmModal() {
        if (confirmCopy) {
            ModNetwork.structureOperatorCopy(menu.containerId);
            statusKey = "screen.dimension_tech.structure_operator.status.copied";
        } else if (selected != null) {
            ModNetwork.structureOperatorWrite(
                    menu.containerId, selected.dimension(), selected.structure());
            statusKey = "screen.dimension_tech.structure_operator.status.written";
        }
        statusTicks = 0;
        cancelModal();
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        search.setVisible(page != Page.OPERATION && !modalOpen());
        renderBackground(graphics);
        int logicalMouseX = toLogical(mouseX);
        int logicalMouseY = toLogical(mouseY);
        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1.0F);
        super.render(graphics, logicalMouseX, logicalMouseY, partial);
        if (modalOpen()) drawModal(graphics, logicalMouseX, logicalMouseY);
        else if (statusKey != null)
            graphics.drawCenteredString(
                    font,
                    Component.translatable(statusKey),
                    leftPos + activeWidth() / 2,
                    topPos + activeHeight() - 12,
                    MythicMinerTheme.SUCCESS);
        graphics.pose().popPose();
        if (!modalOpen()) {
            renderInventoryItemTooltip(graphics, logicalMouseX, logicalMouseY, mouseX, mouseY);
            if (page != Page.OPERATION)
                StructureDataIntegratorPage.renderTooltip(
                        this,
                        graphics,
                        logicalMouseX - leftPos,
                        logicalMouseY - topPos,
                        mouseX,
                        mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(leftPos, topPos, 0.0D);
        MythicMinerTheme.panel(g, 0, 0, activeWidth(), activeHeight(), MythicMinerTheme.AMBER);
        g.drawString(font, title, 8, 18, MythicMinerTheme.TEXT, false);
        drawTabs(g);
        if (page == Page.OPERATION)
            StructureDataOperatorOperationPage.render(this, g, mouseX - leftPos, mouseY - topPos);
        else
            StructureDataIntegratorPage.renderSource(
                    this,
                    g,
                    mouseX - leftPos,
                    mouseY - topPos,
                    page == Page.DATA_INTEGRATOR ? MythicMinerTheme.FLUIX : MythicMinerTheme.AMBER,
                    page == Page.DATA_INTEGRATOR
                            ? "screen.dimension_tech.structure_operator.page.integrator"
                            : "screen.dimension_tech.structure_operator.page.interpreter");
        drawSlots(g);
        drawInventoryChrome(g);
        g.pose().popPose();
    }

    private void drawTabs(GuiGraphics g) {
        Page[] pages =
                interpreterAvailable()
                        ? new Page[] {
                            Page.OPERATION, Page.DATA_INTEGRATOR, Page.STRUCTURE_INTERPRETER
                        }
                        : hasIntegrator()
                                ? new Page[] {Page.OPERATION, Page.DATA_INTEGRATOR}
                                : new Page[] {Page.OPERATION};
        String[] labels = {
            "screen.dimension_tech.structure_operator.tab.operation",
            "screen.dimension_tech.structure_operator.tab.integrator",
            "screen.dimension_tech.structure_operator.tab.interpreter"
        };
        int tabWidth = activeWidth() / pages.length;
        int tabY = page == Page.OPERATION ? 29 : TAB_Y;
        for (int index = 0; index < pages.length; index++)
            MythicMinerTheme.tab(
                    g,
                    font,
                    index * tabWidth + 2,
                    tabY,
                    tabWidth - 4,
                    Component.translatable(labels[pages[index].ordinal()]),
                    page == pages[index]);
    }

    private void drawSlots(GuiGraphics g) {
        for (int index = 0; index <= StructureDataOperatorMenu.SOURCE_MARKER_MENU_SLOT; index++) {
            Slot slot = menu.getSlot(index);
            if (!slot.isActive()) continue;
            g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, MythicMinerTheme.PANEL);
        }
        drawPluginLabel(
                g,
                "screen.dimension_tech.structure_operator.integrator",
                50,
                MythicMinerTheme.FLUIX);
        drawPluginLabel(
                g,
                "screen.dimension_tech.structure_operator.interpreter",
                78,
                MythicMinerTheme.AMBER);
    }

    private void drawPluginLabel(GuiGraphics g, String key, int y, int color) {
        String label = Component.translatable(key).getString();
        g.drawString(font, label, -30 - font.width(label), y + 4, color, false);
    }

    private void drawInventoryChrome(GuiGraphics g) {
        int dividerX = page == Page.OPERATION ? 8 : 79;
        int dividerY = page == Page.OPERATION ? 188 : 248;
        int dividerWidth = page == Page.OPERATION ? 160 : 162;
        g.fill(dividerX, dividerY, dividerX + dividerWidth, dividerY + 1, MythicMinerTheme.AMBER);
        for (int index = StructureDataOperatorMenu.PLAYER_SLOT_START;
                index < menu.slots.size();
                index++) {
            Slot slot = menu.getSlot(index);
            if (slot.isActive())
                g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, MythicMinerTheme.PANEL);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double logicalX = mouseX / uiScale;
        double logicalY = mouseY / uiScale;
        if (button != 0) return super.mouseClicked(logicalX, logicalY, button);
        if (modalOpen()) {
            int x = leftPos + (page == Page.OPERATION ? 12 : 78),
                    y = topPos + (page == Page.OPERATION ? 148 : 146);
            if (inside(logicalX, logicalY, x, y, 72, 18)) {
                cancelModal();
                return true;
            }
            if (inside(logicalX, logicalY, x + 92, y, 72, 18)) {
                confirmModal();
                return true;
            }
            return true;
        }
        int x = (int) logicalX - leftPos, y = (int) logicalY - topPos;
        Page[] pages =
                interpreterAvailable()
                        ? new Page[] {
                            Page.OPERATION, Page.DATA_INTEGRATOR, Page.STRUCTURE_INTERPRETER
                        }
                        : hasIntegrator()
                                ? new Page[] {Page.OPERATION, Page.DATA_INTEGRATOR}
                                : new Page[] {Page.OPERATION};
        int tabY = page == Page.OPERATION ? 29 : TAB_Y;
        if (inside(x, y, 0, tabY, activeWidth(), 13)) {
            setPage(pages[Math.min(pages.length - 1, x / (activeWidth() / pages.length))]);
            if (page != Page.OPERATION) refreshPageData();
            return true;
        }
        boolean handled =
                page == Page.OPERATION
                        ? StructureDataOperatorOperationPage.mouseClicked(this, x, y)
                        : StructureDataIntegratorPage.mouseClicked(this, x, y);
        return handled || super.mouseClicked(logicalX, logicalY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int x = (int) (mouseX / uiScale) - leftPos;
        int y = (int) (mouseY / uiScale) - topPos;
        if (page != Page.OPERATION && inside(x, y, LEFT_X, 104, LEFT_W, 126)) {
            listScroll(listScroll - (int) Math.signum(delta));
            return true;
        }
        if (page != Page.OPERATION && inside(x, y, RIGHT_X, 142, RIGHT_W, 88)) {
            detailScroll(detailScroll - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mouseX / uiScale, mouseY / uiScale, delta);
    }

    private void setPage(Page next) {
        page = next;
        updatePageGeometry();
        catalogue = List.of();
        expandedDimensions.clear();
        selected = null;
        detailMarker = ItemStack.EMPTY;
    }

    private void updatePageGeometry() {
        boolean compact = page == Page.OPERATION;
        imageWidth = compact ? COMPACT_WIDTH : WIDTH;
        imageHeight = compact ? COMPACT_HEIGHT : HEIGHT;
        uiScale =
                compact
                        ? 1.0F
                        : Math.min(
                                1.0F,
                                Math.min(
                                        (float) Math.max(1, width - 16)
                                                / (WIDTH + LEFT_CONTROLS_WIDTH),
                                        (float) Math.max(1, height - 16) / HEIGHT));
        int logicalWidth = (int) Math.floor(width / uiScale);
        int logicalHeight = (int) Math.floor(height / uiScale);
        leftPos =
                compact
                        ? (logicalWidth - COMPACT_WIDTH) / 2
                        : (logicalWidth - WIDTH + LEFT_CONTROLS_WIDTH) / 2;
        topPos = (logicalHeight - imageHeight) / 2;
        menu.setOperationPage(compact);
        if (search != null) {
            search.setX(leftPos + LEFT_X + 26);
            search.setY(topPos + 82);
        }
    }

    private int activeWidth() {
        return page == Page.OPERATION ? COMPACT_WIDTH : WIDTH;
    }

    private int activeHeight() {
        return page == Page.OPERATION ? COMPACT_HEIGHT : HEIGHT;
    }

    private int toLogical(double coordinate) {
        return (int) Math.floor(coordinate / uiScale);
    }

    private void renderInventoryItemTooltip(
            GuiGraphics graphics,
            int logicalMouseX,
            int logicalMouseY,
            int screenMouseX,
            int screenMouseY) {
        for (Slot slot : menu.slots) {
            if (!slot.isActive()
                    || !slot.hasItem()
                    || !inside(
                            logicalMouseX,
                            logicalMouseY,
                            leftPos + slot.x,
                            topPos + slot.y,
                            16,
                            16)) continue;
            graphics.renderTooltip(font, slot.getItem(), screenMouseX, screenMouseY);
            return;
        }
    }

    private void drawModal(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0.0D, 0.0D, 500.0D);
        g.fill(
                0,
                0,
                (int) Math.ceil(width / uiScale),
                (int) Math.ceil(height / uiScale),
                MythicMinerTheme.BACKDROP);
        int x = leftPos + (page == Page.OPERATION ? 6 : 70),
                y = topPos + (page == Page.OPERATION ? 84 : 82),
                w = page == Page.OPERATION ? 164 : 180;
        MythicMinerTheme.panel(
                g, x, y, w, 82, confirmWrite ? MythicMinerTheme.AMBER : MythicMinerTheme.FLUIX);
        g.drawCenteredString(
                font,
                Component.translatable(
                        confirmWrite
                                ? "screen.dimension_tech.structure_operator.confirm.write"
                                : "screen.dimension_tech.structure_operator.confirm.copy"),
                x + w / 2,
                y + 10,
                MythicMinerTheme.TEXT);
        String source =
                confirmWrite && selected != null
                        ? TranslateHelper.structureName(selected.structure()).getString()
                        : markerId(menu.getSlot(StructureDataOperatorBlockEntity.TARGET).getItem());
        g.drawString(
                font,
                font.plainSubstrByWidth(source, w - 12),
                x + 6,
                y + 30,
                MythicMinerTheme.TEXT,
                false);
        MythicMinerTheme.button(
                g,
                font,
                x + 8,
                y + 64,
                72,
                18,
                Component.translatable("screen.dimension_tech.structure_operator.cancel"),
                inside(mouseX, mouseY, x + 8, y + 64, 72, 18),
                true,
                MythicMinerTheme.ERROR);
        MythicMinerTheme.button(
                g,
                font,
                x + 100,
                y + 64,
                72,
                18,
                Component.translatable("screen.dimension_tech.structure_operator.confirm"),
                inside(mouseX, mouseY, x + 100, y + 64, 72, 18),
                true,
                confirmWrite ? MythicMinerTheme.AMBER : MythicMinerTheme.FLUIX);
        g.pose().popPose();
    }

    private String markerId(ItemStack stack) {
        return StructMarkerItem.getMarkerInfo(stack)
                .map(info -> TranslateHelper.structureName(info.structure().id()).getString())
                .orElse(
                        Component.translatable("screen.dimension_tech.structure_operator.no_data")
                                .getString());
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
