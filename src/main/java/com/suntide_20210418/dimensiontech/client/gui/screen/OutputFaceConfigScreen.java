package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Standalone, reusable output-face configuration screen. */
public final class OutputFaceConfigScreen extends Screen {
    private static final int CANVAS_WIDTH = 96;
    private static final int CANVAS_HEIGHT = 112;
    private static final int BUTTON_SIZE = 20;
    // Center (64, 63), 20px buttons with 5px gaps: " 上 "/"左正右"/"后下 ".
    // The horizontal order is mirrored to match the machine's front-facing view.
    private static final Direction[] DIRECTIONS = {
        Direction.UP,
        Direction.EAST,
        Direction.NORTH,
        Direction.WEST,
        Direction.SOUTH,
        Direction.DOWN
    };
    private static final int BUTTON_GAP = 1;
    private static final int CONTROL_SIZE = 16;
    private static final int CONTROL_LEFT_INSET = 8;
    private static final int CONTROL_BOTTOM_INSET = 8;
    private static final int CONTROL_GAP = -2;
    private final Screen parent;
    private final MythicMinerMenu menu;
    private int left;
    private int top;

    public OutputFaceConfigScreen(Screen parent, MythicMinerMenu menu) {
        super(Component.translatable("screen.dimension_tech.mythic_miner.output_face"));
        this.parent = parent;
        this.menu = menu;
    }

    @Override
    protected void init() {
        left = (width - CANVAS_WIDTH) / 2;
        top = (height - CANVAS_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, MythicMinerTheme.BACKDROP);
        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.blit(
                ResourceLocation.fromNamespaceAndPath(
                        "dimension_tech", "guis/output_face_config.png"),
                0,
                0,
                0,
                0,
                CANVAS_WIDTH,
                CANVAS_HEIGHT,
                CANVAS_WIDTH,
                CANVAS_HEIGHT);
        for (int i = 0; i < DIRECTIONS.length; i++) {
            int[] position = buttonPosition(i);
            int x = position[0], y = position[1];
            Direction d = DIRECTIONS[i];
            MythicMinerSpriteRenderer.smallButton(g, x, y, menu.isOutputFaceEnabled(d));
            ItemStack stack = adjacentBlock(d);
            if (!stack.isEmpty()) g.renderItem(stack, x + 2, y + 2);
        }
        Component aeLabel =
                Component.translatable("screen.dimension_tech.mythic_miner.output.ae_mode");
        Component fluidLabel =
                Component.translatable("screen.dimension_tech.mythic_miner.output.auto_pull_fluid");
        // AE mode: normal frame (48,16)-(63,31), pressed frame (48,32)-(63,47).
        int controlX = controlX();
        MythicMinerSpriteRenderer.externalIcon(
                g,
                controlX,
                controlY(0),
                48,
                menu.getOutputState() == BaseMinerBlockEntity.OutputState.ME_NETWORK ? 32 : 16);
        MythicMinerSpriteRenderer.externalIcon(
                g, controlX, controlY(1), 48, menu.isAutoExtractFluidEnabled() ? 32 : 16);
        g.drawString(
                font,
                aeLabel,
                controlX() + CONTROL_SIZE,
                controlY(0) + 4,
                MythicMinerScreen.INK,
                false);
        g.drawString(
                font,
                fluidLabel,
                controlX() + CONTROL_SIZE,
                controlY(1) + 4,
                MythicMinerScreen.INK,
                false);
        g.pose().popPose();
        renderTooltip(g, mouseX - left, mouseY - top, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double x = mouseX - left, y = mouseY - top;
            if (inside(x, y, controlX(), controlY(0), CONTROL_SIZE, CONTROL_SIZE)) {
                menu.toggleAeOutputMode();
                return true;
            }
            if (inside(x, y, controlX(), controlY(1), CONTROL_SIZE, CONTROL_SIZE)) {
                click(26);
                return true;
            }
            for (int i = 0; i < DIRECTIONS.length; i++) {
                int[] p = buttonPosition(i);
                if (inside(x, y, p[0], p[1], BUTTON_SIZE, BUTTON_SIZE)) {
                    click(10 + DIRECTIONS[i].ordinal());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private boolean inside(double x, double y, int px, int py, int w, int h) {
        return x >= px && x < px + w && y >= py && y < py + h;
    }

    private ItemStack adjacentBlock(Direction d) {
        Direction world = menu.getBlockEntity().toWorldDirection(d);
        if (minecraft.level == null) return ItemStack.EMPTY;
        var block =
                minecraft
                        .level
                        .getBlockState(menu.getBlockEntity().getBlockPos().relative(world))
                        .getBlock();
        return block.defaultBlockState().isAir()
                        || block.asItem() == net.minecraft.world.item.Items.AIR
                ? ItemStack.EMPTY
                : new ItemStack(block.asItem());
    }

    private void renderTooltip(GuiGraphics g, int x, int y, int screenX, int screenY) {
        for (int i = 0; i < DIRECTIONS.length; i++) {
            int[] p = buttonPosition(i);
            if (!inside(x, y, p[0], p[1], BUTTON_SIZE, BUTTON_SIZE)) continue;
            Direction d = DIRECTIONS[i];
            boolean fluid = menu.getFluidFaceMode(d) == BaseMinerBlockEntity.FluidFaceMode.INPUT;
            boolean output = menu.isOutputFaceEnabled(d);
            String status =
                    fluid && output
                            ? "both"
                            : fluid ? "fluid_input" : output ? "auto_output" : "disabled";
            ItemStack stack = adjacentBlock(d);
            g.renderTooltip(
                    font,
                    List.of(
                            Component.translatable(
                                    "screen.dimension_tech.mythic_miner.face." + d.getName()),
                            stack.isEmpty()
                                    ? Component.translatable(
                                            "screen.dimension_tech.mythic_miner.face.empty")
                                    : stack.getHoverName(),
                            Component.translatable(
                                    "screen.dimension_tech.mythic_miner.face.status." + status)),
                    Optional.empty(),
                    screenX,
                    screenY);
            return;
        }
    }

    private int centerX() {
        return CANVAS_WIDTH / 2;
    }

    private int centerY() {
        return CANVAS_HEIGHT / 2 - 16;
    }

    private int[] buttonPosition(int index) {
        int x = centerX() - BUTTON_SIZE / 2;
        int y = centerY() - BUTTON_SIZE / 2;
        return switch (index) {
            case 0 -> new int[] {x, y - BUTTON_SIZE - BUTTON_GAP};
            case 1 -> new int[] {x - BUTTON_SIZE - BUTTON_GAP, y};
            case 2 -> new int[] {x, y};
            case 3 -> new int[] {x + BUTTON_SIZE + BUTTON_GAP, y};
            case 4 -> new int[] {x - BUTTON_SIZE - BUTTON_GAP, y + BUTTON_SIZE + BUTTON_GAP};
            case 5 -> new int[] {x, y + BUTTON_SIZE + BUTTON_GAP};
            default -> throw new IllegalArgumentException("face index: " + index);
        };
    }

    private int controlX() {
        return CONTROL_LEFT_INSET;
    }

    private int controlY(int index) {
        int groupHeight = CONTROL_SIZE * 2 + CONTROL_GAP;
        return CANVAS_HEIGHT
                - CONTROL_BOTTOM_INSET
                - groupHeight
                + index * (CONTROL_SIZE + CONTROL_GAP);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
