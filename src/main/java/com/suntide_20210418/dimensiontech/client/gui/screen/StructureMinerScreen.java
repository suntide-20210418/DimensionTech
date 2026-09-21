package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerMenu;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import com.suntide_20210418.dimensiontech.structureminer.output.EquipmentDismantler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/** Compact mining console with centered marker lanes and live machine telemetry. */
public final class StructureMinerScreen extends AbstractContainerScreen<StructureMinerMenu>
        implements StructureMinerScreenContext {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("dimension_tech", "guis/void_structre_miner.png");
    private static final ResourceLocation OUTPUT_FACE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("dimension_tech", "guis/output_face_config.png");
    /**
     * Real pixel size of {@link #GUI_TEXTURE}. {@code blit} normalises UVs by the size it is handed
     * ({@code (uOffset + uWidth) / textureWidth}), so it has to match the file exactly or every
     * region silently samples the wrong columns. Two pixels of lie on the width is enough: the energy
     * strip loses its last column to the panel and the fluid overlay reaches back into the energy
     * strip. Package-private so the contract test can pin the pair to the file's own header.
     */
    static final int TEXTURE_WIDTH = 256, TEXTURE_HEIGHT = 256;
    /**
     * Control panel region: (0,0) to (243,161). The dark outline on column 244 / row 162 is outside
     * it, matching the measured 244x162.
     */
    private static final int PANEL_WIDTH = 244, PANEL_HEIGHT = 162;
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 254;
    private static final int INFO_X = 32, INFO_Y = 33, INFO_W = 208, INFO_H = 123;
    private static final int FLUID_X = 18, FLUID_Y = 34, FLUID_W = 8, FLUID_H = 121;

    enum Page {
        WORK,
        INFO,
        ATTRIBUTES
    }

    private static final int WIDTH = 320;

    /** Body ink for the light panel face. Also read by {@code OutputFaceConfigScreen}. */
    static final int INK = StructureMinerTheme.INK;
    /** Energy fill artwork in the texture: (245,0) to (249,143) inclusive, i.e. 5 wide, 144 tall. */
    private static final int ENERGY_U = 245, ENERGY_V = 0, ENERGY_SRC_W = 5, ENERGY_SRC_H = 144;
    /** The well it fills: x 7..11, filled bottom-up, so the last row it can reach is 154. */
    private static final int ENERGY_BAR_X = 7, ENERGY_BAR_BOTTOM = 155, ENERGY_BAR_H = 121;
    private static final int LEFT_CONTROLS_WIDTH = 30;

    /**
     * Left control rail, top to bottom: output face, one-click build, equipment dismantling on the
     * tiers that support it, then redstone. The last two share a slot depending on the tier, so
     * both are resolved by identity rather than by position.
     */
    private static final int PLACE_STRUCTURE_BUTTON_INDEX = 1;

    private static final int EQUIPMENT_DISMANTLING_BUTTON_INDEX = 2;

    /**
     * Item icons are committed at a raised GUI Z, so anything meant to sit on top of one has to be
     * raised past that layer instead of relying on draw order alone.
     */
    private static final double ICON_MARKER_Z = 200.0D;

    private static final int SCREEN_MARGIN = 8;
    private static final int MARKER_INFO_PADDING = 5;
    static final int MARKER_INFO_ROW_HEIGHT = 20;
    private static final int MARKER_INFO_EXPECTED_Y = 180;
    private static final int PARALLEL_BREAKDOWN_HEIGHT = 36;
    static final int INFO_PANEL_Y = 52;
    static final int INFO_SLOT_Y = INFO_PANEL_Y + 22;
    static final int INFO_VIEWPORT_Y = INFO_PANEL_Y + 58;
    private float uiScale = 1.0F;
    private final StructureMinerUiState uiState = new StructureMinerUiState();
    private int markerInfoContentHeight;
    private int attributeContentHeight;
    /**
     * Expected products as reported by the last analysis packet.
     *
     * <p>Not the display source: the marker item carries the same expectations, rewritten only when
     * they change. See {@link #expectedItemRows()} for why the item wins.
     */
    private List<ExpectedItemRow> analysisRows = List.of();
    private Set<ResourceLocation> disabledExpectedItems = Set.of();

    /**
     * {@code null} until the first analysis request, so the first tick can tell "never asked" from
     * "asked with this setting".
     */
    private Boolean lastDismantlingRequest;

    @Override
    public StructureMinerMenu menu() {
        return menu;
    }

    @Override
    public net.minecraft.client.gui.Font font() {
        return font;
    }

    @Override
    public int imageWidth() {
        return imageWidth;
    }

    @Override
    public int leftPos() {
        return leftPos;
    }

    @Override
    public int topPos() {
        return topPos;
    }

    @Override
    public float uiScale() {
        return uiScale;
    }

    @Override
    public int playerInventoryY() {
        return menu.getPlayerInventoryY();
    }

    @Override
    public int attributeScroll() {
        return uiState.attributeScroll;
    }

    @Override
    public void attributeScroll(int value) {
        uiState.attributeScroll = value;
    }

    @Override
    public void attributeContentHeight(int value) {
        attributeContentHeight = value;
    }

    @Override
    public int attributeContentHeight() {
        return attributeContentHeight;
    }

    @Override
    public boolean upgradeRowExpanded(int key) {
        return uiState.expandedUpgradeRows.contains(key);
    }

    @Override
    public void toggleUpgradeRow(int key) {
        if (!uiState.expandedUpgradeRows.add(key)) uiState.expandedUpgradeRows.remove(key);
    }

    @Override
    public int selectedMarkerSlot() {
        return uiState.selectedMarkerSlot;
    }

    @Override
    public int markerInfoScroll() {
        return uiState.markerInfoScroll;
    }

    @Override
    public void markerInfoScroll(int value) {
        uiState.markerInfoScroll = value;
    }

    @Override
    public void markerInfoContentHeight(int value) {
        markerInfoContentHeight = value;
    }

    @Override
    public int markerInfoContentHeight() {
        return markerInfoContentHeight;
    }

    @Override
    public boolean markerPropertiesExpanded() {
        return uiState.markerPropertiesExpanded;
    }

    @Override
    public boolean workStatusExpanded() {
        return uiState.workStatusExpanded;
    }

    @Override
    public boolean productInfoExpanded() {
        return uiState.productInfoExpanded;
    }

    @Override
    public boolean showParallelBreakdown() {
        return uiState.showParallelBreakdown;
    }

    @Override
    public void toggleMarkerProperties() {
        uiState.markerPropertiesExpanded = !uiState.markerPropertiesExpanded;
    }

    @Override
    public void toggleWorkStatus() {
        uiState.workStatusExpanded = !uiState.workStatusExpanded;
    }

    @Override
    public void toggleProductInfo() {
        uiState.productInfoExpanded = !uiState.productInfoExpanded;
    }

    @Override
    public void toggleParallelBreakdown() {
        uiState.showParallelBreakdown = !uiState.showParallelBreakdown;
    }

    /**
     * Expected products for the selected marker, read from the marker item first.
     *
     * <p>The item carries the exact expectations the marking pass persisted. That makes it the right
     * source rather than the async snapshot: when a machine cannot recompute a loot table the
     * snapshot comes back empty, and an empty result printed as data is worse than no result.
     */
    @Override
    public List<ExpectedItemRow> expectedItemRows() {
        Map<ResourceLocation, ExactProbability> stored =
                StructMarkerItem.getExpectedItemCounts(selectedMarkerStack());
        List<ExpectedItemRow> raw = stored.isEmpty() ? analysisRows : rowsFromCounts(stored);
        if (!menu.isEquipmentDismantlingEnabled()) {
            return raw;
        }
        // With equipment dismantling on, equipment must never appear as its own entry. Every
        // equipment item is broken down into the materials that craft it, and those materials are
        // summed with any other source of the same item so the page shows one unified resource
        // total. Re-dismantling is a no-op for items that are already materials, so this is safe
        // whether the rows came from the persisted marker item or from the already-dismantled
        // analysis packet.
        return dismantledRows(raw);
    }

    /**
     * Breaks down every equipment row into the materials that craft it and merges those materials with
     * any other source of the same item, so the result is a single unified resource total with no
     * equipment left standing.
     *
     * <p>The marker item persists raw loot (including equipment) independent of the miner's dismantling
     * toggle, so the breakdown has to happen here, on the client. {@link
     * EquipmentDismantler#dismantle} is passed a {@code null} level: the primary-material path it uses
     * for the common armour and tool cases needs no recipe manager, and only the rare recipe-lookup
     * fallback is skipped — leaving such an item intact is acceptable rather than guessing at it.
     */
    private static List<ExpectedItemRow> dismantledRows(List<ExpectedItemRow> rows) {
        Map<ResourceLocation, Double> expectations = new LinkedHashMap<>();
        for (ExpectedItemRow row : rows) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(row.item());
            if (id != null) expectations.put(id, row.expected());
        }
        Map<ResourceLocation, Double> dismantled =
                EquipmentDismantler.dismantleExpectations(null, expectations);
        List<ExpectedItemRow> result = new ArrayList<>(dismantled.size());
        dismantled.forEach(
                (id, value) ->
                        BuiltInRegistries.ITEM
                                .getOptional(id)
                                .ifPresent(item -> result.add(new ExpectedItemRow(item, value))));
        result.sort(
                Comparator.comparingDouble(ExpectedItemRow::expected)
                        .reversed()
                        .thenComparing(row -> BuiltInRegistries.ITEM.getKey(row.item()).toString()));
        return List.copyOf(result);
    }

    @Override
    public Set<ResourceLocation> disabledExpectedItems() {
        return disabledExpectedItems;
    }

    /**
     * The marker item is the authority for these numbers. It is what the marking pass persisted, so
     * it still reads correctly on a machine whose own recomputation yields nothing — which is exactly
     * the case where the async snapshot shows zero and looks like a real reading.
     */
    @Override
    public double effectiveDimensionValue() {
        return StructMarkerItem.getDimensionValue(selectedMarkerStack());
    }

    @Override
    public double effectiveStructureValue() {
        return StructMarkerItem.getStructureValue(selectedMarkerStack());
    }

    @Override
    public int hoveredMarkerSlot() {
        return uiState.hoveredMarkerSlot;
    }

    @Override
    /**
     * True when the selected marker actually carries analysis data.
     *
     * <p>Judged from the item, not from the packet's arrival. A packet can land carrying zeros —
     * the server returns an empty snapshot whenever it cannot recompute the loot table — and
     * accepting that as "ready" is what printed zeroes as if they were readings.
     */
    public boolean markerAnalysisReady() {
        ItemStack stack = selectedMarkerStack();
        return !stack.isEmpty()
                && StructMarkerItem.getMarkerInfo(stack).isPresent()
                && StructMarkerItem.getAnalysisStatus(stack) != AnalysisStatus.LEGACY;
    }

    /** The selected slot's item, or {@code EMPTY}. Safe to call with nothing selected. */
    private ItemStack selectedMarkerStack() {
        int slot = uiState.selectedMarkerSlot;
        return slot < 0 || slot >= menu.slots.size()
                ? ItemStack.EMPTY
                : menu.slots.get(slot).getItem();
    }

    /** Turns the marker's persisted exact expectations into display rows, sorted like the packet's. */
    private static List<ExpectedItemRow> rowsFromCounts(
            Map<ResourceLocation, ExactProbability> counts) {
        List<ExpectedItemRow> rows = new ArrayList<>(counts.size());
        for (Map.Entry<ResourceLocation, ExactProbability> entry : counts.entrySet()) {
            BuiltInRegistries.ITEM
                    .getOptional(entry.getKey())
                    .ifPresent(
                            item ->
                                    rows.add(
                                            new ExpectedItemRow(
                                                    item, entry.getValue().doubleValue())));
        }
        rows.sort(
                Comparator.comparingDouble(ExpectedItemRow::expected)
                        .reversed()
                        .thenComparing(
                                row -> BuiltInRegistries.ITEM.getKey(row.item()).toString()));
        return List.copyOf(rows);
    }

    @Override
    public void clickContainerSlot(int index, int mouseButton) {
        if (index < 0 || index >= menu.getContainerSlotCount()) return;
        slotClicked(
                menu.slots.get(index),
                index,
                mouseButton,
                net.minecraft.world.inventory.ClickType.PICKUP);
    }

    /**
     * Resolves the marker cell under a panel-local point, or {@code -1}.
     *
     * <p>Deliberately does not consult {@code menu.slots}: the menu parks those off-screen, so the lane
     * is the only authority on where its cells are drawn.
     */
    private int markerSlotAt(double localX, double localY) {
        if (uiState.page == Page.ATTRIBUTES) return -1;
        return StructureMinerInfoLayout.markerSlotAt(localX, localY, menu.getContainerSlotCount());
    }

    private static final int TAB_Y = 31;
    private static final int TAB_HEIGHT = 13;

    public StructureMinerScreen(StructureMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
        inventoryLabelY = Integer.MIN_VALUE;
        titleLabelY = Integer.MIN_VALUE;
    }

    @Override
    protected void init() {
        super.init();
        float widthScale =
                (float) Math.max(1, width - SCREEN_MARGIN * 2) / (imageWidth + LEFT_CONTROLS_WIDTH);
        float heightScale = (float) Math.max(1, height - SCREEN_MARGIN * 2) / imageHeight;
        uiScale = Math.min(1.0F, Math.min(widthScale, heightScale));
        int logicalWidth = (int) Math.floor(width / uiScale);
        int logicalHeight = (int) Math.floor(height / uiScale);
        leftPos = (logicalWidth - imageWidth + LEFT_CONTROLS_WIDTH) / 2;
        topPos = (logicalHeight - imageHeight) / 2;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (uiState.page != Page.INFO || uiState.selectedMarkerSlot < 0) return;
        // Compared against what was last requested, not against the snapshot's own flag. The old
        // comparison made every disagreeing reply clear the analysis and ask again, so the request
        // cycle never settled: it ran for as long as the screen stayed open.
        boolean enabled = menu.isEquipmentDismantlingEnabled();
        if (Boolean.valueOf(enabled).equals(lastDismantlingRequest)) return;
        lastDismantlingRequest = enabled;
        ModNetwork.requestStructureMinerAnalysis(menu.containerId, uiState.selectedMarkerSlot);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int logicalMouseX = toLogical(mouseX);
        int logicalMouseY = toLogical(mouseY);
        // Traced here rather than inside the pages so the lane has one source of hover truth for both
        // the work page's lane and the info page's selector, which share the same grid.
        uiState.hoveredMarkerSlot = markerSlotAt(logicalMouseX - leftPos, logicalMouseY - topPos);
        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1.0F);
        super.render(graphics, logicalMouseX, logicalMouseY, partialTick);
        // AbstractContainerScreen renders slots/labels after renderBg. Paint the page a second
        // time on top so slot/background passes can never obscure information text.
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0.0D);
        drawPage(graphics, logicalMouseX - leftPos, logicalMouseY - topPos);
        graphics.pose().popPose();
        if (!menu.telemetrySnapshot().structureComplete()) {
            GuiText.centered(
                    graphics,
                    font,
                    Component.translatable(
                            "screen.dimension_tech.structure_miner.structure_incomplete"),
                    leftPos + imageWidth / 2,
                    topPos + 46,
                    INK);
        }
        graphics.pose().popPose();

        // AbstractContainerScreen tracks hovered slots while rendering under the
        // logical UI scale. Re-render the item tooltip in screen coordinates so
        // inventory hover information remains visible at every scale.
        renderInventoryItemTooltip(graphics, logicalMouseX, logicalMouseY, mouseX, mouseY);

        if (logicalMouseX >= leftPos + 7
                && logicalMouseX < leftPos + 11
                && logicalMouseY >= topPos + 34
                && logicalMouseY < topPos + 155) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(
                    Component.translatable("screen.dimension_tech.structure_miner.energy_tooltip"));
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.structure_miner.energy_value",
                            menu.getEnergyStored(),
                            menu.getEnergyCapacity()));
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.structure_miner.energy_consumption",
                            menu.getEffectiveEnergyConsumption()));
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
        if (logicalMouseX >= leftPos + 18
                && logicalMouseX < leftPos + 26
                && logicalMouseY >= topPos + 34
                && logicalMouseY < topPos + 155) {
            net.minecraft.world.level.material.Fluid fluid =
                    menu.hasFluid() ? menu.getFluid() : menu.getRequiredFluid();
            Component fluidName =
                    fluid == null || fluid == Fluids.EMPTY
                            ? Component.translatable(
                                    "screen.dimension_tech.structure_miner.fluid_empty")
                            : Component.translatable(fluid.getFluidType().getDescriptionId());
            graphics.renderTooltip(
                    font,
                    List.of(
                            fluidName,
                            Component.translatable(
                                    "screen.dimension_tech.structure_miner.fluid_amount",
                                    menu.getFluidAmount(),
                                    menu.getFluidCapacity()),
                            Component.translatable(
                                    menu.isAutoExtractFluidEnabled()
                                            ? "screen.dimension_tech.structure_miner.auto_extract_enabled"
                                            : "screen.dimension_tech.structure_miner.auto_extract_disabled")),
                    Optional.empty(),
                    mouseX,
                    mouseY);
        }
        int buttonIndex = externalButtonAt(logicalMouseX - leftPos, logicalMouseY - topPos);
        if (buttonIndex >= 0) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(externalButtonTooltipKey(buttonIndex)),
                    mouseX,
                    mouseY);
        }
        // Page-owned hovers go through the page router, so a tooltip can only ever describe something
        // the page actually drew. It is gated to the page canvas: below it, the player inventory owns
        // the hover, and the inventory's own item tooltip must not be covered. Panel-local coordinates
        // feed its hit tests; the raw GUI-scaled pair is handed over separately because renderTooltip
        // draws in window space and ignores the pose.
        if (insidePageCanvas(logicalMouseX - leftPos, logicalMouseY - topPos)) {
            StructureMinerPageRenderer.renderTooltip(
                    this,
                    uiState.page,
                    graphics,
                    logicalMouseX - leftPos,
                    logicalMouseY - topPos,
                    mouseX,
                    mouseY);
        }
    }

    private void renderInventoryItemTooltip(
            GuiGraphics graphics, int logicalMouseX, int logicalMouseY, int screenX, int screenY) {
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            if (!slot.hasItem()
                    || !inside(
                            logicalMouseX,
                            logicalMouseY,
                            leftPos + slot.x,
                            topPos + slot.y,
                            16,
                            16)) continue;
            graphics.renderTooltip(font, slot.getItem(), screenX, screenY);
            return;
        }
    }

    /**
     * Scrolls the current page by a page or to an end.
     *
     * <p>Keys are GLFW constants: 266 PAGE_UP, 267 PAGE_DOWN, 268 HOME, 269 END. A page step leaves
     * one row of overlap so the reader keeps context, exactly like a text editor's pager.
     */
    private boolean scrollByKey(int keyCode) {
        int viewportH = scrollViewportH();
        if (viewportH <= 0) return false;
        int range = Math.max(0, scrollContentHeight() - viewportH);
        if (range == 0) return false;
        int step = Math.max(1, viewportH - StructureMinerInfoLayout.ROW_H_INTERACTIVE);
        int target;
        switch (keyCode) {
            case 267 -> target = pageScroll() + step;
            case 266 -> target = pageScroll() - step;
            case 269 -> target = range;
            case 268 -> target = 0;
            default -> {
                return false;
            }
        }
        pageScroll(Math.max(0, Math.min(range, target)));
        return true;
    }

    // --- scrollbar ---------------------------------------------------------

    /**
     * Top of the scrolling page's viewport, or {@code -1} when the current page does not scroll.
     *
     * <p>The scrollbar is shared chrome between the two scrolling pages, exactly like the tab strip,
     * so the screen owns the gesture while the pages only own their content.
     */
    private int scrollViewportY() {
        return switch (uiState.page) {
            case INFO -> StructureMinerInfoLayout.INFO_LIST_Y;
            case ATTRIBUTES -> StructureMinerInfoLayout.ATTR_LIST_Y;
            case WORK -> -1;
        };
    }

    private int scrollViewportH() {
        return switch (uiState.page) {
            case INFO -> StructureMinerInfoLayout.INFO_LIST_H;
            case ATTRIBUTES -> StructureMinerInfoLayout.ATTR_LIST_H;
            case WORK -> 0;
        };
    }

    /**
     * Height the pages last reported. The pages write it while drawing, so it is always the height of
     * what is on screen rather than a value guessed from the menu.
     */
    private int scrollContentHeight() {
        return uiState.page == Page.ATTRIBUTES ? attributeContentHeight : markerInfoContentHeight;
    }

    private int pageScroll() {
        return uiState.page == Page.ATTRIBUTES ? uiState.attributeScroll : uiState.markerInfoScroll;
    }

    private void pageScroll(int value) {
        if (uiState.page == Page.ATTRIBUTES) {
            uiState.attributeScroll = value;
        } else {
            uiState.markerInfoScroll = value;
        }
    }

    /** Grabs the scrollbar when the pointer is on the bar of a page that actually has one. */
    private boolean beginScrollbarDrag(double localX, double localY) {
        int viewportY = scrollViewportY();
        if (viewportY < 0) return false;
        int viewportH = scrollViewportH();
        if (!StructureMinerInfoLayout.scrollColumnContains(localX, localY, viewportY, viewportH)) {
            return false;
        }
        // A drag on a bar with nothing to reveal would latch the state and keep swallowing events.
        if (scrollContentHeight() <= viewportH) return false;
        uiState.draggingScrollbar = true;
        uiState.scrollbarDragOriginY = (int) Math.floor(localY);
        uiState.scrollbarDragOriginScroll = pageScroll();
        return true;
    }

    private boolean updateScrollbarDrag(double localY) {
        if (!uiState.draggingScrollbar) return false;
        pageScroll(
                StructureMinerInfoLayout.scrollFromDrag(
                        uiState.scrollbarDragOriginScroll,
                        uiState.scrollbarDragOriginY,
                        localY,
                        scrollContentHeight(),
                        scrollViewportH()));
        return true;
    }

    private int toLogical(double coordinate) {
        return (int) Math.floor(coordinate / uiScale);
    }

    /**
     * True when a panel-local point sits inside the current page's canvas. The canvas stops above
     * the player inventory, so click, drag and wheel handling bound to it never leak into the
     * slots underneath the machine.
     */
    private boolean insidePageCanvas(double localX, double localY) {
        return StructureMinerInfoLayout.inside(
                localX,
                localY,
                StructureMinerInfoLayout.INFO_X,
                StructureMinerInfoLayout.INFO_Y,
                StructureMinerInfoLayout.INFO_W,
                StructureMinerInfoLayout.INFO_H);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX /= uiScale;
        mouseY /= uiScale;
        if (button == 0 && selectPageAt(mouseX, mouseY)) {
            return true;
        }
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        if (button == 0 && handleExternalButtonClick(localX, localY)) return true;
        // Ahead of the page dispatch on purpose: the info and attributes pages consume every click
        // inside their canvas, so a scrollbar grab placed after them would never be reached.
        if (button == 0 && beginScrollbarDrag(localX, localY)) return true;
        if (StructureMinerPageRenderer.mouseClicked(this, uiState.page, localX, localY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int externalButtonAt(double x, double y) {
        int index =
                (int)
                        ((y - StructureMinerInfoLayout.EXTERNAL_BUTTON_Y)
                                / StructureMinerInfoLayout.EXTERNAL_BUTTON_STRIDE);
        int count = menu.supportsEquipmentDismantling() ? 4 : 3;
        return index >= 0
                        && index < count
                        && inside(
                                x,
                                y,
                                StructureMinerInfoLayout.EXTERNAL_BUTTON_X,
                                StructureMinerInfoLayout.EXTERNAL_BUTTON_Y
                                        + index * StructureMinerInfoLayout.EXTERNAL_BUTTON_STRIDE,
                                20,
                                20)
                ? index
                : -1;
    }

    private boolean handleExternalButtonClick(double x, double y) {
        int index =
                (int)
                        ((y - StructureMinerInfoLayout.EXTERNAL_BUTTON_Y)
                                / StructureMinerInfoLayout.EXTERNAL_BUTTON_STRIDE);
        if (!inside(
                x,
                y,
                StructureMinerInfoLayout.EXTERNAL_BUTTON_X,
                StructureMinerInfoLayout.EXTERNAL_BUTTON_Y
                        + index * StructureMinerInfoLayout.EXTERNAL_BUTTON_STRIDE,
                20,
                20)) return false;
        if (index == 0) {
            Minecraft.getInstance().setScreen(new OutputFaceConfigScreen(this, menu));
            return true;
        } else if (index == 1) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 3);
        } else if (index == 2 && menu.supportsEquipmentDismantling()) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 4);
        } else if (index == redstoneButtonIndex()) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 2);
        } else return false;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (uiState.draggingScrollbar) {
            uiState.draggingScrollbar = false;
            return true;
        }
        double localX = mouseX / uiScale - leftPos;
        double localY = mouseY / uiScale - topPos;
        if (uiState.page != Page.WORK && insidePageCanvas(localX, localY)) return true;
        return super.mouseReleased(mouseX / uiScale, mouseY / uiScale, button);
    }

    /**
     * Escape still closes the screen; the scroll keys are handled by the page instead.
     *
     * <p>Keyboard scrolling is a third affordance on purpose. The drag target is only a few pixels
     * wide and the wheel needs the pointer somewhere sensible, so a key path keeps both off the
     * critical path of "I need to see the rest of this page".
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (uiState.page != Page.WORK) {
            // 256 = GLFW escape, which must still close the screen.
            if (keyCode == 256) return super.keyPressed(keyCode, scanCode, modifiers);
            scrollByKey(keyCode);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Checked before the page guard below: that guard exists to stop drags reaching vanilla slot
        // handling, and it used to swallow scrollbar drags too, leaving the bar inert.
        if (updateScrollbarDrag(mouseY / uiScale - topPos)) return true;
        double localX = mouseX / uiScale - leftPos;
        double localY = mouseY / uiScale - topPos;
        if (uiState.page != Page.WORK && insidePageCanvas(localX, localY)) return true;
        return super.mouseDragged(
                mouseX / uiScale, mouseY / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    /**
     * Wheel handling for the scrolling pages.
     *
     * <p>The wheel applies anywhere over the page canvas — the selector lane and the list alike —
     * but never over the player inventory below the canvas, which is a separate surface. A narrower
     * viewport-only target was the previous failure mode: the wheel reported success to the game
     * while nothing on screen moved.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double localX = mouseX / uiScale - leftPos;
        double localY = mouseY / uiScale - topPos;
        // The work page has nothing to scroll; the scrolling pages scroll anywhere over their
        // canvas but not over the player inventory below it, which is disjoint from the page.
        if (uiState.page == Page.WORK || !insidePageCanvas(localX, localY)) return false;
        return StructureMinerPageRenderer.mouseScrolled(this, uiState.page, delta);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0.0D);
        drawPanel(graphics, 0, 0, imageWidth, imageHeight);
        drawHeader(graphics);
        drawEnergyRail(graphics);
        drawFluidGauge(graphics);
        drawExternalButtons(graphics, mouseX - leftPos, mouseY - topPos);
        graphics.pose().popPose();
    }

    private void drawFluidGauge(GuiGraphics g) {
        int capacity = Math.max(1, menu.getFluidCapacity());
        int amount = Math.max(0, Math.min(capacity, menu.getFluidAmount()));
        int filled = fillPixels(amount, capacity, FLUID_H);
        net.minecraft.world.level.material.Fluid fluid =
                menu.hasFluid() ? menu.getFluid() : menu.getRequiredFluid();
        boolean rendered = false;
        if (filled > 0 && fluid != null && fluid != Fluids.EMPTY) {
            ResourceLocation still = IClientFluidTypeExtensions.of(fluid).getStillTexture();
            if (still != null) {
                TextureAtlasSprite sprite =
                        Minecraft.getInstance()
                                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                                .apply(still);
                int tint = IClientFluidTypeExtensions.of(fluid).getTintColor();
                g.setColor(
                        ((tint >> 16) & 0xFF) / 255.0F,
                        ((tint >> 8) & 0xFF) / 255.0F,
                        (tint & 0xFF) / 255.0F,
                        1.0F);
                int top = FLUID_Y + FLUID_H - filled;
                // A still-fluid texture is greyscale and only becomes fluid-coloured once tinted,
                // and it tiles at its own 16x16 resolution rather than stretching to the tank.
                g.enableScissor(
                        toScreenX(FLUID_X),
                        toScreenY(top),
                        toScreenX(FLUID_X + FLUID_W),
                        toScreenY(FLUID_Y + FLUID_H));
                for (int tileY = top; tileY < FLUID_Y + FLUID_H; tileY += 16) {
                    for (int tileX = FLUID_X; tileX < FLUID_X + FLUID_W; tileX += 16) {
                        g.blit(tileX, tileY, 0, 16, 16, sprite);
                    }
                }
                g.disableScissor();
                g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                rendered = true;
            }
        }
        if (filled > 0 && !rendered) {
            g.fill(
                    FLUID_X,
                    FLUID_Y + FLUID_H - filled,
                    FLUID_X + FLUID_W,
                    FLUID_Y + FLUID_H,
                    StructureMinerTheme.FLUID);
        }
        g.blit(
                GUI_TEXTURE,
                FLUID_X + FLUID_W - 5,
                FLUID_Y,
                0,
                250,
                0,
                5,
                120,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT);
    }

    /**
     * Projects a panel-local x into the absolute GUI space that scissor rectangles live in. {@code
     * GuiGraphics.enableScissor} applies the window GUI scale only and ignores the render pose, so a
     * scissor built from panel-local coordinates clips in the wrong place entirely.
     */
    private int toScreenX(int panelX) {
        return Math.round((leftPos + panelX) * uiScale);
    }

    /** Panel-local counterpart of {@link #toScreenX(int)}. */
    private int toScreenY(int panelY) {
        return Math.round((topPos + panelY) * uiScale);
    }

    private void drawExternalButtons(GuiGraphics g, int mouseX, int mouseY) {
        int x = StructureMinerInfoLayout.EXTERNAL_BUTTON_X;
        int[] iconU = {32, 0, 32};
        int[] iconV = {16, 16, 32};
        int count = menu.supportsEquipmentDismantling() ? 4 : 3;
        for (int i = 0; i < count; i++) {
            int y =
                    StructureMinerInfoLayout.EXTERNAL_BUTTON_Y
                            + i * StructureMinerInfoLayout.EXTERNAL_BUTTON_STRIDE;
            boolean hovered = inside(mouseX, mouseY, x, y, 20, 20);
            StructureMinerSpriteRenderer.smallButton(g, x, y, false);
            if (hovered) StructureMinerSpriteRenderer.smallButton(g, x, y, true);
            if (i == redstoneButtonIndex()) {
                drawRedstoneControlIcon(g, x, y);
            } else {
                StructureMinerSpriteRenderer.externalIcon(
                        g,
                        x + StructureMinerInfoLayout.EXTERNAL_ICON_INSET,
                        y + StructureMinerInfoLayout.EXTERNAL_ICON_INSET,
                        iconU[i],
                        iconV[i]);
            }
        }
    }

    /**
     * The torch is the control's identity, so it is drawn in every mode; the mode itself only adds
     * or removes the "off" marker stacked on top of it. Both sit
     * {@link StructureMinerInfoLayout#REDSTONE_ICON_RISE} pixels above the ordinary icon slots.
     */
    private void drawRedstoneControlIcon(GuiGraphics g, int buttonX, int buttonY) {
        int iconX = buttonX + StructureMinerInfoLayout.EXTERNAL_ICON_INSET;
        int iconY =
                buttonY
                        + StructureMinerInfoLayout.EXTERNAL_ICON_INSET
                        - StructureMinerInfoLayout.REDSTONE_ICON_RISE;
        g.renderItem(new ItemStack(Items.REDSTONE_TORCH), iconX, iconY);
        if (menu.isRedstoneControlEnabled()) return;
        // Item icons are committed on a raised Z, so the marker has to clear that layer to land on
        // top of the torch instead of behind it.
        g.pose().pushPose();
        g.pose().translate(0.0D, 0.0D, ICON_MARKER_Z);
        StructureMinerSpriteRenderer.externalIcon(
                g,
                iconX,
                iconY,
                StructureMinerSpriteRenderer.REDSTONE_OFF_U,
                StructureMinerSpriteRenderer.REDSTONE_OFF_V);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 4, 4, INK, false);
        if (uiState.page == Page.WORK) {
            GuiText.centered(
                    graphics,
                    font,
                    playerInventoryTitle,
                    imageWidth / 2,
                    inventoryLabelY,
                    INK);
        }
    }

    private void drawPage(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.enableScissor(
                toScreenX(INFO_X),
                toScreenY(INFO_Y),
                toScreenX(INFO_X + INFO_W),
                toScreenY(INFO_Y + INFO_H));
        StructureMinerPageRenderer.render(this, graphics, uiState.page);
        graphics.disableScissor();
    }

    private int redstoneButtonIndex() {
        return menu.supportsEquipmentDismantling()
                ? EQUIPMENT_DISMANTLING_BUTTON_INDEX + 1
                : EQUIPMENT_DISMANTLING_BUTTON_INDEX;
    }

    /** Resolves a hovered button's tooltip from what the button is, not from where it sits. */
    private String externalButtonTooltipKey(int buttonIndex) {
        if (buttonIndex == redstoneButtonIndex()) {
            return "screen.dimension_tech.structure_miner.redstone_control_"
                    + (menu.isRedstoneControlEnabled() ? "on" : "off");
        }
        if (menu.supportsEquipmentDismantling()
                && buttonIndex == EQUIPMENT_DISMANTLING_BUTTON_INDEX) {
            return "screen.dimension_tech.structure_miner.equipment_dismantling_"
                    + (menu.isEquipmentDismantlingEnabled() ? "on" : "off");
        }
        // Everything else the rail accepts is either the output-face or the build button.
        return buttonIndex == PLACE_STRUCTURE_BUTTON_INDEX
                ? "screen.dimension_tech.structure_miner.place_structure"
                : "screen.dimension_tech.structure_miner.output_face";
    }

    private boolean selectPageAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (y < StructureMinerInfoLayout.BUTTON_Y || y >= StructureMinerInfoLayout.BUTTON_Y + 16)
            return false;
        if (x < StructureMinerInfoLayout.WORK_BUTTON_X
                || x >= StructureMinerInfoLayout.ATTR_BUTTON_X + 32) return false;
        int index =
                x < StructureMinerInfoLayout.INFO_BUTTON_X
                        ? 0
                        : x < StructureMinerInfoLayout.ATTR_BUTTON_X ? 1 : 2;
        uiState.page = Page.values()[Math.min(Page.values().length - 1, index)];
        uiState.markerInfoScroll = 0;
        return true;
    }

    @Override
    public void selectMarkerSlot(int slot) {
        if (slot < 0 || slot >= menu.getContainerSlotCount()) return;
        if (uiState.selectedMarkerSlot != slot) {
            uiState.markerInfoScroll = 0;
            clearEffectiveAnalysis();
        }
        uiState.selectedMarkerSlot = slot;
        if (StructMarkerItem.getMarkerInfo(menu.slots.get(slot).getItem()).isPresent()) {
            ModNetwork.requestStructureMinerAnalysis(menu.containerId, slot);
        }
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blit(
                GUI_TEXTURE,
                x,
                y,
                0,
                0,
                0,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT);
        graphics.blit(
                GUI_TEXTURE,
                x + 35,
                y + 167,
                0,
                35,
                167,
                175,
                87,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT);
    }

    private void drawHeader(GuiGraphics graphics) {
        int tabWidth = 32;
        String[] labels = {
            "screen.dimension_tech.structure_miner.tab.work",
            "screen.dimension_tech.structure_miner.tab.info",
            "screen.dimension_tech.structure_miner.tab.attributes"
        };
        Page[] pages = Page.values();
        for (int index = 0; index < pages.length; index++) {
            int x =
                    switch (index) {
                        case 0 -> StructureMinerInfoLayout.WORK_BUTTON_X;
                        case 1 -> StructureMinerInfoLayout.INFO_BUTTON_X;
                        default -> StructureMinerInfoLayout.ATTR_BUTTON_X;
                    };
            boolean selected = uiState.page == pages[index];
            StructureMinerSpriteRenderer.button(graphics, x, StructureMinerInfoLayout.BUTTON_Y, selected);
            Component label = Component.translatable(labels[index]);
            String text = font.plainSubstrByWidth(label.getString(), 30);
            graphics.drawString(font, text, x + 16 - font.width(text) / 2, 19, INK, false);
        }
    }

    private void clearEffectiveAnalysis() {
        uiState.clearAnalysis();
        analysisRows = List.of();
        disabledExpectedItems = Set.of();
    }

    public static void receiveAnalysis(
            int containerId,
            int slot,
            double dimensionValue,
            double structureValue,
            boolean equipmentDismantling,
            Map<ResourceLocation, Double> itemExpectations,
            Set<ResourceLocation> disabledItems) {
        if (!(Minecraft.getInstance().screen instanceof StructureMinerScreen screen)
                || screen.menu.containerId != containerId
                || screen.uiState.selectedMarkerSlot != slot) {
            return;
        }
        screen.applyAnalysis(
                slot,
                dimensionValue,
                structureValue,
                equipmentDismantling,
                itemExpectations,
                disabledItems);
    }

    private void applyAnalysis(
            int slot,
            double dimensionValue,
            double structureValue,
            boolean equipmentDismantling,
            Map<ResourceLocation, Double> itemExpectations,
            Set<ResourceLocation> disabledItems) {
        List<ExpectedItemRow> refreshed = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Double> entry : itemExpectations.entrySet()) {
            BuiltInRegistries.ITEM
                    .getOptional(entry.getKey())
                    .ifPresent(item -> refreshed.add(new ExpectedItemRow(item, entry.getValue())));
        }
        refreshed.sort(
                Comparator.comparingDouble(ExpectedItemRow::expected)
                        .reversed()
                        .thenComparing(
                                row -> BuiltInRegistries.ITEM.getKey(row.item()).toString()));
        analysisRows = List.copyOf(refreshed);
        disabledExpectedItems = Set.copyOf(disabledItems);
        uiState.analysis =
                new com.suntide_20210418.dimensiontech.block.entity.StructureMinerAnalysisSnapshot(
                        dimensionValue,
                        structureValue,
                        equipmentDismantling,
                        itemExpectations,
                        disabledItems);
        uiState.analysisSlot = slot;
    }

    /**
     * Fills the energy well bottom-up.
     *
     * <p>Source and destination are aligned at their bottom edges: strip row 143 is the row that
     * lands on the well's last filled row (154), so a partial bar takes the bottom {@code filled}
     * rows of the strip rather than the top ones.
     */
    private void drawEnergyRail(GuiGraphics graphics) {
        int filled =
                fillPixels(
                        menu.getEnergyStored(), Math.max(1, menu.getEnergyCapacity()), ENERGY_BAR_H);
        if (filled > 0) {
            graphics.blit(
                    GUI_TEXTURE,
                    ENERGY_BAR_X,
                    ENERGY_BAR_BOTTOM - filled,
                    0,
                    ENERGY_U,
                    ENERGY_V + ENERGY_SRC_H - filled,
                    ENERGY_SRC_W,
                    filled,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT);
        }
    }

    static String formatDecimal(int hundredths) {
        return String.format(java.util.Locale.ROOT, "%.2f", hundredths / 100.0D);
    }

    /** Compaction for energy figures, which routinely run into six and seven digits. */
    static String formatCompact(int value) {
        if (value >= 1_000_000_000) {
            return String.format(java.util.Locale.ROOT, "%.2fB", value / 1_000_000_000.0D);
        }
        if (value >= 1_000_000) {
            return String.format(java.util.Locale.ROOT, "%.2fM", value / 1_000_000.0D);
        }
        if (value >= 1_000) {
            return String.format(java.util.Locale.ROOT, "%.2fK", value / 1_000.0D);
        }
        return Integer.toString(value);
    }

    /** A percentage held as hundredths, with trailing zeros stripped: 20 -> "20", 25 -> "0.25". */
    static String formatPercent(int hundredths) {
        return java.math.BigDecimal.valueOf(hundredths, 2)
                .stripTrailingZeros()
                .toPlainString();
    }

    /** A configured percentage, already in whole percent: 20.0 -> "20", 12.5 -> "12.5". */
    static String formatConfigPercent(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    static String formatRatio(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    static int fillPixels(int amount, int capacity, int pixels) {
        if (amount <= 0 || capacity <= 0 || pixels <= 0) return 0;
        float fillRatio = (float) amount / (float) capacity;
        fillRatio = Math.max(0.0F, Math.min(1.0F, fillRatio));
        return Math.min(pixels, (int) (pixels * fillRatio));
    }

    record ExpectedItemRow(Item item, double expected) {}
}
