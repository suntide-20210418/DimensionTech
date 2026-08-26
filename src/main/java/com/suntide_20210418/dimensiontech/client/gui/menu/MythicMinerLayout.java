package com.suntide_20210418.dimensiontech.client.gui.menu;

/** Shared marker-slot geometry used by the menu and its screen. */
public final class MythicMinerLayout {
    public static final int MARKER_BAY_X = 28;
    public static final int MARKER_BAY_Y = 48;
    public static final int MARKER_SLOT_X_OFFSET = 9;
    public static final int MARKER_SLOT_Y = 58;
    public static final int MARKER_COLUMN_STRIDE = 23;
    public static final int MARKER_ROW_STRIDE = 27;
    public static final int PROGRESS_X_OFFSET = -1;
    public static final int PROGRESS_Y_OFFSET = 19;
    public static final int PROGRESS_WIDTH = 18;
    public static final int PROGRESS_HEIGHT = 3;

    /** Leaves room below the fixed marker bay and the compact dashboard. */
    public static final int BASE_PLAYER_INVENTORY_Y = 258;

    public static final int BASE_MARKER_BAY_HEIGHT = 40;
    public static final int MARKER_BAY_ROW_HEIGHT = 27;
    public static final int FIXED_MARKER_BAY_HEIGHT = 94;
    public static final int MARKER_BAY_HORIZONTAL_PADDING = 16;
    public static final int FIXED_MARKER_BAY_WIDTH =
            MARKER_BAY_HORIZONTAL_PADDING + PROGRESS_WIDTH + 2 * MARKER_COLUMN_STRIDE;
    public static final int MARKER_INFO_GAP = 8;
    public static final int CONTENT_RIGHT_MARGIN = 28;
    public static final int FLUID_PANEL_WIDTH = 48;
    public static final int FLUID_PANEL_GAP = 8;
    public static final int FLUID_PANEL_X = 264;
    public static final int FLUID_TANK_OFFSET_Y = 24;
    public static final int FLUID_CONTROLS_OFFSET_Y = 72;
    public static final int FLUID_TANK_X = 279;
    public static final int FLUID_TANK_Y = 72;
    public static final int FLUID_TANK_WIDTH = 18;
    public static final int FLUID_TANK_HEIGHT = 42;
    public static final int ATTRIBUTE_GAP = 0;

    private MythicMinerLayout() {}

    public static boolean hasFluidInput(int tier) {
        return tier >= 2;
    }

    public static int columnsForSlotCount(int slotCount) {
        return Math.min(3, Math.max(1, slotCount));
    }

    public static int rowsForSlotCount(int slotCount) {
        int columns = columnsForSlotCount(slotCount);
        return Math.max(1, (slotCount + columns - 1) / columns);
    }

    public static int markerSlotX(int slotIndex, int slotCount) {
        int columns = columnsForSlotCount(slotCount);
        int contentWidth = PROGRESS_WIDTH + (columns - 1) * MARKER_COLUMN_STRIDE;
        int centeredOffset = (FIXED_MARKER_BAY_WIDTH - contentWidth) / 2;
        return MARKER_BAY_X + centeredOffset + 1 + (slotIndex % columns) * MARKER_COLUMN_STRIDE;
    }

    public static int markerSlotY(int row) {
        return MARKER_SLOT_Y + row * MARKER_ROW_STRIDE;
    }

    public static int playerInventoryY(int markerRows) {
        return BASE_PLAYER_INVENTORY_Y;
    }

    public static int progressBarX(int slotX) {
        return slotX + PROGRESS_X_OFFSET;
    }

    public static int progressBarY(int slotY) {
        return slotY + PROGRESS_Y_OFFSET;
    }

    public static int markerBayWidth(int slotCount) {
        return FIXED_MARKER_BAY_WIDTH;
    }

    public static int markerInfoX(int slotCount) {
        return MARKER_BAY_X + markerBayWidth(slotCount) + MARKER_INFO_GAP;
    }

    public static int markerInfoWidth(int slotCount, int menuWidth) {
        return menuWidth - CONTENT_RIGHT_MARGIN - markerInfoX(slotCount);
    }

    public static int markerBayHeight(int slotCount) {
        return FIXED_MARKER_BAY_HEIGHT;
    }

    public static int markerInfoHeight(int slotCount) {
        return markerBayHeight(slotCount);
    }

    public static int attributeY(int slotCount) {
        return MARKER_BAY_Y + markerBayHeight(slotCount) + ATTRIBUTE_GAP;
    }

    public static ScissorBounds scaleToScreen(
            int logicalX, int logicalY, int width, int height, float scale) {
        return new ScissorBounds(
                (int) Math.floor(logicalX * scale),
                (int) Math.floor(logicalY * scale),
                (int) Math.ceil((logicalX + width) * scale),
                (int) Math.ceil((logicalY + height) * scale));
    }

    public record ScissorBounds(int left, int top, int right, int bottom) {}
}
