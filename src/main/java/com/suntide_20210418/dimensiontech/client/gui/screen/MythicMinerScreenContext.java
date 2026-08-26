package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared surface exposed to the three page renderers. */
interface MythicMinerScreenContext {
    MythicMinerMenu menu();

    Font font();

    int imageWidth();

    int leftPos();

    int topPos();

    float uiScale();

    void openOutputFaceScreen();

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

    void resetExpectedHover();

    void setExpectedHover(ItemStack stack, boolean disabled);
}
