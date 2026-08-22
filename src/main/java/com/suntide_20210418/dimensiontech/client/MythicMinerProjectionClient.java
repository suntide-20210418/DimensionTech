package com.suntide_20210418.dimensiontech.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.MythicMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.MythicMinerMultiblock.ProjectionBlock;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DimensionTechMod.MOD_ID, value = Dist.CLIENT)
public final class MythicMinerProjectionClient {
    private static final double MAX_DISTANCE_SQUARED = 96.0D * 96.0D;
    private static Projection projection;

    private MythicMinerProjectionClient() {}

    public static void toggle(BlockPos center, int tier) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        ResourceKey<Level> dimension = minecraft.level.dimension();
        if (projection != null
                && projection.center().equals(center)
                && projection.dimension().equals(dimension)) {
            projection = null;
            minecraft.player.displayClientMessage(
                    Component.translatable("message.dimension_tech.mythic_miner.projection_off"),
                    true);
            return;
        }
        projection =
                new Projection(
                        center.immutable(), dimension, MythicMinerMultiblock.projection(tier));
        minecraft.player.displayClientMessage(
                Component.translatable("message.dimension_tech.mythic_miner.projection_on"), true);
    }

    @SubscribeEvent
    public static void renderProjection(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || projection == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || minecraft.player == null
                || !minecraft.level.dimension().equals(projection.dimension())
                || minecraft.player.distanceToSqr(Vec3.atCenterOf(projection.center()))
                        > MAX_DISTANCE_SQUARED) {
            projection = null;
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.38F);
        for (ProjectionBlock block : projection.blocks()) {
            BlockPos worldPos = projection.center().offset(block.offset());
            poseStack.pushPose();
            poseStack.translate(worldPos.getX(), worldPos.getY(), worldPos.getZ());
            minecraft
                    .getBlockRenderer()
                    .renderSingleBlock(
                            block.state(),
                            poseStack,
                            buffers,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        buffers.endBatch();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.disableDepthTest();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (ProjectionBlock block : projection.blocks()) {
            BlockPos worldPos = projection.center().offset(block.offset());
            float[] color = color(block);
            LevelRenderer.renderLineBox(
                    poseStack,
                    lines,
                    new AABB(worldPos).inflate(0.002D),
                    color[0],
                    color[1],
                    color[2],
                    0.85F);
        }
        buffers.endBatch(RenderType.lines());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static float[] color(ProjectionBlock block) {
        return switch (block.kind()) {
            case CASING -> new float[] {0.30F, 0.84F, 0.82F};
            case FOCUS -> new float[] {0.72F, 0.42F, 1.00F};
            case STRUCTURE -> new float[] {1.00F, 0.71F, 0.36F};
            case UPGRADE -> new float[] {0.42F, 0.92F, 0.58F};
        };
    }

    private record Projection(
            BlockPos center, ResourceKey<Level> dimension, List<ProjectionBlock> blocks) {}
}
