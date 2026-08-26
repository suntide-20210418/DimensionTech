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
    public enum Mode {
        ITEM_OUTPUT,
        FLUID
    }
    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 142;
    private static final int BUTTON_SIZE = 24;
    private static final int BUTTON_GAP = 5;
    private static final int PANEL = MythicMinerTheme.FRAME;
    private static final int RAISED = MythicMinerTheme.PANEL;
    private static final int RULE = MythicMinerTheme.EDGE;
    private static final int MUTED = MythicMinerTheme.MUTED;
    private static final int CYAN = MythicMinerTheme.FLUIX;
    private static final int SCREEN_MARGIN = 8;

    private final MythicMinerScreen parent;
    private final MythicMinerMenu menu;
    private final Mode mode;
    private int left;
    private int top;
    private float uiScale = 1.0F;

    public OutputFaceScreen(MythicMinerScreen parent, MythicMinerMenu menu) {
        this(parent, menu, Mode.ITEM_OUTPUT);
    }

    public OutputFaceScreen(MythicMinerScreen parent, MythicMinerMenu menu, Mode mode) {
        super(Component.translatable(
                mode == Mode.FLUID
                        ? "screen.dimension_tech.mythic_miner.fluid_faces"
                        : "screen.dimension_tech.mythic_miner.output_face"));
        this.parent = parent;
        this.menu = menu;
        this.mode = mode;
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
        Direction worldDirection = worldDirection(direction);
        if (mode == Mode.FLUID) {
            return menu.getFluidFaceMode(worldDirection)
                    != com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity
                            .FluidFaceMode.DISABLED;
        }
        return (menu.telemetrySnapshot().outputFaceMask() & (1 << worldDirection.ordinal())) != 0;
    }

    private Direction worldDirection(Direction logicalDirection) {
        return menu.getBlockEntity().toWorldDirection(logicalDirection);
    }

    static Direction commandDirection(Mode mode, Direction logicalDirection, Direction worldDirection) {
        return mode == Mode.FLUID ? logicalDirection : worldDirection;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, MythicMinerTheme.BACKDROP);
        int scaledMouseX = toLogical(mouseX);
        int scaledMouseY = toLogical(mouseY);
        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1.0F);
        MythicMinerTheme.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, MythicMinerTheme.AMBER);
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 14, MythicMinerTheme.TEXT);
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
                int modeColor = faceColor(direction);
                MythicMinerTheme.button(graphics, font, x, y, BUTTON_SIZE, BUTTON_SIZE,
                        Component.empty(), hovered, enabled, modeColor);
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
        MythicMinerTheme.button(graphics, font, x, y, 18, 18, Component.empty(), hovered, true, CYAN);
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
        List<Component> tooltip = new java.util.ArrayList<>(
                List.of(
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.face."
                                        + direction.getSerializedName()),
                        adjacent));
        if (mode == Mode.FLUID) {
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.fluid_face_mode."
                                    + menu.getFluidFaceMode(worldDirection(direction))
                                            .name()
                                            .toLowerCase(java.util.Locale.ROOT)));
        }
        graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    private int faceColor(Direction direction) {
        if (mode != Mode.FLUID) return CYAN;
        return switch (menu.getFluidFaceMode(worldDirection(direction))) {
            case INPUT -> CYAN;
            case OUTPUT -> MythicMinerTheme.AMBER;
            case DISABLED -> RULE;
        };
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
                        Direction worldDirection = worldDirection(direction);
                        Minecraft.getInstance()
                                .gameMode
                                .handleInventoryButtonClick(
                                        menu.containerId,
                                        (mode == Mode.FLUID ? 20 : 10)
                                                + commandDirection(mode, direction, worldDirection)
                                                        .ordinal());
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
