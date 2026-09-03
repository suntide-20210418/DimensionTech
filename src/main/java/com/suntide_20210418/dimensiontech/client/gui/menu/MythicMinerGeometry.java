package com.suntide_20210418.dimensiontech.client.gui.menu;

/** Immutable logical rectangles shared by the miner screen and its page renderers. */
public record MythicMinerGeometry(
        Rect frame,
        Rect markerBay,
        Rect overview,
        Rect operations,
        Rect fluidPanel,
        Rect inventory,
        Rect tabs) {

    public static MythicMinerGeometry forMenu(int slotCount, boolean hasFluidInput, int menuWidth) {
        int markerHeight = MythicMinerLayout.markerBayHeight(slotCount);
        int playerY =
                MythicMinerLayout.playerInventoryY(MythicMinerLayout.rowsForSlotCount(slotCount));
        int inventoryHeight = 82;
        int frameHeight = playerY + inventoryHeight;
        int workWidth = hasFluidInput ? MythicMinerLayout.FLUID_PANEL_X - 36 : menuWidth - 56;
        int overviewX = MythicMinerLayout.markerInfoX(slotCount);
        int overviewWidth = workWidth - (overviewX - MythicMinerLayout.MARKER_BAY_X);
        int overviewY = MythicMinerLayout.MARKER_BAY_Y;
        int overviewHeight = MythicMinerLayout.markerBayHeight(slotCount);
        int operationsY = overviewY + overviewHeight + 5;
        return new MythicMinerGeometry(
                new Rect(0, 0, menuWidth, frameHeight),
                new Rect(
                        MythicMinerLayout.MARKER_BAY_X,
                        MythicMinerLayout.MARKER_BAY_Y,
                        MythicMinerLayout.markerBayWidth(slotCount),
                        markerHeight),
                new Rect(overviewX, overviewY, overviewWidth, overviewHeight),
                new Rect(MythicMinerLayout.MARKER_BAY_X, operationsY, workWidth, 28),
                hasFluidInput
                        ? new Rect(
                                MythicMinerLayout.FLUID_PANEL_X,
                                48,
                                MythicMinerLayout.FLUID_PANEL_WIDTH,
                                112)
                        : Rect.EMPTY,
                new Rect(0, playerY, menuWidth, inventoryHeight),
                new Rect(0, 31, menuWidth, 13));
    }

    public record Rect(int x, int y, int width, int height) {
        public static final Rect EMPTY = new Rect(0, 0, 0, 0);

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }

        public boolean intersects(Rect other) {
            return x < other.right()
                    && right() > other.x
                    && y < other.bottom()
                    && bottom() > other.y;
        }
    }
}
