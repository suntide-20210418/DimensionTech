package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared pixel geometry for the Structure Data Operator texture, menu and screen.
 *
 * <p><b>Every number here was measured off {@code guis/structure_data_operator.png}, not copied
 * from the layout proposal.</b> The texture is 357x245 and the scan results it encodes are:
 *
 * <ul>
 *   <li>Canvas well: white rule at {@code x=7}/{@code x=351}, shade at {@code y=43}, recessed fill
 *       from {@code (8,44)} to {@code (350,146)}. The scissor target is therefore {@code (8,43)}
 *       sized {@code 343x104} — one pixel taller than the fill so the shade line stays inside.
 *   <li>Machine slots: 18x18 cells (1px white rule + 16x16 face, the "sprite" grammar). The item
 *       lands one pixel inside the cell, at {@code (287,21)}, {@code (311,21)}, {@code (335,21)}.
 *   <li>Write array and player inventory: 16x16 faces with a 2px panel-coloured gutter, stride 18
 *       (the "texture" grammar this project's backpack already uses).
 * </ul>
 *
 * <p>The page tabs are <b>not</b> baked into the texture either. They are the 48x16 long-button
 * sprites at {@code (0,144)} and {@code (48,144)}, drawn by the screen on the empty panel face left
 * of the machine slots — the same arrangement the structure miner uses for its own page tabs.
 *
 * <p>Row heights and other page-internal metrics deliberately live with the pages that use them:
 * the menu needs slot geometry, and nothing else here is shared with the container.
 */
public final class StructureDataOperatorLayout {
    public static final ResourceLocation TEXTURE =
            ResourceLocationHelper.modLoc("guis/structure_data_operator.png");

    /**
     * Real file size. {@code blit} normalises UVs by whatever it is handed, so these must match.
     */
    public static final int TEXTURE_WIDTH = 357;

    public static final int TEXTURE_HEIGHT = 245;
    public static final int WIDTH = TEXTURE_WIDTH;
    public static final int HEIGHT = TEXTURE_HEIGHT;

    public static final int ITEM_SLOT_SIZE = 16;
    public static final int ITEM_SLOT_GAP = 2;
    public static final int ITEM_SLOT_STRIDE = ITEM_SLOT_SIZE + ITEM_SLOT_GAP;

    // --- page canvas -------------------------------------------------------
    /** Scissor target: the recessed page viewport. Pages draw in canvas-local coordinates. */
    public static final GuiRect CANVAS = new GuiRect(8, 43, 343, 104);

    // --- page tabs ---------------------------------------------------------
    public static final int TAB_BUTTON_X = 8;
    public static final int TAB_BUTTON_Y = 20;
    public static final int TAB_BUTTON_WIDTH = 48;
    public static final int TAB_BUTTON_HEIGHT = 16;

    /** Four pixels of face between one tab and the next; three tabs then end at x=160. */
    public static final int TAB_BUTTON_STRIDE = 52;

    /** Left edge of the {@code index}-th tab, left-packed from the panel edge. */
    public static int tabX(int index) {
        return TAB_BUTTON_X + index * TAB_BUTTON_STRIDE;
    }

    // --- machine slots (item icon origin, i.e. cell origin +1) --------------
    /** Reads from a marked structure marker. */
    public static final GuiRect TARGET_SLOT = new GuiRect(287, 21, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

    /** Data Integrator — unlocks the explored-structures page. */
    public static final GuiRect INTEGRATOR_SLOT =
            new GuiRect(311, 21, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

    /** Structure Interpreter — unlocks the all-structures page. */
    public static final GuiRect INTERPRETER_SLOT =
            new GuiRect(335, 21, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

    // --- write array: 36 markers, 9x4 --------------------------------------
    public static final int OPERAND_COLUMNS = 9;
    public static final int OPERAND_ROWS = 4;
    public static final int OPERAND_X = 8;
    public static final int OPERAND_Y = 164;

    // --- player inventory --------------------------------------------------
    public static final int PLAYER_INVENTORY_X = 183;
    public static final int PLAYER_INVENTORY_Y = 160;
    public static final int HOTBAR_Y = 218;
    public static final int PLAYER_INVENTORY_COLUMNS = 9;
    public static final int PLAYER_INVENTORY_ROWS = 3;

    // --- section labels ----------------------------------------------------
    /**
     * Blank panel rows kept between a section label and the first face of the block it names.
     *
     * <p>The strip between the canvas rule at {@code y=147} and the two wells is the only place a
     * label fits: it is 15 rows tall on the left and 11 on the right, and a label box needs 9, so
     * the gap cannot grow much beyond this without pushing the text under the canvas.
     */
    public static final int SECTION_LABEL_GAP = 3;

    /** Line height of the vanilla font — the height of the box a label occupies. */
    public static final int SECTION_LABEL_HEIGHT = 9;

    /** Nine columns of 16px faces on an 18px pitch, i.e. the width of either slot block. */
    public static final int BLOCK_WIDTH = ITEM_SLOT_SIZE + ITEM_SLOT_STRIDE * (OPERAND_COLUMNS - 1);

    /** Label box for the write array, {@link #SECTION_LABEL_GAP} rows above its topmost face. */
    public static final int WRITE_LABEL_Y = OPERAND_Y - SECTION_LABEL_GAP - SECTION_LABEL_HEIGHT;

    /**
     * A one-row downward correction applied to the inventory caption only.
     *
     * <p>Both captions start at the same nominal gap, but the inventory well opens four rows higher
     * than the write array's — y=159 against y=163 — so its caption reads a touch high against that
     * edge. The nudge was asked for by eye. It must stay below {@link #SECTION_LABEL_GAP},
     * otherwise the caption would sit on the well's rule with no blank row left between them.
     */
    public static final int INVENTORY_LABEL_NUDGE = 1;

    /** Label box for the player inventory: the write array's gap, nudged down by one row. */
    public static final int INVENTORY_LABEL_Y =
            PLAYER_INVENTORY_Y - SECTION_LABEL_GAP - SECTION_LABEL_HEIGHT + INVENTORY_LABEL_NUDGE;

    private StructureDataOperatorLayout() {}

    public static int operandX(int column) {
        return OPERAND_X + column * ITEM_SLOT_STRIDE;
    }

    public static int operandY(int row) {
        return OPERAND_Y + row * ITEM_SLOT_STRIDE;
    }

    public static int playerSlotX(int column) {
        return PLAYER_INVENTORY_X + column * ITEM_SLOT_STRIDE;
    }

    public static int playerSlotY(int row) {
        return PLAYER_INVENTORY_Y + row * ITEM_SLOT_STRIDE;
    }

    public static int hotbarSlotY() {
        return HOTBAR_Y;
    }

    /** Grid coordinates of write-array cell {@code index}, as {@code {column, row}}. */
    public static int operandColumn(int index) {
        return index % OPERAND_COLUMNS;
    }

    public static int operandRow(int index) {
        return index / OPERAND_COLUMNS;
    }
}
