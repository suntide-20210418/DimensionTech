package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.MythicMinerAnalysisSnapshot;
import java.util.HashSet;
import java.util.Set;

/** Client-only state kept separate from rendering and menu synchronization. */
final class MythicMinerUiState {
    MythicMinerScreen.Page page = MythicMinerScreen.Page.WORK;
    int selectedMarkerSlot = -1;
    int hoveredMarkerSlot = -1;
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
    MythicMinerAnalysisSnapshot analysis = MythicMinerAnalysisSnapshot.EMPTY;
    int analysisSlot = -1;

    void resetInfoScroll() {
        markerInfoScroll = 0;
    }

    void resetAttributeScroll() {
        attributeScroll = 0;
    }

    void clearAnalysis() {
        analysis = MythicMinerAnalysisSnapshot.EMPTY;
        analysisSlot = -1;
    }
}
