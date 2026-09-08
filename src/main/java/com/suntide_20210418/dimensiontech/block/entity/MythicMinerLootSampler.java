package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.block.entity.EquipmentDismantler;
import com.suntide_20210418.dimensiontech.block.entity.MythicMinerMarkerAnalysisCache;

import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Generates rewards from cached item expectations.
 *
 * <p>This is expectation-preserving reward generation, not a replay of Vanilla's original LootTable
 * joint distribution, pool selection, function chain, or random sequence.
 */
final class ExpectationRewardGenerator {
    private ExpectationRewardGenerator() {}

    static List<ItemStack> draw(
            ServerLevel level,
            Map<ResourceLocation, ExactProbability> expectedItems,
            Set<ResourceLocation> disabledItems,
            int draws) {
        if (draws <= 0) return List.of();
        List<WeightedItem> weightedItems = new ArrayList<>();
        double total = 0.0D;
        for (Map.Entry<ResourceLocation, ExactProbability> entry : expectedItems.entrySet()) {
            double weight = entry.getValue().finiteDoubleValue();
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item == null
                    || disabledItems.contains(entry.getKey())
                    || !Double.isFinite(weight)
                    || weight <= 0.0D) continue;
            weightedItems.add(new WeightedItem(item, weight));
            total += weight;
        }
        if (weightedItems.isEmpty() || !Double.isFinite(total) || total <= 0.0D) return List.of();

        List<ItemStack> result = new ArrayList<>();
        for (int draw = 0; draw < draws; draw++) {
            double target = level.random.nextDouble() * total;
            for (WeightedItem weighted : weightedItems) {
                target -= weighted.weight();
                if (target <= 0.0D) {
                    result.add(new ItemStack(weighted.item(), 1));
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Builds all loot for completed miner cycles, including filtering, dismantling, and rewards.
     */
    static List<ItemStack> generate(
            MinecraftServer server,
            List<Cycle> cycles,
            Set<ResourceLocation> disabledItems,
            boolean equipmentDismantling,
            int minerTier) {
        List<ItemStack> merged = new ArrayList<>();
        int clampedTier = Math.max(1, Math.min(6, minerTier));
        for (Cycle cycle : cycles) {
            MythicMinerMarkerAnalysisCache.CachedMarkerLoot cached = cycle.loot();
            ServerLevel lootLevel =
                    server.getLevel(ResourceKey.create(Registries.DIMENSION, cached.dimension()));
            if (lootLevel == null
                    || !Double.isFinite(cached.quantity())
                    || cached.quantity() <= 0.0D) {
                continue;
            }
            for (ItemStack generated :
                    draw(lootLevel, cached.expectedItems(), disabledItems, cycle.draws())) {
                List<ItemStack> output =
                        equipmentDismantling
                                ? EquipmentDismantler.dismantle(lootLevel, generated)
                                : List.of(generated);
                for (ItemStack stack : output) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemId == null || !disabledItems.contains(itemId))
                        mergeEquivalent(merged, stack);
                }
            }
            addDimensionCoreReward(lootLevel, merged, cycle.parallel(), clampedTier, disabledItems);
            addTieredRewards(merged, cycle.parallel(), clampedTier);
        }
        return List.copyOf(merged);
    }

    static void mergeEquivalent(List<ItemStack> mergedLoot, ItemStack stack) {
        if (stack.isEmpty()) return;
        for (ItemStack merged : mergedLoot) {
            if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(merged, stack)) {
                merged.grow(stack.getCount());
                return;
            }
        }
        mergedLoot.add(stack.copy());
    }

    private static void addTieredRewards(List<ItemStack> mergedLoot, int parallel, int tier) {
        int count = Math.min(10, Math.max(0, parallel));
        if (count <= 0) return;
        mergeEquivalent(
                mergedLoot, new ItemStack(ModItems.DIMENSION_FRAGMENTS[tier - 1].get(), count));
        mergeEquivalent(mergedLoot, new ItemStack(ModItems.MINING_TOKENS[tier - 1].get(), count));
    }

    private static void addDimensionCoreReward(
            ServerLevel level,
            List<ItemStack> mergedLoot,
            int parallel,
            int tier,
            Set<ResourceLocation> disabledItems) {
        if (disabledItems.contains(ModItems.DIMENSION_DECONSTRUCTION_CORE_ID)) return;
        int cores = 0;
        for (int roll = 0; roll < Math.min(10, Math.max(0, parallel)); roll++) {
            if (level.random.nextFloat() < Math.min(1.0F, 0.05F * tier)) cores++;
        }
        if (cores > 0) {
            mergeEquivalent(
                    mergedLoot, new ItemStack(ModItems.DIMENSION_DECONSTRUCTION_CORE.get(), cores));
        }
    }

    record Cycle(MythicMinerMarkerAnalysisCache.CachedMarkerLoot loot, int parallel, int draws) {}

    private record WeightedItem(Item item, double weight) {}
}
