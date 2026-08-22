package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/** Six-face output selector with a compact 1-3-2 instrument layout. */
public final class OutputFaceScreen extends Screen {
    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 142;
    private static final int BUTTON_SIZE = 24;
    private static final int BUTTON_GAP = 5;
    private static final int PANEL = 0xFF11181D;
    private static final int RAISED = 0xFF1B282F;
    private static final int RULE = 0xFF2B3941;
    private static final int MUTED = 0xFF8EA2A9;
    private static final int CYAN = 0xFF4DD6D0;
    private static final int SCREEN_MARGIN = 8;

    private final MythicMinerScreen parent;
    private final MythicMinerMenu menu;
    private int left;
    private int top;
    private float uiScale = 1.0F;

    public OutputFaceScreen(MythicMinerScreen parent, MythicMinerMenu menu) {
        super(Component.translatable("screen.dimension_tech.mythic_miner.output_face"));
        this.parent = parent;
        this.menu = menu;
    }

    @Override
    protected void init() {
        float widthScale = (float) Math.max(1, width - SCREEN_MARGIN * 2) / PANEL_WIDTH;
        float heightScale = (float) Math.max(1, height - SCREEN_MARGIN * 2) / PANEL_HEIGHT;
        uiScale = Math.min(1.0F, Math.min(widthScale, heightScale));
        int logicalWidth = (int) Math.floor(width / uiScale);
        int logicalHeight = (int) Math.floor(height / uiScale);
        left = (logicalWidth - PANEL_WIDTH) / 2;
        top = (logicalHeight - PANEL_HEIGHT) / 2;
    }

    private int buttonX(int row, int column) {
        int count = row == 0 ? 1 : row == 1 ? 3 : 2;
        int rowWidth = count * BUTTON_SIZE + (count - 1) * BUTTON_GAP;
        if (row == 2) {
            // Keep the back/down pair under the left/front columns.
            int middleRowWidth = 3 * BUTTON_SIZE + 2 * BUTTON_GAP;
            return left + (PANEL_WIDTH - middleRowWidth) / 2 + column * (BUTTON_SIZE + BUTTON_GAP);
        }
        return left + (PANEL_WIDTH - rowWidth) / 2 + column * (BUTTON_SIZE + BUTTON_GAP);
    }

    private int buttonY(int row) {
        return top + 39 + row * (BUTTON_SIZE + BUTTON_GAP);
    }

    private Direction directionAt(int row, int column) {
        if (row == 0) return Direction.UP;
        // The console is viewed from the miner's front: left/front/right/back
        // map to south/east/north/west in world coordinates.
        if (row == 1)
            return new Direction[] {Direction.EAST, Direction.NORTH, Direction.WEST}[column];
        return new Direction[] {Direction.SOUTH, Direction.DOWN}[column];
    }

    private boolean isEnabled(Direction direction) {
        Direction worldDirection = menu.getBlockEntity().toWorldDirection(direction);
        return (menu.getTelemetry(13) & (1 << worldDirection.ordinal())) != 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF0B0E12);
        int scaledMouseX = toLogical(mouseX);
        int scaledMouseY = toLogical(mouseY);
        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1.0F);
        graphics.fill(
                left + 3, top + 3, left + PANEL_WIDTH + 3, top + PANEL_HEIGHT + 3, 0xFF080A0C);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, PANEL);
        graphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + 3, CYAN);
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 14, 0xFFE7EEF0);
        drawBackButton(graphics, scaledMouseX, scaledMouseY);

        Direction hoveredDirection = null;
        for (int row = 0; row < 3; row++) {
            int count = row == 0 ? 1 : row == 1 ? 3 : 2;
            for (int column = 0; column < count; column++) {
                Direction direction = directionAt(row, column);
                int x = buttonX(row, column);
                int y = buttonY(row);
                boolean hovered =
                        contains(scaledMouseX, scaledMouseY, x, y, BUTTON_SIZE, BUTTON_SIZE);
                boolean enabled = isEnabled(direction);
                graphics.fill(
                        x - 1,
                        y - 1,
                        x + BUTTON_SIZE + 1,
                        y + BUTTON_SIZE + 1,
                        enabled ? CYAN : RULE);
                graphics.fill(
                        x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, hovered ? 0xFF41545B : RAISED);
                BlockState state = adjacentState(direction);
                ItemStack icon =
                        state == null || state.isAir() || state.getBlock().asItem() == Items.AIR
                                ? new ItemStack(Items.BARRIER)
                                : new ItemStack(state.getBlock().asItem());
                graphics.renderItem(icon, x + 4, y + 4);
                if (hovered) hoveredDirection = direction;
            }
        }
        super.render(graphics, scaledMouseX, scaledMouseY, partialTick);
        if (hoveredDirection != null)
            renderFaceTooltip(graphics, hoveredDirection, scaledMouseX, scaledMouseY);
        if (contains(scaledMouseX, scaledMouseY, left + PANEL_WIDTH - 25, top + 7, 18, 18)) {
            graphics.renderTooltip(
                    font, Component.translatable("gui.back"), scaledMouseX, scaledMouseY);
        }
        graphics.pose().popPose();
    }

    private int toLogical(double coordinate) {
        return (int) Math.floor(coordinate / uiScale);
    }

    private void drawBackButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + PANEL_WIDTH - 25;
        int y = top + 7;
        boolean hovered = contains(mouseX, mouseY, x, y, 18, 18);
        graphics.fill(x - 1, y - 1, x + 19, y + 19, hovered ? CYAN : RULE);
        graphics.fill(x, y, x + 18, y + 18, hovered ? 0xFF41545B : RAISED);
        graphics.fill(x + 5, y + 8, x + 14, y + 10, CYAN);
        graphics.fill(x + 5, y + 6, x + 7, y + 12, CYAN);
        graphics.fill(x + 3, y + 8, x + 5, y + 10, CYAN);
    }

    private void renderFaceTooltip(
            GuiGraphics graphics, Direction direction, int mouseX, int mouseY) {
        BlockState state = adjacentState(direction);
        Component adjacent =
                state == null || state.isAir()
                        ? Component.translatable("screen.dimension_tech.mythic_miner.face.empty")
                        : state.getBlock().getName();
        List<Component> tooltip =
                List.of(
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.face."
                                        + direction.getSerializedName()),
                        adjacent);
        graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    private BlockState adjacentState(Direction direction) {
        Direction worldDirection = menu.getBlockEntity().toWorldDirection(direction);
        return Minecraft.getInstance().level == null
                ? null
                : Minecraft.getInstance()
                        .level
                        .getBlockState(
                                menu.getBlockEntity().getBlockPos().relative(worldDirection));
    }

    private boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX /= uiScale;
        mouseY /= uiScale;
        if (button == 0) {
            if (contains(mouseX, mouseY, left + PANEL_WIDTH - 25, top + 7, 18, 18)) {
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
            for (int row = 0; row < 3; row++) {
                int count = row == 0 ? 1 : row == 1 ? 3 : 2;
                for (int column = 0; column < count; column++) {
                    int x = buttonX(row, column);
                    int y = buttonY(row);
                    if (contains(mouseX, mouseY, x, y, BUTTON_SIZE, BUTTON_SIZE)) {
                        Direction direction = directionAt(row, column);
                        Minecraft.getInstance()
                                .gameMode
                                .handleInventoryButtonClick(
                                        menu.containerId, 10 + direction.ordinal());
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
