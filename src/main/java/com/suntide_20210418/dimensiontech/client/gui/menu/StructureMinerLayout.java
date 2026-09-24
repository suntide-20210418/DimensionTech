package com.suntide_20210418.dimensiontech.client.gui.menu;

/**
 * Machine-wide geometry shared by the miner menu and its screen.
 *
 * <p>The marker lane no longer lives here. Slots are laid out in a single centred row, kept
 * off-screen by {@link StructureMinerMenu}, and drawn by the page renderers — so their geometry
 * belongs with the pages that own it, in {@code StructureMinerInfoLayout}. What remains is what the
 * menu and the screen genuinely share: the inventory anchor, and the scissor projection.
 */
public final class StructureMinerLayout {
    /** Anchor of the player inventory block, below the fixed marker lane and dashboard. */
    public static final int BASE_PLAYER_INVENTORY_Y = 258;

    private StructureMinerLayout() {}

    /**
     * Y of the player inventory. It does not depend on the marker count: the lane is a single row
     * of at most nine cells at every tier, so the section above the inventory never grows.
     */
    public static int playerInventoryY() {
        return BASE_PLAYER_INVENTORY_Y;
    }

    /**
     * Projects a panel-local rectangle into the absolute space scissor rectangles live in.
     *
     * <p>{@code GuiGraphics.enableScissor} applies the window GUI scale only and ignores the render
     * pose, so a scissor built from panel-local coordinates clips in the wrong place whenever the
     * pose has been translated or scaled.
     */
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
