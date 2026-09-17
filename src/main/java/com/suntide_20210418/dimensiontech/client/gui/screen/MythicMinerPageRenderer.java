package com.suntide_20210418.dimensiontech.client.gui.screen;

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
final class MythicMinerPageRenderer {
    private MythicMinerPageRenderer() {}

    static void render(MythicMinerScreenContext context, GuiGraphics g, MythicMinerScreen.Page page) {
        switch (page) {
            case WORK -> MythicMinerWorkPage.render(context, g);
            case INFO -> MythicMinerInfoPage.render(context, g);
            case ATTRIBUTES -> MythicMinerAttributesPage.render(context, g);
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
            MythicMinerScreenContext c,
            MythicMinerScreen.Page page,
            GuiGraphics g,
            double x,
            double y,
            int screenX,
            int screenY) {
        switch (page) {
            case WORK -> MythicMinerWorkPage.renderTooltip(c, g, x, y, screenX, screenY);
            case INFO -> MythicMinerInfoPage.renderTooltip(c, g, x, y, screenX, screenY);
            case ATTRIBUTES -> MythicMinerAttributesPage.renderTooltip(c, g, x, y, screenX, screenY);
        }
    }

    /**
     * @return {@code true} when the page consumed the click. The work page returns {@code false} for
     *     anything that is not part of its lane, because the player inventory is still vanilla's to
     *     handle.
     */
    static boolean mouseClicked(
            MythicMinerScreenContext c,
            MythicMinerScreen.Page page,
            double x,
            double y,
            int button) {
        if (button != 0) return false;
        return switch (page) {
            case WORK -> workClicked(c, x, y);
            case INFO -> infoClicked(c, x, y);
            case ATTRIBUTES -> attributesClicked(c, x, y);
        };
    }

    /**
     * Scrolls the current page.
     *
     * <p>There is deliberately no position gate. The page is the only scrollable surface on the
     * panel, so gating on hover position can only ever create a way for the wheel to do nothing —
     * which is indistinguishable from a page that does not scroll, and is exactly the report that
     * prompted removing it. Where the pointer rests is not information a player expects to matter.
     */
    static boolean mouseScrolled(
            MythicMinerScreenContext c, MythicMinerScreen.Page page, double delta) {
        // Pages draw at `viewportY - scroll`, so a larger offset looks further down the content.
        // GLFW reports a positive y-offset for a wheel pushed away, which must show content above,
        // so the wheel sign is inverted: wheel down (delta < 0) increases the offset. This matches
        // both vanilla's creative inventory and StructureDataOperatorScreen, which subtract too.
        int step = -(int) Math.signum(delta) * MythicMinerInfoLayout.ROW_H_INTERACTIVE;
        return switch (page) {
            case INFO -> {
                int max =
                        Math.max(0, c.markerInfoContentHeight() - MythicMinerInfoLayout.INFO_LIST_H);
                c.markerInfoScroll(Math.max(0, Math.min(max, c.markerInfoScroll() + step)));
                yield true;
            }
            case ATTRIBUTES -> {
                int max =
                        Math.max(0, c.attributeContentHeight() - MythicMinerInfoLayout.ATTR_LIST_H);
                c.attributeScroll(Math.max(0, Math.min(max, c.attributeScroll() + step)));
                yield true;
            }
            // The work page has nothing to scroll: its lane is a single row at every tier.
            case WORK -> false;
        };
    }

    private static boolean workClicked(MythicMinerScreenContext c, double x, double y) {
        int count = c.menu().getContainerSlotCount();
        int slot = MythicMinerInfoLayout.markerSlotAt(x, y, count);
        if (slot >= 0) {
            // Selecting and handling the item in one gesture: the player does not need two conventions
            // for "look at this thread" and "put a marker here".
            c.selectMarkerSlot(slot);
            c.clickContainerSlot(slot, 0);
            return true;
        }

        int barY = MythicMinerInfoLayout.MARKER_PROGRESS_Y;
        if (y >= barY && y < barY + MythicMinerInfoLayout.MARKER_PROGRESS_H) {
            for (int index = 0; index < count; index++) {
                int barX =
                        MythicMinerInfoLayout.laneX(index, count)
                                + MythicMinerInfoLayout.MARKER_PROGRESS_INSET;
                if (x >= barX && x < barX + MythicMinerInfoLayout.MARKER_PROGRESS_W) {
                    ModNetwork.toggleMythicMinerSlot(c.menu().containerId, index);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean infoClicked(MythicMinerScreenContext c, double x, double y) {
        int slot = MythicMinerInfoLayout.markerSlotAt(x, y, c.menu().getContainerSlotCount());
        if (slot >= 0) {
            c.selectMarkerSlot(slot);
            return true;
        }

        int row = MythicMinerInfoPage.productRowAt(c, x, y);
        if (row >= 0) {
            MythicMinerScreen.ExpectedItemRow entry = c.expectedItemRows().get(row);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.item());
            if (itemId != null) {
                ModNetwork.toggleMythicMinerExpectedItem(
                        c.menu().containerId, c.selectedMarkerSlot(), itemId);
            }
        }
        // The page covers the whole viewport and has no click-through to the inventory.
        return true;
    }

    private static boolean attributesClicked(MythicMinerScreenContext c, double x, double y) {
        int key = MythicMinerAttributesPage.rowKeyAt(c, x, y);
        if (key != Integer.MIN_VALUE) {
            c.toggleUpgradeRow(key);
        }
        return true;
    }

}
