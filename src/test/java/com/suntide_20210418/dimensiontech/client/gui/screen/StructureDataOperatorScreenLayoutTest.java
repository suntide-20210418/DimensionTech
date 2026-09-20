package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Locks the operator console to the authored texture and to the geometry its menu ships.
 *
 * <p>The console went through a restyle that removed two things the previous contract depended on:
 * the window no longer resizes per page, and the slot faces are now baked into the texture rather
 * than painted by the render path. The invariants below are the ones that still have to hold —
 * where the canvas sits, where the chrome around it may go, and the fact that the render path must
 * leave slot faces alone.
 */
class StructureDataOperatorScreenLayoutTest {
    private static final Path SCREEN =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen/StructureDataOperatorScreen.java");
    private static final Path OPERATION =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen/StructureDataOperatorOperationPage.java");
    private static final Path INTEGRATOR =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen/StructureDataIntegratorPage.java");
    private static final Path MENU =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/client/gui/menu/StructureDataOperatorMenu.java");

    @Test
    void theWindowMatchesTheAuthoredTexture() {
        assertEquals(StructureDataOperatorLayout.TEXTURE_WIDTH, StructureDataOperatorScreen.WIDTH);
        assertEquals(
                StructureDataOperatorLayout.TEXTURE_HEIGHT, StructureDataOperatorScreen.HEIGHT);
    }

    @Test
    void theCanvasColumnsFitInsideTheViewport() {
        int canvasWidth = StructureDataOperatorLayout.CANVAS.width();
        assertEquals(
                StructureDataOperatorScreen.LIST_X + StructureDataOperatorScreen.LIST_W + 4,
                StructureDataOperatorScreen.DETAIL_X,
                "the detail pane starts one gutter after the master list");
        assertTrue(
                StructureDataOperatorScreen.DETAIL_X + StructureDataOperatorScreen.DETAIL_W
                        <= canvasWidth,
                "the detail pane must not overrun the canvas");
    }

    @Test
    void theActionRowClearsTheTabsAndTheMachineSlots() {
        int lastTabRight =
                StructureDataOperatorLayout.tabX(2)
                        + StructureDataOperatorLayout.TAB_BUTTON_WIDTH;
        assertTrue(
                lastTabRight < StructureDataOperatorScreen.OPERATE_COPY_X,
                "a third tab must not run into the action row");
        assertTrue(
                StructureDataOperatorScreen.OPERATE_COPY_X + StructureDataOperatorScreen.ACTION_W
                        < StructureDataOperatorScreen.OPERATE_CLEAR_X,
                "the two operate actions must not overlap");
        assertTrue(
                StructureDataOperatorScreen.OPERATE_CLEAR_X + StructureDataOperatorScreen.ACTION_W
                        < StructureDataOperatorLayout.TARGET_SLOT.x(),
                "the action row must stop before the machine slots");
        assertTrue(
                StructureDataOperatorScreen.ACTION_FIRST_X
                                + StructureDataOperatorScreen.ACTION_W
                        < StructureDataOperatorScreen.ACTION_SECOND_X,
                "the two catalogue actions must not overlap");
        assertTrue(
                StructureDataOperatorScreen.ACTION_SECOND_X + StructureDataOperatorScreen.ACTION_W
                        < StructureDataOperatorLayout.TARGET_SLOT.x(),
                "the catalogue action row must stop before the machine slots");
    }

    @Test
    void theRenderPathLeavesSlotFacesToTheTexture() throws Exception {
        String screen = Files.readString(SCREEN);
        String integrator = Files.readString(INTEGRATOR);

        assertFalse(
                screen.contains("StructureMinerTheme.slot("),
                "every slot face is baked into the texture, so repainting one would double it up");
        assertFalse(
                integrator.contains("StructureMinerTheme.subPanel("),
                "the canvas is already a recess; nested panels would fight the artwork");
    }

