package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;

/** Shared pixel geometry for the Structure Reactor texture, menu and screen. */
public final class StructureReactorLayout {
    public static final int ITEM_SLOT_SIZE = 16;
    public static final int ITEM_SLOT_GAP = 2;
    public static final int ITEM_SLOT_STRIDE = ITEM_SLOT_SIZE + ITEM_SLOT_GAP;
    public static final int WIDTH = 292;
    public static final int HEIGHT = 165;
    public static final int TEXTURE_WIDTH = 320;
    public static final int TEXTURE_HEIGHT = 256;
    public static final ResourceLocation TEXTURE =
            ResourceLocationHelper.modLoc("guis/structure_reactor.png");

    public static final GuiRect INPUT_TANK = new GuiRect(8, 12, 16, 50);
    public static final GuiRect FRAGMENT_SLOT = new GuiRect(32, 16, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    public static final GuiRect OPERATION_SLOT =
            new GuiRect(32, 42, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    public static final GuiRect PROGRESS_BAR = new GuiRect(56, 18, 88, 5);
    /** JEI opens the reactor recipe category when this area is clicked; reused as the progress bar. */
    public static final GuiRect RECIPE_DISPLAY = PROGRESS_BAR;
    /** Compact status + reward window readout on the machine face. */
    public static final GuiRect STATUS_REWARD_AREA = new GuiRect(56, 27, 88, 29);
    public static final GuiRect OUTPUT_TANK = new GuiRect(152, 12, 16, 50);
    /** Scrollable detail panel: sequence, needs, recipe changes and last settlement. */
    public static final GuiRect VIEWPORT = new GuiRect(182, 42, 100, 112);
    public static final GuiRect PLAYER_INVENTORY = new GuiRect(7, 83, 162, 54);
    public static final GuiRect HOTBAR = new GuiRect(7, 141, 162, 18);

    public static final int PROGRESS_OVERLAY_U = 0;
    public static final int PROGRESS_OVERLAY_V = 166;
    public static final int PROGRESS_OVERLAY_WIDTH = 88;
    public static final int PROGRESS_OVERLAY_HEIGHT = 5;

    public static final int FLUID_SCALE_U = 315;
    public static final int FLUID_SCALE_V = 0;
    public static final int FLUID_SCALE_WIDTH = 5;
    public static final int FLUID_SCALE_HEIGHT = 41;

    private StructureReactorLayout() {}

    public static int playerSlotX(int column) {
        return PLAYER_INVENTORY.x() + 1 + column * ITEM_SLOT_STRIDE;
    }

    public static int playerSlotY(int row) {
        return PLAYER_INVENTORY.y() + 1 + row * ITEM_SLOT_STRIDE;
    }

    public static int hotbarSlotX(int column) {
        return HOTBAR.x() + 1 + column * ITEM_SLOT_STRIDE;
    }

    public static GuiRect playerSlot(int column, int row) {
        return new GuiRect(playerSlotX(column), playerSlotY(row), ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }

    public static GuiRect hotbarSlot(int column) {
        return new GuiRect(hotbarSlotX(column), hotbarSlotY(), ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }

    public static int hotbarSlotY() {
        return HOTBAR.y() + 1;
    }

    public static int innerX(GuiRect rect) {
        return rect.x() + 1;
    }

    public static int innerY(GuiRect rect) {
        return rect.y() + 1;
    }

    public static int innerWidth(GuiRect rect) {
        return Math.max(0, rect.width() - 2);
    }

    public static int innerHeight(GuiRect rect) {
        return Math.max(0, rect.height() - 2);
    }
}
