package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicCrucibleMenu;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Industrial ritual console for the Mythic Crucible. */
public final class MythicCrucibleScreen extends AbstractContainerScreen<MythicCrucibleMenu> {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 282;
    private static final int TICKS = MythicCrucibleCycle.STATE_TIMEOUT_TICKS;

    public MythicCrucibleScreen(MythicCrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = 188;
        titleLabelY = 7;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(0, 0, width, height, MythicMinerTheme.BACKDROP);
        int x = leftPos;
        int y = topPos;
        MythicMinerTheme.panel(g, x, y, WIDTH, HEIGHT, MythicMinerTheme.FLUIX);
        MythicMinerTheme.panel(g, x + 8, y + 28, WIDTH - 16, 143, MythicMinerTheme.PANEL);
        MythicMinerTheme.panel(g, x + 8, y + 176, WIDTH - 16, 98, MythicMinerTheme.FRAME);

        drawFluidColumn(g, x + 22, y + 46, menu.inputAmount(), MythicMinerTheme.FLUIX);
        drawFluidColumn(g, x + 284, y + 46, menu.outputAmount(), 0xFFB06FDD);
        drawRitualTrack(g, x + 59, y + 68);

        MythicMinerTheme.slot(g, x + 104, y + 156, false, false);
        boolean operationMissing =
                menu.status() == MythicCrucibleCycle.Status.RUNNING.ordinal()
                        && menu.currentState() >= 0
                        && !menu.getSlot(1).hasItem();
        MythicMinerTheme.slot(g, x + 184, y + 156, false, operationMissing);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++)
                MythicMinerTheme.slot(g, x + 8 + col * 18, y + 200 + row * 18, false, false);
        }
        for (int col = 0; col < 9; col++)
            MythicMinerTheme.slot(g, x + 8 + col * 18, y + 258, false, false);
        g.fill(x + 9, y + 183, x + WIDTH - 9, y + 184, MythicMinerTheme.EDGE);
    }

    private void drawFluidColumn(GuiGraphics g, int x, int y, int amount, int color) {
        g.fill(x, y, x + 14, y + 78, MythicMinerTheme.INSET);
        int filled = progressPixels(amount, 16000, 76);
        if (filled > 0) g.fill(x + 1, y + 77 - filled, x + 13, y + 77, color);
        g.fill(x, y + 78, x + 14, y + 80, MythicMinerTheme.EDGE);
    }

    static int progressPixels(long value, long max, int pixels) {
        if (max <= 0 || pixels <= 0) return 0;
        return (int) Math.max(0, Math.min(pixels, value * pixels / max));
    }

    private void drawRitualTrack(GuiGraphics g, int x, int y) {
        g.fill(x, y + 5, x + 194, y + 7, MythicMinerTheme.EDGE);
        int current = menu.currentState();
        int index = menu.stateIndex();
        for (int i = 0; i < 4; i++) {
            int cx = x + i * 64;
            int color =
                    i < index
                            ? MythicMinerTheme.FLUIX
                            : (i == current ? MythicMinerTheme.AMBER : MythicMinerTheme.MUTED);
            g.fill(cx - 4, y, cx + 5, y + 10, color);
            g.fill(cx - 2, y + 2, cx + 3, y + 8, MythicMinerTheme.INSET);
            g.drawCenteredString(font, stageName(i), cx, y + 13, color);
        }
        long ticks = menu.stateTicks();
        MythicMinerTheme.progress(
                g,
                x,
                y + 33,
                194,
                ticks,
                TICKS,
                menu.status() == MythicCrucibleCycle.Status.READY_TO_COMMIT.ordinal()
                        ? MythicMinerTheme.SUCCESS
                        : MythicMinerTheme.FLUIX);
    }

    private Component stageName(int ordinal) {
        String key =
                switch (ordinal) {
                    case 0 -> "screen.dimension_tech.mythic_crucible.stage.branch";
                    case 1 -> "screen.dimension_tech.mythic_crucible.stage.recurse";
                    case 2 -> "screen.dimension_tech.mythic_crucible.stage.converge";
                    default -> "screen.dimension_tech.mythic_crucible.stage.stabilize";
                };
        return Component.translatable(key);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 7, MythicMinerTheme.TEXT, false);
        Component status = Component.translatable(statusKey());
        g.drawString(font, status, imageWidth - 10 - font.width(status), 7, statusColor(), false);
        g.drawString(
                font,
                Component.translatable("screen.dimension_tech.mythic_crucible.input"),
                18,
                35,
                MythicMinerTheme.MUTED,
                false);
        g.drawString(
                font,
                Component.translatable("screen.dimension_tech.mythic_crucible.output"),
                278,
                35,
                MythicMinerTheme.MUTED,
                false);
        Component amount = Component.literal(menu.inputAmount() + "/16000 mB");
        g.drawString(font, amount, 15, 128, MythicMinerTheme.TEXT, false);
        Component out = Component.literal(menu.outputAmount() + "/16000 mB");
        g.drawString(font, out, 267, 128, MythicMinerTheme.TEXT, false);
        g.drawCenteredString(
                font,
                Component.translatable("screen.dimension_tech.mythic_crucible.ritual"),
                160,
                47,
                MythicMinerTheme.TEXT);
        Component phase =
                menu.currentState() >= 0
                        ? Component.translatable(
                                "screen.dimension_tech.mythic_crucible.current",
                                stageName(menu.currentState()),
                                menu.stateTicks(),
                                TICKS)
                        : Component.translatable("screen.dimension_tech.mythic_crucible.waiting");
        g.drawCenteredString(font, phase, 160, 106, MythicMinerTheme.TEXT);
        g.drawString(
                font,
                Component.translatable("screen.dimension_tech.mythic_crucible.fragments"),
                100,
                145,
                MythicMinerTheme.MUTED,
                false);
        g.drawString(
                font,
                Component.translatable("screen.dimension_tech.mythic_crucible.operation"),
                180,
                145,
                MythicMinerTheme.MUTED,
                false);
        Component reward =
                Component.translatable(
                        "screen.dimension_tech.mythic_crucible.reward",
                        MythicCrucibleCycle.REWARD_START_TICK,
                        MythicCrucibleCycle.REWARD_END_TICK);
        g.drawString(font, reward, 215, 145, MythicMinerTheme.AMBER, false);
        g.drawString(
                font,
                Component.translatable("screen.dimension_tech.mythic_crucible.inventory"),
                10,
                178,
                MythicMinerTheme.TEXT,
                false);
    }

    private String statusKey() {
        return switch (menu.status()) {
            case 1 -> "screen.dimension_tech.mythic_crucible.status.running";
            case 2 -> "screen.dimension_tech.mythic_crucible.status.ready";
            default -> "screen.dimension_tech.mythic_crucible.status.idle";
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case 1 -> MythicMinerTheme.FLUIX;
            case 2 -> MythicMinerTheme.SUCCESS;
            default -> MythicMinerTheme.MUTED;
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        if (localY >= 46
                && localY < 126
                && (localX >= 22 && localX < 36 || localX >= 284 && localX < 298)) {
            boolean input = localX < 100;
            var tank = input ? menu.crucible().inputTank() : menu.crucible().outputTank();
            Component name =
                    tank.getFluid().isEmpty()
                            ? Component.translatable(
                                    input
                                            ? "screen.dimension_tech.mythic_crucible.input"
                                            : "screen.dimension_tech.mythic_crucible.output")
                            : tank.getFluid().getDisplayName();
            g.renderTooltip(
                    font,
                    List.of(
                            name,
                            Component.literal(
                                    tank.getFluidAmount() + "/" + tank.getCapacity() + " mB")),
                    Optional.empty(),
                    mouseX,
                    mouseY);
        }
    }
}