    /**
     * The read marker, data integrator and structure interpreter are function slots: a hovered empty
     * slot still tells the player what goes there and what it unlocks, and a filled one shows the
     * item's own tooltip instead of burying it under the hint. So the screen owns a decision pass for
     * exactly these three slots and otherwise keeps the vanilla item tooltip.
     */
    @Test
    void theFunctionSlotsShowPurposeTooltips() throws Exception {
        String screen = Files.readString(SCREEN);

        assertTrue(
                screen.contains("renderSlotTooltips("),
                "all slot tooltips are drawn by a single manual hover pass");
        assertTrue(
                screen.contains("functionSlotTooltip("),
                "the three function slots choose hint-vs-item via one helper");
        assertTrue(
                screen.contains("drawSlotHint("),
                "an empty function slot gets a custom purpose-hint (head + func)");
        assertTrue(
                screen.contains("slot.target")
                        && screen.contains("slot.integrator")
                        && screen.contains("slot.interpreter"),
                "each function slot must name what it accepts and what it unlocks");
        assertTrue(
                screen.contains("hovered.getItem()"),
                "a filled function slot, and every other slot, keeps the vanilla item tooltip");
        assertTrue(
                screen.contains("StructureDataOperatorBlockEntity.TARGET")
                        && screen.contains("StructureDataOperatorBlockEntity.INTEGRATOR")
                        && screen.contains("StructureDataOperatorBlockEntity.INTERPRETER"),
                "the purpose hint must cover exactly the three function slots");
    }

    /**
     * Both console pages must show the readings through the shared block.
     *
     * <p>They used to have a private renderer each — four framed cells stacked on the operate page,
     * two side by side under a structure name on the catalogue pages — so identical data looked like
     * two different things and only one page listed the dimension and structure at all.
     */
    @Test
    void bothPagesDrawTheSameUnframedReadings() throws Exception {
        String operate = Files.readString(OPERATION);
        String catalogue = Files.readString(INTEGRATOR);

        assertTrue(
                operate.contains("StructureDataOperatorReadings.draw("),
                "the operate page must use the shared readings block");
        assertTrue(
                catalogue.contains("StructureDataOperatorReadings.draw("),
                "the catalogue pages must use the same block rather than their own cells");
        assertFalse(
                operate.contains("metricCard") || catalogue.contains("metricCard"),
                "a framed value cell is exactly what the readings block replaced");
    }

    /**
     * The canvas is clipped in GUI-logical coordinates, exactly as {@code enableScissor} expects.
     *
     * <p>It applies the window GUI scale by itself, so scaling the rectangle here multiplies it twice.
     * That is what happened while this screen borrowed the miner's projection: the canvas bottom edge
     * landed past the window, the scissor height clamped to zero, and every page rendered blank while
     * the chrome outside the scissor stayed visible.
     */
    @Test
    void theCanvasIsClippedInLogicalCoordinates() throws Exception {
        String screen = Files.readString(SCREEN);

        assertFalse(
                screen.contains("getGuiScale"),
                "enableScissor already applies the window GUI scale; applying it again clips to nothing");
        assertFalse(
                screen.contains("scaleToScreen"),
                "the canvas needs no projection - this screen never scales its pose");
    }

    /**
     * The chrome is sprites, and the actions fire straight away.
     *
     * <p>The tabs and the action pair come off the sheet — 48x16 and 32x16 — so they cannot drift
     * apart stylistically, and a code-drawn button beside them would read as a different control.
     * The confirmation modal is gone with them: the buttons were already gated on the same
     * conditions the modal repeated back.
     */
    @Test
    void theChromeDrawsSpritesAndSkipsTheConfirmationModal() throws Exception {
        String screen = Files.readString(SCREEN);

        assertTrue(
                screen.contains("StructureMinerSpriteRenderer.longButton("),
                "the page tabs are the 48x16 long buttons");
        assertTrue(
                screen.contains("StructureMinerSpriteRenderer.button("),
                "the action pair is the 32x16 buttons");
        assertFalse(
                screen.contains("StructureMinerTheme.button("),
                "a code-drawn button would not match the sprites it sits beside");
        assertFalse(
                screen.contains("drawModal"),
                "clicking an action applies it; the confirmation modal is gone");
    }

