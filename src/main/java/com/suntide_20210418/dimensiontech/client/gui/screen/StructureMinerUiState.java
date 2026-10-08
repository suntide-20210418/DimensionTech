package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.StructureMinerAnalysisSnapshot;
import java.util.HashSet;
import java.util.Set;

/** Client-only state kept separate from rendering and menu synchronization. */
final class StructureMinerUiState {
    StructureMinerScreen.Page page = StructureMinerScreen.Page.WORK;
    int selectedMarkerSlot = -1;
    int hoveredMarkerSlot = -1;

    /**
     * Which bulk button the pointer is over, traced once per frame by the info page.
     *
     * <p>Held here rather than recomputed by the draw pass because the buttons are drawn during the
     * page render and the answer depends on the page's scroll offset, which the draw pass is in the
     * middle of applying. Tracing first and drawing from the trace keeps both on the same frame.
     */
    StructureMinerInfoPage.BulkAction hoveredBulkAction = StructureMinerInfoPage.BulkAction.NONE;

    int markerInfoScroll;
    int attributeScroll;

    /**
     * Scrollbar drag, kept with the window rather than with a page: both scrolling pages share one
     * scrollbar column, so the gesture belongs to the screen that owns that column.
     */
    boolean draggingScrollbar;

    int scrollbarDragOriginY;
    int scrollbarDragOriginScroll;
    final Set<Integer> expandedUpgradeRows = new HashSet<>();
    boolean markerPropertiesExpanded;
    boolean workStatusExpanded;
    boolean productInfoExpanded;
    boolean showParallelBreakdown;
    boolean outputFaceConfig;
    StructureMinerAnalysisSnapshot analysis = StructureMinerAnalysisSnapshot.EMPTY;
    int analysisSlot = -1;

    void resetInfoScroll() {
        markerInfoScroll = 0;
    }

    void resetAttributeScroll() {
        attributeScroll = 0;
    }

    void clearAnalysis() {
        analysis = StructureMinerAnalysisSnapshot.EMPTY;
        analysisSlot = -1;
    }
}
