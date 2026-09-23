package com.suntide_20210418.dimensiontech.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.StructureMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.StructureMinerMultiblock.ProjectionBlock;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DimensionTechMod.MOD_ID, value = Dist.CLIENT)
public final class StructureMinerProjectionClient {
    /** Past this the ghost is neither readable nor worth the draw calls, so the overlay drops. */
    private static final double MAX_DISTANCE_SQUARED = 16.0D * 16.0D;
    private static Projection projection;

    private StructureMinerProjectionClient() {}

    public static void toggle(BlockPos center) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        ResourceKey<Level> dimension = minecraft.level.dimension();
        if (projection != null
                && projection.center().equals(center)
                && projection.dimension().equals(dimension)) {
            projection = null;
            minecraft.player.displayClientMessage(
                    Component.translatable("message.dimension_tech.structure_miner.projection_off"),
                    true);
            return;
        }
        // A finished build has no missing slot to point at, so the overlay would only lie about
        // being on: refuse it here instead of letting the render pass clear it a frame later.
        if (StructureMinerMultiblock.isComplete(minecraft.level, center)) {
            minecraft.player.displayClientMessage(
                    Component.translatable(
                            "message.dimension_tech.structure_miner.structure_complete"),
                    true);
            return;
        }
        projection =
                new Projection(
                        center.immutable(), dimension, StructureMinerMultiblock.projection());
        minecraft.player.displayClientMessage(
                Component.translatable("message.dimension_tech.structure_miner.projection_on"), true);
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
                        > MAX_DISTANCE_SQUARED
                // Finishing the build leaves nothing to project, so the overlay retires itself.
                || StructureMinerMultiblock.isComplete(minecraft.level, projection.center())) {
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
        for (ProjectionBlock block : projection.blocks()) {
            BlockPos worldPos = projection.center().offset(block.offset());
            BlockState actual = minecraft.level.getBlockState(worldPos);
            // Already carries whatever this slot accepts: nothing left to place here, so it drops out.
            if (StructureMinerMultiblock.isFilled(block, actual)) continue;
            float[] tint = color(block);
            if (!actual.isAir() && !actual.canBeReplaced()) {
                // Occupied by an unrelated, non-replaceable block: flag it red instead of hiding it.
                tint = RED;
            }
            // Flush per block so the ColorModulator tint set here actually applies.
            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], 0.38F);
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
            buffers.endBatch();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static final float[] RED = {1.00F, 0.16F, 0.16F};

    private static float[] color(ProjectionBlock block) {
        return switch (block.kind()) {
            case CASING -> new float[] {0.30F, 0.84F, 0.82F};
            case GLASS -> new float[] {0.55F, 0.78F, 1.00F};
            case STRUCTURE -> new float[] {1.00F, 0.71F, 0.36F};
            case UPGRADE -> new float[] {0.42F, 0.92F, 0.58F};
        };
    }

    private record Projection(
            BlockPos center, ResourceKey<Level> dimension, List<ProjectionBlock> blocks) {}
}
