package com.suntide_20210418.dimensiontech.client.model;

import com.google.gson.GsonBuilder;
import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.AssemblyPanelBlock;
import com.suntide_20210418.dimensiontech.block.BaseMinerBlock;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.StructureMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.StructureMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.block.StructureMinerVisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Opt-in integration checks after a real Forge client resource/model bake. Run with
 * -PredwedgeModelValidation; ordinary clients only check the boolean flag.
 */
@Mod.EventBusSubscriber(
        modid = DimensionTechMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ConnectedModelClientChecks {
    private ConnectedModelClientChecks() {}

    @SubscribeEvent
    public static void afterBake(ModelEvent.BakingCompleted event) {
        if (!Boolean.getBoolean("dimension_tech.validate_models")) return;
        int states = 0, formedFaces = 0, topologyCases = 0;
        var random = RandomSource.create(0);
        var registry = event.getModels();
        for (var holder : ModBlocks.BLOCKS.getEntries()) {
            var block = holder.get();
            if (!(block instanceof AssemblyPanelBlock) && !(block instanceof BaseMinerBlock))
                continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BakedModel model = registry.get(BlockModelShaper.stateToModelLocation(state));
                require(
                        block instanceof BaseMinerBlock
                                ? model != null && !(model instanceof ConnectedPanelModel)
                                : model instanceof ConnectedPanelModel,
                        "Missing wrapper for " + state);
                states++;
            }
        }
        // Actual formed slots, including every upgrade type/tier in a waist position.
        int moduleSlot = 0;
        for (int slot = 1; slot <= 66; slot++) {
            BlockPos offset = StructureMinerVisuals.offset(slot);
            var projected =
                    StructureMinerMultiblock.projection().stream()
                            .filter(p -> p.offset().equals(offset))
                            .findFirst()
                            .orElseThrow();
            var block = projected.state().getBlock();
            BlockState state =
                    block.defaultBlockState().setValue(AssemblyPanelBlock.ASSEMBLY_SLOT, slot);
            BakedModel model = registry.get(BlockModelShaper.stateToModelLocation(state));
            for (Direction face : Direction.values()) {
                require(
                        model.getQuads(state, face, random, ModelData.EMPTY, null).size() == 1,
                        "Formed part is not one quad: " + state);
                formedFaces++;
            }
            if (-offset.getY() == 3 && offset.getX() == 0 && offset.getZ() == -1) moduleSlot = slot;
        }
        require(moduleSlot != 0, "Missing waist fixture");
        for (var holder : ModBlocks.BLOCKS.getEntries()) {
            var block = holder.get();
            if (!(block instanceof StructureMinerUpgradeBlock)) continue;
            BlockState loose = block.defaultBlockState(),
                    formed = loose.setValue(AssemblyPanelBlock.ASSEMBLY_SLOT, moduleSlot);
            BakedModel model = registry.get(BlockModelShaper.stateToModelLocation(formed));
            require(
                    model.getQuads(
                                            loose,
                                            Direction.NORTH,
                                            random,
                                            ModelData.EMPTY,
                                            RenderType.cutout())
                                    .size()
                            == 2,
                    "Loose upgrade lost its identifier");
            require(
                    model.getQuads(
                                            formed,
                                            Direction.NORTH,
                                            random,
                                            ModelData.EMPTY,
                                            RenderType.cutout())
                                    .size()
                            == 1,
                    "Formed upgrade still repeats its icon");
        }
        var controllerModels = new java.util.HashSet<BakedModel>();
        for (var holder : ModBlocks.BLOCKS.getEntries()) {
            if (!(holder.get() instanceof BaseMinerBlock)) continue;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state =
                        holder.get()
                                .defaultBlockState()
                                .setValue(BaseMinerBlock.FACING, facing)
                                .setValue(BaseMinerBlock.ASSEMBLED, true);
                BakedModel model = registry.get(BlockModelShaper.stateToModelLocation(state));
                BlockState loose = state.setValue(BaseMinerBlock.ASSEMBLED, false);
                require(
                        model == registry.get(BlockModelShaper.stateToModelLocation(loose)),
                        "Formation must retain the original tier-specific controller model");
                if (facing == Direction.NORTH) controllerModels.add(model);
            }
        }
        require(controllerModels.size() == 6, "Six controller tiers must keep distinct models");
        // Forge getModelData must select the expected material and never query the
        // world again in getQuads. Exercise full and concave connections on real quads.
        PanelMaterialBank bank = new PanelMaterialBank(registry);
        int[][] fixtures = {{0, 0}, {15, 15}, {255, 255}, {239, 239}, {16, 0}};
        BlockPos[] neighbours = {
            new BlockPos(0, 1, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, -1, 0),
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 1, 0),
            new BlockPos(-1, -1, 0),
            new BlockPos(1, -1, 0),
            new BlockPos(1, 1, 0)
        };
        var blocks =
                new net.minecraft.world.level.block.Block[] {
                    ModBlocks.STRUCTURE_MINER_CASING.get(),
                    ModBlocks.STRUCTURE_MINER_GLASS.get(),
                    ModBlocks.STRUCTURE_MINER_STRUCTURE.get()
                };
        String[] kinds = {"casing", "glass", "structure"};
        for (int i = 0; i < blocks.length; i++)
            for (int[] fixture : fixtures) {
                BlockState state = blocks[i].defaultBlockState();
                BakedModel model = registry.get(BlockModelShaper.stateToModelLocation(state));
                Grid world = new Grid();
                world.cells.put(BlockPos.ZERO, state);
                for (int bit = 0; bit < 8; bit++)
                    if ((fixture[0] & (1 << bit)) != 0) world.cells.put(neighbours[bit], state);
                ModelData data = model.getModelData(world, BlockPos.ZERO, state, ModelData.EMPTY);
                world.reads = 0;
                var actual = model.getQuads(state, Direction.NORTH, random, data, null);
                require(
                        actual.equals(bank.face(kinds[i] + "_" + fixture[1], Direction.NORTH)),
                        "Incorrect CTM topology " + kinds[i] + "/" + fixture[0]);
                for (int repeat = 0; repeat < 100; repeat++)
                    require(
                            model.getQuads(state, Direction.NORTH, random, data, null) == actual,
                            "Uncached quads");
                require(world.reads == 0, "getQuads queried neighbours");
                topologyCases++;
            }
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "passed");
            result.put("wrapped_blockstates", states);
            result.put("formed_faces", formedFaces);
            result.put("live_topology_fixtures", topologyCases);
            result.put("material_models", PanelMaterialBank.names().size());
            result.put("quad_calls_query_world", false);
            result.put("performance_fps_benchmark", "not performed");
            Files.writeString(
                    Path.of("redwedge-client-model-report.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(result));
            DimensionTechMod.LOGGER.info(
                    "RED WEDGE real client model validation passed: {} states, {} faces, {}"
                            + " fixtures",
                    states,
                    formedFaces,
                    topologyCases);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Writing client validation report", error);
        }
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class Grid implements BlockAndTintGetter {
        final Map<BlockPos, BlockState> cells = new HashMap<>();
        int reads;

        @Override
        public BlockState getBlockState(BlockPos pos) {
            reads++;
            return cells.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return 1;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return -1;
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }
}
