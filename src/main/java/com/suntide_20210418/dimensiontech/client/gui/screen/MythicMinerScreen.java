package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Compact mining console with centered marker lanes and live machine telemetry. */
public final class MythicMinerScreen extends AbstractContainerScreen<MythicMinerMenu> {
    private static final int WIDTH = 320;
    private static final int PANEL = 0xFF11181D;
    private static final int PANEL_INSET = 0xFF0B1115;
    private static final int PANEL_RAISED = 0xFF1B282F;
    private static final int RULE = 0xFF2B3941;
    private static final int TEXT = 0xFFE7EEF0;
    private static final int MUTED = 0xFF8EA2A9;
    private static final int CYAN = 0xFF4DD6D0;
    private static final int CYAN_DARK = 0xFF1D7778;
    private static final int AMBER = 0xFFF0B45C;
    private static final int RED = 0xFFE45B51;
    private static final int SLOT_FACE = 0xFF33434A;
    private static final int ENERGY_X = 5;
    private static final int ENERGY_TOP = 36;
    private static final int ENERGY_HEIGHT = 164;

    public MythicMinerScreen(MythicMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = WIDTH;
        imageHeight = menu.getPlayerInventoryY() + 82;
        inventoryLabelY = menu.getPlayerInventoryY() - 13;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + ENERGY_X
                && mouseX <= leftPos + ENERGY_X + 14
                && mouseY >= topPos + ENERGY_TOP
                && mouseY <= topPos + ENERGY_TOP + ENERGY_HEIGHT) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("screen.dimension_tech.mythic_miner.energy_tooltip"));
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.energy_value",
                            menu.getTelemetry(2),
                            menu.getTelemetry(3)));
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.energy_consumption",
                            menu.getTelemetry(10)));
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0.0D);
        drawPanel(graphics, 0, 0, imageWidth, imageHeight);
        drawHeader(graphics);
        drawEnergyRail(graphics);
        drawMarkerBay(graphics);
        drawTelemetry(graphics);
        drawPlayerDivider(graphics);
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            drawSlotFrame(
                    graphics,
                    slot.x,
                    slot.y,
                    index < menu.getContainerSlotCount());
        }
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, playerInventoryTitle, imageWidth / 2, inventoryLabelY, MUTED);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, 0x99000000);
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 3, CYAN);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, RULE);
    }

    private void drawHeader(GuiGraphics graphics) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 14, TEXT);
        graphics.fill(24, 31, imageWidth - 24, 32, RULE);
    }

    private void drawMarkerBay(GuiGraphics graphics) {
        int x = 28;
        int y = 38;
        int width = imageWidth - 56;
        int height = menu.getContainerRows() * 34 + 34;
        graphics.fill(x, y, x + width, y + height, PANEL_RAISED);
        graphics.fill(x, y, x + width, y + 2, CYAN_DARK);
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.dimension_tech.mythic_miner.markers"),
                imageWidth / 2,
                y + 8,
                MUTED);

        int progress = menu.getTelemetry(0);
        int processing = Math.max(1, menu.getTelemetry(1));
        int progressWidth = 18;
        for (int index = 0; index < menu.getContainerSlotCount(); index++) {
            Slot slot = menu.slots.get(index);
            int slotX = slot.x;
            int slotY = slot.y;
            int barX = slotX - 1;
            int barY = slotY + 19;
            graphics.fill(barX, barY, barX + progressWidth, barY + 3, PANEL_INSET);
            int filled = Math.max(0, Math.min(progressWidth, progressWidth * progress / processing));
            graphics.fill(barX, barY, barX + filled, barY + 3, CYAN);
            graphics.fill(barX + filled, barY, barX + Math.min(progressWidth, filled + 1), barY + 3, AMBER);
        }
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.dimension_tech.mythic_miner.slots", menu.getContainerSlotCount()),
                imageWidth / 2,
                y + height - 13,
                MUTED);
    }

    private void drawEnergyRail(GuiGraphics graphics) {
        int x = ENERGY_X;
        int y = ENERGY_TOP;
        int energy = menu.getTelemetry(2);
        int capacity = Math.max(1, menu.getTelemetry(3));
        int filled = Math.max(0, Math.min(ENERGY_HEIGHT, ENERGY_HEIGHT * energy / capacity));
        graphics.fill(x - 1, y - 1, x + 15, y + ENERGY_HEIGHT + 1, RULE);
        graphics.fill(x + 2, y + 2, x + 12, y + ENERGY_HEIGHT - 2, PANEL_INSET);
        int innerHeight = ENERGY_HEIGHT - 4;
        for (int offset = 0; offset < innerHeight; offset++) {
            int red = 18 + (offset * 190 / innerHeight);
            int green = 18 + (offset * 35 / innerHeight);
            int color = (0xFF << 24) | (red << 16) | (green << 8) | 12;
            graphics.fill(x + 3, y + 2 + innerHeight - offset - 1, x + 11, y + 2 + innerHeight - offset, color);
        }
        int filledHeight = Math.min(innerHeight, filled * innerHeight / ENERGY_HEIGHT);
        graphics.fill(x + 3, y + 2 + innerHeight - filledHeight, x + 11, y + 2 + innerHeight, RED);
        graphics.drawString(font, Component.literal("FE"), x - 1, y + ENERGY_HEIGHT + 7, MUTED, false);
    }

    private void drawTelemetry(GuiGraphics graphics) {
        int x = 40;
        int width = imageWidth - 55;
        int y = 132;
        int progress = menu.getTelemetry(0);
        int processing = Math.max(1, menu.getTelemetry(1));
        int percent = Math.min(100, progress * 100 / processing);
        graphics.drawString(font, Component.translatable("screen.dimension_tech.mythic_miner.progress"), x, y, MUTED, false);
        graphics.drawString(font, Component.literal(percent + "%  |  " + progress + " / " + processing + " tick"), x + width - 145, y, TEXT, false);
        graphics.fill(x, y + 15, x + width, y + 21, 0xFF25343A);
        int progressX = x + Math.max(1, width * percent / 100);
        graphics.fill(x, y + 15, progressX, y + 21, CYAN);
        graphics.fill(progressX - 2, y + 13, progressX, y + 23, AMBER);

        int baseParallel = menu.getTelemetry(7);
        int accumulatedHundredths = menu.getTelemetry(8);
        String currentParallel =
                String.format(
                        java.util.Locale.ROOT,
                        "%d(+0.%02d)",
                        baseParallel,
                        accumulatedHundredths);
        graphics.drawString(
                font,
                Component.translatable("screen.dimension_tech.mythic_miner.current_parallel"),
                x,
                y + 39,
                MUTED,
                false);
        graphics.drawString(font, Component.literal(currentParallel), x + 112, y + 39, AMBER, false);

        graphics.drawString(font, Component.translatable("screen.dimension_tech.mythic_miner.extra_items"), x, y + 61, MUTED, false);
        graphics.drawString(font, Component.literal("+" + menu.getTelemetry(9)), x + 88, y + 61, AMBER, false);
        BaseMinerBlockEntity.OutputState output = BaseMinerBlockEntity.OutputState.values()[Math.max(0, Math.min(2, menu.getTelemetry(5)))];
        Component outputName = Component.translatable("screen.dimension_tech.mythic_miner.output." + output.name().toLowerCase(java.util.Locale.ROOT));
        graphics.drawString(font, Component.translatable("screen.dimension_tech.mythic_miner.output"), x + 134, y + 61, MUTED, false);
        graphics.drawString(font, outputName, x + 196, y + 61, output == BaseMinerBlockEntity.OutputState.NONE ? RED : CYAN, false);
    }

    private void drawPlayerDivider(GuiGraphics graphics) {
        int y = menu.getPlayerInventoryY() - 22;
        graphics.fill(24, y, imageWidth - 24, y + 1, RULE);
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y, boolean markerSlot) {
        int border = markerSlot ? CYAN_DARK : RULE;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, border);
        graphics.fill(x, y, x + 16, y + 16, SLOT_FACE);
        graphics.fill(x, y, x + 16, y + 1, 0xFF6C858B);
        graphics.fill(x, y + 15, x + 16, y + 16, PANEL_INSET);
        if (markerSlot) graphics.fill(x - 3, y - 3, x + 2, y - 2, CYAN);
    }
}
