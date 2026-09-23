package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Routes page content and page-scoped interaction to the page that owns it.
 *
 * <p>Dispatch lives here so the screen stays a shell: it owns the window, the control rail and the tab
 * strip, while each page owns everything inside the viewport — including where its own clickable rows
 * are. Asking the page rather than recomputing its layout here is what keeps a click landing on the
 * row that was actually drawn, even when rows change height as they expand.
 */
final class StructureMinerPageRenderer {
    private StructureMinerPageRenderer() {}

    static void render(StructureMinerScreenContext context, GuiGraphics g, StructureMinerScreen.Page page) {
        switch (page) {
            case WORK -> StructureMinerWorkPage.render(context, g);
            case INFO -> StructureMinerInfoPage.render(context, g);
            case ATTRIBUTES -> StructureMinerAttributesPage.render(context, g);
        }
    }

    /**
     * Routes the hover pass.
     *
     * <p>The screen passes panel-local coordinates for the page's own hit tests and the raw
     * GUI-scaled pair for {@code renderTooltip}, which draws in window space and would otherwise be
     * placed at the wrong corner at any {@code uiScale} below 1.
     */
    static void renderTooltip(
            StructureMinerScreenContext c,
            StructureMinerScreen.Page page,
            GuiGraphics g,
            double x,
            double y,
            int screenX,
            int screenY) {
        switch (page) {
            case WORK -> StructureMinerWorkPage.renderTooltip(c, g, x, y, screenX, screenY);
            case INFO -> StructureMinerInfoPage.renderTooltip(c, g, x, y, screenX, screenY);
            case ATTRIBUTES -> StructureMinerAttributesPage.renderTooltip(c, g, x, y, screenX, screenY);
        }
    }

    /**
     * @return {@code true} when the page consumed the click. The work page returns {@code false} for
     *     anything that is not part of its lane. The scrolling pages own every primary click inside
     *     their canvas, and {@code false} everywhere else, so the player inventory below the canvas
     *     stays vanilla's to handle.
     */
    static boolean mouseClicked(
            StructureMinerScreenContext c,
            StructureMinerScreen.Page page,
            double x,
            double y,
            int button) {
        return switch (page) {
            case WORK -> workClicked(c, x, y, button);
            case INFO -> button == 0 && insideCanvas(x, y) && infoClicked(c, x, y);
            case ATTRIBUTES -> button == 0 && insideCanvas(x, y) && attributesClicked(c, x, y);
        };
    }

    /** True when a panel-local point is inside the page canvas, clear of the player inventory. */
    private static boolean insideCanvas(double x, double y) {
        return StructureMinerInfoLayout.inside(
                x,
                y,
                StructureMinerInfoLayout.INFO_X,
                StructureMinerInfoLayout.INFO_Y,
                StructureMinerInfoLayout.INFO_W,
                StructureMinerInfoLayout.INFO_H);
    }

    /**
     * Scrolls the current page.
     *
     * <p>Position is gated by the screen before this is reached: the wheel applies over the page
     * canvas but not over the player inventory. Pages draw at {@code viewportY - scroll}, so a
     * larger offset looks further down the content. GLFW reports a positive y-offset for a wheel
     * pushed away, which must show content above, so the wheel sign is inverted: wheel down
     * (delta &lt; 0) increases the offset. This matches vanilla's creative inventory, which subtracts
     * too.
     */
    static boolean mouseScrolled(
            StructureMinerScreenContext c, StructureMinerScreen.Page page, double delta) {
        // Pages draw at `viewportY - scroll`, so a larger offset looks further down the content.
        // GLFW reports a positive y-offset for a wheel pushed away, which must show content above,
        // so the wheel sign is inverted: wheel down (delta < 0) increases the offset. This matches
        // vanilla's creative inventory, which subtracts too.
        int step = -(int) Math.signum(delta) * StructureMinerInfoLayout.ROW_H_INTERACTIVE;
        return switch (page) {
            case INFO -> {
                int max =
                        Math.max(0, c.markerInfoContentHeight() - StructureMinerInfoLayout.INFO_LIST_H);
                c.markerInfoScroll(Math.max(0, Math.min(max, c.markerInfoScroll() + step)));
                yield true;
            }
            case ATTRIBUTES -> {
                int max =
                        Math.max(0, c.attributeContentHeight() - StructureMinerInfoLayout.ATTR_LIST_H);
                c.attributeScroll(Math.max(0, Math.min(max, c.attributeScroll() + step)));
                yield true;
            }
            // The work page has nothing to scroll: its lane is a single row at every tier.
            case WORK -> false;
        };
    }

    /**
     * Left-click keeps the item gesture, right-click owns thread toggling.
     *
     * <p>The two are deliberately kept apart by button rather than by target: the strip under each
     * cell used to be the toggle, which shared its precise 4px band with nothing else and could not
     * be hovered without also covering the cell above it.
     */
    private static boolean workClicked(
            StructureMinerScreenContext c, double x, double y, int button) {
        if (button == 1) return toggleMarkerSlot(c, x, y);
        if (button != 0) return false;
        int slot = StructureMinerInfoLayout.markerSlotAt(x, y, c.menu().getContainerSlotCount());
        if (slot < 0) return false;
        // Selecting and handling the item in one gesture: the player does not need two conventions
        // for "look at this thread" and "put a marker here".
        c.selectMarkerSlot(slot);
        c.clickContainerSlot(slot, 0);
        return true;
    }

    /**
     * Flips a thread's enabled flag, but only where the lane can draw the answer back.
     *
     * <p>An empty slot refuses rather than forwarding: the server would flip a flag nothing renders,
     * so the player would see a click that did nothing at all. Everything else falls through to
     * vanilla so right-click stays a working inventory gesture — including on a slot holding some
     * other item, which the menu still lets through.
     */
    private static boolean toggleMarkerSlot(StructureMinerScreenContext c, double x, double y) {
        int count = c.menu().getContainerSlotCount();
        int slot = StructureMinerInfoLayout.markerSlotAt(x, y, count);
        if (slot < 0) return false;
        if (StructMarkerItem.getMarkerInfo(c.menu().slots.get(slot).getItem()).isEmpty()) {
            return false;
        }
        ModNetwork.toggleStructureMinerSlot(c.menu().containerId, slot);
        return true;
    }

    private static boolean infoClicked(StructureMinerScreenContext c, double x, double y) {
        int slot = StructureMinerInfoLayout.markerSlotAt(x, y, c.menu().getContainerSlotCount());
        if (slot >= 0) {
            c.selectMarkerSlot(slot);
            return true;
        }

        int row = StructureMinerInfoPage.productRowAt(c, x, y);
        if (row >= 0) {
            StructureMinerScreen.ExpectedItemRow entry = c.expectedItemRows().get(row);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.item());
            if (itemId != null) {
                ModNetwork.toggleStructureMinerExpectedItem(
                        c.menu().containerId, c.selectedMarkerSlot(), itemId);
            }
        }
        // The page covers the whole viewport and has no click-through to the inventory.
        return true;
    }

    private static boolean attributesClicked(StructureMinerScreenContext c, double x, double y) {
        int key = StructureMinerAttributesPage.rowKeyAt(c, x, y);
        if (key != Integer.MIN_VALUE) {
            c.toggleUpgradeRow(key);
        }
        return true;
    }

}
