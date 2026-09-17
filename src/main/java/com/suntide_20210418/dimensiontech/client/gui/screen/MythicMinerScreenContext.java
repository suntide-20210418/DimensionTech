package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;

/** Shared surface exposed to the three page renderers. */
interface MythicMinerScreenContext {
    MythicMinerMenu menu();

    Font font();

    int imageWidth();

    int leftPos();

    int topPos();

    float uiScale();

    int playerInventoryY();

    int attributeScroll();

    void attributeScroll(int value);

    void attributeContentHeight(int value);

    int attributeContentHeight();

    boolean upgradeRowExpanded(int key);

    void toggleUpgradeRow(int key);

    int selectedMarkerSlot();

    int markerInfoScroll();

    void markerInfoScroll(int value);

    void markerInfoContentHeight(int value);

    int markerInfoContentHeight();

    void selectMarkerSlot(int slot);

    boolean markerPropertiesExpanded();

    boolean workStatusExpanded();

    boolean productInfoExpanded();

    boolean showParallelBreakdown();

    void toggleMarkerProperties();

    void toggleWorkStatus();

    void toggleProductInfo();

    void toggleParallelBreakdown();

    List<MythicMinerScreen.ExpectedItemRow> expectedItemRows();

    Set<ResourceLocation> disabledExpectedItems();

    double effectiveDimensionValue();

    double effectiveStructureValue();

    /**
     * Marker cell under the pointer, or {@code -1}. The work lane and the info page's thread selector
     * occupy the same grid, so one value serves both.
     */
    int hoveredMarkerSlot();

    /**
     * True once the asynchronous analysis for the selected slot has arrived.
     *
     * <p>Pages must not read a missing analysis as zero: zero is a legitimate value, and showing it
     * before the result lands would report a wrong number with full confidence.
     */
    boolean markerAnalysisReady();

    /**
     * Forwards a click to the vanilla slot protocol.
     *
     * <p>The lane is drawn by the pages, and the menu deliberately parks its slots off-screen, so the
     * pages own the hit-testing and hand the result back here — which is what keeps pickup, shift-move
     * and drag behaving exactly as they do in any other container.
     */
    void clickContainerSlot(int index, int mouseButton);
}