    @Test
    void theSlotGeometryComesFromTheSharedLayout() throws Exception {
        String menu = Files.readString(MENU);

        assertTrue(menu.contains("StructureDataOperatorLayout.TARGET_SLOT"));
        assertTrue(menu.contains("StructureDataOperatorLayout.INTEGRATOR_SLOT"));
        assertTrue(menu.contains("StructureDataOperatorLayout.INTERPRETER_SLOT"));
        assertTrue(menu.contains("StructureDataOperatorLayout.operandX("));
        assertTrue(
                menu.contains("StructureDataOperatorBlockEntity.OPERAND_COUNT"),
                "the write array must follow the block entity's slot count, not a literal");
    }

    @Test
    void theScrollingRegionsFitInsideTheCanvas() {
        int canvasHeight = StructureDataOperatorLayout.CANVAS.height();
        assertTrue(
                StructureDataOperatorScreen.LIST_Y
                                + StructureDataOperatorScreen.listRows()
                                        * StructureDataOperatorScreen.LIST_ROW_H
                        <= canvasHeight);
        assertTrue(
                StructureDataOperatorScreen.listRows() >= 4,
                "the catalogue list must show at least four rows");
        assertTrue(
                StructureDataOperatorScreen.tableRows() >= 2,
                "the expectation table must show at least two rows");
    }

    /**
     * Dark ink on the grey field: black on {@code GuiPalette.WELL} is about 5.3:1, which clears the
     * 4.5:1 WCAG AA floor for body text. The light {@code TEXT} would not — it belongs to the dark
     * title band.
     */
    @Test
    void theSearchFieldInkIsDarkEnoughForTheConsoleFace() throws Exception {
        String field = Files.readString(SCREEN.getParent().resolve("GuiSearchField.java"));

        assertTrue(field.contains("setTextColor(StructureMinerTheme.INK)"));
        assertFalse(field.contains("setTextColor(StructureMinerTheme.TEXT)"));
    }

    /**
     * Both slot blocks under the canvas carry a caption, three blank rows above the block.
     *
     * <p>The write array and the player's inventory are both 9x4 grids of the same 16px faces on the
     * same pitch, so without a caption the only way to tell them apart is the item sitting in them.
     *
     * <p>The strip a caption may use is narrow. The canvas' bottom rule is at y=147 and the wells open
     * at y=163 (write array) and y=159 (inventory), so the boxes have to land between them: y=152 and
     * y=149. A caption that drifted up would fall inside the canvas scissor and be clipped away, read
     * as a stray word inside whichever page was open.
     *
     * <p>The inventory's caption is one row lower than the plain three-row rule asks for; that nudge is
     * a named constant, and this test holds it to the part that matters — it may never consume the
     * whole gap and leave the caption sitting on the well's rule.
     */
    @Test
    void eachSlotBlockCarriesACaptionAboveIt() {
        assertEquals(
                16 + 18 * 8,
                StructureDataOperatorLayout.BLOCK_WIDTH,
                "nine 16px faces on an 18px pitch span 160px - the clip budget for a caption");
        assertTrue(
                StructureDataOperatorLayout.INVENTORY_LABEL_NUDGE
                        < StructureDataOperatorLayout.SECTION_LABEL_GAP,
                "a nudge that eats the whole gap would put the caption on the well's rule");

        int[][] sections = {
            {
                StructureDataOperatorLayout.WRITE_LABEL_Y,
                StructureDataOperatorLayout.OPERAND_Y,
                0
            },
            {
                StructureDataOperatorLayout.INVENTORY_LABEL_Y,
                StructureDataOperatorLayout.PLAYER_INVENTORY_Y,
                StructureDataOperatorLayout.INVENTORY_LABEL_NUDGE
            }
        };
        for (int[] section : sections) {
            int labelY = section[0];
            int blockTop = section[1];
            int nudge = section[2];

            assertEquals(
                    StructureDataOperatorLayout.SECTION_LABEL_GAP - nudge,
                    blockTop - labelY - StructureDataOperatorLayout.SECTION_LABEL_HEIGHT,
                    "a caption keeps the nominal gap to its block, less any named nudge");
            assertTrue(
                    labelY >= StructureDataOperatorLayout.CANVAS.bottom(),
                    "a caption drawn under the canvas would be clipped away by its scissor");
            assertTrue(
                    labelY + StructureDataOperatorLayout.SECTION_LABEL_HEIGHT < blockTop,
                    "a caption must end above the well's rule rather than on its faces");
        }
        assertTrue(
                StructureDataOperatorLayout.WRITE_LABEL_Y
                        > StructureDataOperatorLayout.INVENTORY_LABEL_Y,
                "the write array starts lower than the inventory, so its caption follows");
    }

