package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MythicMinerScreen extends AbstractContainerScreen<MythicMinerMenu> {
    private static final int ALLOY_FACE = 0xFFD1D3D5;
    private static final int ALLOY_LIGHT = 0xFFF2F3F4;
    private static final int ALLOY_MID = 0xFF9A9DA0;
    private static final int GRAPHITE = 0xFF25292D;
    private static final int GRAPHITE_INSET = 0xFF343A40;
    private static final int SLOT_FACE = 0xFF8D9194;
    private static final int CYAN = 0xFF39C3CF;
    private static final int CYAN_DARK = 0xFF21747B;
    private static final int AMBER = 0xFFD8943B;
    private static final int LABEL_COLOR = 0xFF2B2E31;

    public MythicMinerScreen(
            MythicMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = menu.getPlayerInventoryY() + 82;
        inventoryLabelY = menu.getPlayerInventoryY() - 12;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        drawRaisedPanel(guiGraphics, leftPos, topPos, imageWidth, imageHeight);

        int workAreaHeight = menu.getPlayerInventoryY() - 24;
        drawInsetPanel(guiGraphics, leftPos + 7, topPos + 7, imageWidth - 14, workAreaHeight);
        drawDimensionChannels(guiGraphics);

        int separatorY = topPos + menu.getPlayerInventoryY() - 18;
        guiGraphics.fill(
                leftPos + 7, separatorY, leftPos + imageWidth - 7, separatorY + 1, ALLOY_MID);
        guiGraphics.fill(
                leftPos + 7,
                separatorY + 1,
                leftPos + imageWidth - 7,
                separatorY + 2,
                ALLOY_LIGHT);

        for (int index = 0; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            drawSlotFrame(
                    guiGraphics,
                    leftPos + slot.x,
                    topPos + slot.y,
                    index < menu.getContainerSlotCount());
        }

        drawTitlePlate(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                LABEL_COLOR,
                false);
    }

    private void drawRaisedPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x99000000);
        guiGraphics.fill(x, y, x + width, y + height, GRAPHITE);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, ALLOY_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, ALLOY_FACE);
        guiGraphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, ALLOY_MID);
        guiGraphics.fill(x + width - 3, y + 2, x + width - 2, y + height - 2, ALLOY_MID);
    }

    private void drawInsetPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, ALLOY_MID);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, GRAPHITE);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, GRAPHITE_INSET);
        guiGraphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0x33FFFFFF);
        guiGraphics.fill(x + width - 3, y + 2, x + width - 2, y + height - 2, 0x33FFFFFF);
    }

    private void drawSlotFrame(
            GuiGraphics guiGraphics, int x, int y, boolean machineSlot) {
        int border = machineSlot ? CYAN_DARK : GRAPHITE;
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, border);
        guiGraphics.fill(x, y, x + 16, y + 16, SLOT_FACE);
        guiGraphics.fill(x, y, x + 16, y + 1, ALLOY_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + 16, ALLOY_LIGHT);
        guiGraphics.fill(x, y + 15, x + 16, y + 16, GRAPHITE);
        guiGraphics.fill(x + 15, y, x + 16, y + 16, GRAPHITE);

        if (machineSlot) {
            guiGraphics.fill(x - 3, y - 3, x + 2, y - 2, CYAN);
            guiGraphics.fill(x - 3, y - 3, x - 2, y + 2, CYAN);
            guiGraphics.fill(x + 14, y + 18, x + 19, y + 19, CYAN);
            guiGraphics.fill(x + 18, y + 14, x + 19, y + 19, CYAN);
        }
    }

    private void drawDimensionChannels(GuiGraphics guiGraphics) {
        int slotCount = menu.getContainerSlotCount();
        for (int row = 0; row < menu.getContainerRows(); row++) {
            int firstIndex = row * MythicMinerMenu.SLOT_COLUMNS;
            if (firstIndex >= slotCount) {
                break;
            }
            int lastIndex = Math.min(firstIndex + MythicMinerMenu.SLOT_COLUMNS, slotCount) - 1;
            Slot first = menu.slots.get(firstIndex);
            Slot last = menu.slots.get(lastIndex);
            int centerY = topPos + first.y + 8;
            int startX = leftPos + first.x - 10;
            int endX = leftPos + last.x + 26;
            guiGraphics.fill(startX, centerY, endX, centerY + 1, CYAN_DARK);
            guiGraphics.fill(startX, centerY - 2, startX + 2, centerY + 3, AMBER);
            guiGraphics.fill(endX - 2, centerY - 2, endX, centerY + 3, AMBER);
        }
    }

    private void drawTitlePlate(GuiGraphics guiGraphics) {
        int iconX = leftPos + 12;
        int iconY = topPos - 24;
        drawRaisedPanel(guiGraphics, iconX, iconY, 28, 28);

        ItemStack icon = new ItemStack(menu.getBlockEntity().getBlockState().getBlock());
        guiGraphics.renderItem(icon, iconX + 6, iconY + 6);

        int maxTextWidth = imageWidth - 70;
        String titleText = title.getString();
        String visibleTitle = font.plainSubstrByWidth(titleText, maxTextWidth);
        if (!visibleTitle.equals(titleText)) {
            visibleTitle =
                    font.plainSubstrByWidth(titleText, maxTextWidth - font.width("...")) + "...";
        }
        int titleWidth = font.width(visibleTitle) + 14;
        int titleX = iconX + 33;
        int titleY = topPos - 17;
        drawRaisedPanel(guiGraphics, titleX, titleY, titleWidth, 16);
        guiGraphics.drawString(font, visibleTitle, titleX + 7, titleY + 4, LABEL_COLOR, false);
    }
}
