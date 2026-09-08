package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.integration.MinerIntegrationHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Owns reward generation, output hooks, and routing for completed miner cycles. */
final class MinerOutputController {
    List<ItemStack> emit(
            BaseMinerBlockEntity miner,
            MinecraftServer server,
            ServerLevel level,
            BlockPos position,
            List<BaseMinerBlockEntity.CompletedMarker> completed,
            Set<ResourceLocation> disabledItems,
            boolean equipmentDismantling,
            int tier,
            BaseMinerBlockEntity.OutputState outputState,
            Predicate<Direction> outputFaceEnabled,
            IntFunction<Integer> drawsForSlot) {
        List<ExpectationRewardGenerator.Cycle> cycles = new ArrayList<>();
        for (BaseMinerBlockEntity.CompletedMarker cycle : completed) {
            var loot = cycle.loot();
            if (Double.isFinite(loot.quantity()) && loot.quantity() > 0.0D) {
                cycles.add(new ExpectationRewardGenerator.Cycle(
                        loot, cycle.parallel(), drawsForSlot.apply(loot.slot())));
            }
        }
        List<ItemStack> generated = ExpectationRewardGenerator.generate(
                server, cycles, disabledItems, equipmentDismantling, tier);
        MinerIntegrationHooks.OutputResult hook = MinerIntegrationHooks.postOutput(miner, level, generated);
        return hook.cancelled() ? List.of() : MythicMinerOutputRouter.output(
                level, position, outputState, outputFaceEnabled, hook.outputs());
    }

    List<ItemStack> retry(
            ServerLevel level,
            BlockPos position,
            BaseMinerBlockEntity.OutputState outputState,
            Predicate<Direction> outputFaceEnabled,
            List<ItemStack> pending) {
        return MythicMinerOutputRouter.output(level, position, outputState, outputFaceEnabled, pending);
    }
}