    @Test
    void theScreenCaptionsBothSlotBlocks() throws Exception {
        String screen = Files.readString(SCREEN);

        assertTrue(screen.contains("drawSectionLabels(g)"), "both captions must be drawn");
        assertTrue(screen.contains("structure_operator.section.write_slots"));
        assertTrue(screen.contains("structure_operator.section.inventory"));
        assertTrue(
                screen.contains("StructureDataOperatorLayout.WRITE_LABEL_Y")
                        && screen.contains("StructureDataOperatorLayout.INVENTORY_LABEL_Y"),
                "the positions come from the shared layout, not from literals in the render path");
    }

    /**
     * The search field gets a row of its own, and nothing is painted underneath it.
     *
     * <p>It is rendered with the other renderables, after the page has been drawn into the canvas,
     * so whatever sits under it is covered, not blended. Sharing the title strip put its text over
     * the page title, and the title's width depends on the language — so no horizontal offset can
     * make that safe.
     */
    @Test
    void theSearchFieldHasARowOfItsOwn() {
        // The page title is gone; the field now takes that top band, and the list begins just
        // below its frame, so neither can ever paint over the other.
        assertTrue(
                StructureDataOperatorScreen.SEARCH_Y <= 4 + GuiSearchField.TEXT_HEIGHT,
                "the field takes the title band at the top of the canvas");
        assertTrue(
                StructureDataOperatorScreen.LIST_Y
                        >= StructureDataOperatorScreen.SEARCH_Y
                                + StructureDataOperatorScreen.SEARCH_H,
                "the list must begin below the field's frame, never under it");
        assertTrue(
                StructureDataOperatorScreen.LIST_Y
                        >= StructureDataOperatorScreen.SEARCH_Y
                                + StructureDataOperatorScreen.SEARCH_H,
                "the catalogue list must start below the field rather than under it");
        assertEquals(
                StructureDataOperatorScreen.LIST_X,
                StructureDataOperatorScreen.SEARCH_X,
                "the field filters the catalogue column, so it belongs to that column");
        assertEquals(
                StructureDataOperatorScreen.LIST_W, StructureDataOperatorScreen.SEARCH_W);
    }

    /**
     * A Chinese glyph is a full font line tall.
     *
     * <p>Vanilla centres a bordered edit box against {@code 8} — the ASCII cap height — so a
     * nine-pixel glyph in the usual twelve-pixel box rides the frame. The text area here is one
     * whole line, with the padding outside it.
     */
    @Test
    void theSearchFieldFitsAGlyph() {
        assertEquals(
                9, GuiSearchField.TEXT_HEIGHT, "the vanilla font lays text out nine pixels tall");
        assertTrue(
                StructureDataOperatorScreen.SEARCH_H > GuiSearchField.TEXT_HEIGHT,
                "the frame must leave the glyph a row above and below, not clip it");
    }

    @Test
    void theSearchFieldIsAVanillaEditBoxInThisModsColours() throws Exception {
        String screen = Files.readString(SCREEN);
        String field = Files.readString(SCREEN.getParent().resolve("GuiSearchField.java"));

        assertTrue(
                screen.contains("GuiSearchField.framed("),
                "the field stays the vanilla edit box, so typing and selection keep working");
        assertFalse(
                screen.contains("setBordered(true)"),
                "vanilla's frame is near-black and would not sit on these panels");
        assertTrue(
                field.contains("GuiChrome.recessWell("),
                "the frame comes from the canvas family, not a grey literal");
        assertFalse(
                field.contains("GuiPalette.WELL"),
                "the grey well family is a foreign panel on the green canvas");
    }

