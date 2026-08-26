package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.MythicMinerAnalysisSnapshot;
import java.util.HashSet;
import java.util.Set;

/** Client-only state kept separate from rendering and menu synchronization. */
final class MythicMinerUiState {
    MythicMinerScreen.Page page = MythicMinerScreen.Page.WORK;
    int selectedMarkerSlot = -1;
    int markerInfoScroll;
    int attributeScroll;
    final Set<Integer> expandedUpgradeRows = new HashSet<>();
    boolean markerPropertiesExpanded;
    boolean workStatusExpanded;
    boolean productInfoExpanded;
    boolean showParallelBreakdown;
    MythicMinerAnalysisSnapshot analysis = MythicMinerAnalysisSnapshot.EMPTY;
    int analysisSlot = -1;

    void resetInfoScroll() { markerInfoScroll = 0; }
    void resetAttributeScroll() { attributeScroll = 0; }
    void clearAnalysis() {
        analysis = MythicMinerAnalysisSnapshot.EMPTY;
        analysisSlot = -1;
    }
}