    /**
     * The structure tree is painted straight onto the machine canvas, so its row states come from the
     * canvas' own green family.
     *
     * <p>They used to come from the grey well palette — every row filled with {@code WELL} and lit
     * with {@code STRIPE_WELL} — which turned the list into a slab that read as a foreign panel
     * dropped onto the recess. The light {@code TEXT} ink came with it and only worked because that
     * fill was near-black; on a lit green row it is unreadable, so the ink is dark now.
     */
    @Test
    void theTreeRowStatesComeFromTheCanvasFamily() throws Exception {
        String chrome = Files.readString(SCREEN.getParent().resolve("GuiChrome.java"));
        int start = chrome.indexOf("static void listRow(");
        int end = chrome.indexOf("static void scrollbar(");
        assertTrue(start > 0 && end > start, "listRow must still be defined in GuiChrome");
        String body = chrome.substring(start, end);

        assertTrue(
                body.contains("GuiPalette.RECESS_PRESSED"),
                "a selected row presses into the canvas rather than turning blue-grey");
        assertTrue(
                body.contains("GuiPalette.RECESS_LIT"),
                "a hovered row lifts off the canvas rather than turning grey");
        assertFalse(
                body.contains("GuiPalette.WELL")
                        || body.contains("GuiPalette.STRIPE_WELL")
                        || body.contains("GuiPalette.SELECT"),
                "the grey well family is a foreign panel on the green canvas");
        assertTrue(body.contains("GuiPalette.INK"), "dark ink clears AA on every state");
        assertFalse(
                body.contains("GuiPalette.TEXT"),
                "light ink was only readable on the old near-black fill");
        assertTrue(
                body.contains("int height"),
                "the row pitch is the caller's, not a literal that overruns the next row");
    }

    /** The tree must hand its row pitch down, or the fill would outrun the row it belongs to. */
    @Test
    void theTreePassesItsRowPitchToTheRowRenderer() throws Exception {
        String page = Files.readString(INTEGRATOR);
        int start = page.indexOf("StructureMinerTheme.listRow(");
        assertTrue(start > 0, "the catalogue tree draws its rows through the shared renderer");
        assertTrue(
                page.substring(start, page.indexOf(");", start)).contains("ROW_H"),
                "the row renderer needs the pitch to size its fill");
    }

    /**
     * The search box matches what the tree shows, not the ids behind it.
     *
     * <p>Filtering on the resource key meant a query in the player's own language could never hit: the
     * key reads the same in every language while the row beside the cursor is translated. The key is
     * kept as a last resort for structures with no translation, but the translated names have to lead.
     *
     * <p>This can only be checked as source, not behaviour: resolving a translation needs the Minecraft
     * language, which the plain unit tests cannot initialise.
     */
    @Test
    void theSearchMatchesTranslatedNamesNotJustResourceKeys() throws Exception {
        String screen = Files.readString(SCREEN);
        int start = screen.indexOf("private boolean matches(");
        int end = screen.indexOf("List<CatalogueRow> catalogueRows()");
        assertTrue(start > 0 && end > start, "the catalogue filter must live on the screen");
        String body = screen.substring(start, end);

        assertTrue(
                body.contains("TranslateHelper.structureName("),
                "the search must match the name the row displays");
        assertTrue(
                body.contains("TranslateHelper.dimensionName("),
                "the dimension header above a row is part of what the player reads for it");
        assertTrue(
                body.contains("searchLabels.computeIfAbsent("),
                "the tree is rebuilt every frame, so names cannot be resolved per row per frame");
        assertTrue(
                screen.contains("searchLabels.clear()"),
                "a new catalogue invalidates the names cached for the old one");
    }
}
