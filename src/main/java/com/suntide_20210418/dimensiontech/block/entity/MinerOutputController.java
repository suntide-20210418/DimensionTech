package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.integration.MinerIntegrationHooks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Owns reward generation, output hooks, and routing for completed miner cycles. */
final class MinerOutputController {
    private static final int ALL_FACES = (1 << Direction.values().length) - 1;
    private List<ItemStack> pending = List.of();
    private BaseMinerBlockEntity.OutputState outputState =
            BaseMinerBlockEntity.OutputState.ITEM_HANDLER;
    private int outputFaceMask = ALL_FACES;
    private boolean equipmentDismantling;
    private final Set<ResourceLocation> disabledItems = new HashSet<>();

    boolean blocked() { return !pending.isEmpty(); }
    int pendingCount() { return pending.stream().mapToInt(ItemStack::getCount).sum(); }
    BaseMinerBlockEntity.OutputState outputState() { return outputState; }
    void cycleOutputState() {
        outputState = outputState == BaseMinerBlockEntity.OutputState.ITEM_HANDLER
                ? BaseMinerBlockEntity.OutputState.ME_NETWORK
                : BaseMinerBlockEntity.OutputState.ITEM_HANDLER;
    }
    int outputFaceMask() { return outputFaceMask; }
    boolean outputFaceEnabled(Direction worldDirection) {
        return (outputFaceMask & (1 << worldDirection.ordinal())) != 0;
    }
    void toggleOutputFace(Direction worldDirection) { outputFaceMask ^= 1 << worldDirection.ordinal(); }
    boolean equipmentDismantling() { return equipmentDismantling; }
    void toggleEquipmentDismantling() { equipmentDismantling = !equipmentDismantling; }
    void toggleDisabledItem(ResourceLocation item) {
        if (!disabledItems.add(item)) disabledItems.remove(item);
    }
    boolean disabled(ResourceLocation item) { return disabledItems.contains(item); }
    Set<ResourceLocation> disabledItems() { return Set.copyOf(disabledItems); }

    List<ItemStack> emit(
            BaseMinerBlockEntity miner,
            MinecraftServer server,
            ServerLevel level,
            BlockPos position,
            List<BaseMinerBlockEntity.CompletedMarker> completed,
            int tier,
            Predicate<Direction> outputFaceEnabled,
            IntFunction<Integer> drawsForSlot) {
        List<ExpectationRewardGenerator.Cycle> cycles = new ArrayList<>();
        for (BaseMinerBlockEntity.CompletedMarker cycle : completed) {
            var loot = cycle.loot();
            if (Double.isFinite(loot.quantity()) && loot.quantity() > 0.0D) {
                cycles.add(
                        new ExpectationRewardGenerator.Cycle(
                                loot, cycle.parallel(), drawsForSlot.apply(loot.slot())));
            }
        }
        List<ItemStack> generated =
                ExpectationRewardGenerator.generate(
                        server, cycles, disabledItems, equipmentDismantling, tier);
        MinerIntegrationHooks.OutputResult hook =
                MinerIntegrationHooks.postOutput(miner, level, generated);
        return hook.cancelled()
                ? List.of()
                : MythicMinerOutputRouter.output(
                        level, position, outputState, outputFaceEnabled, hook.outputs());
    }

    List<ItemStack> retry(
            ServerLevel level,
            BlockPos position,
            Predicate<Direction> outputFaceEnabled) {
        return MythicMinerOutputRouter.output(level, position, outputState, outputFaceEnabled, pending);
    }

    void setPending(List<ItemStack> output) { pending = List.copyOf(output); }

    void save(CompoundTag tag, String equipmentTag, String pendingTag) {
        tag.putInt("ConfiguredOutputState", outputState.ordinal());
        tag.putInt("OutputFaceMask", outputFaceMask);
        tag.putBoolean(equipmentTag, equipmentDismantling);
        ListTag disabled = new ListTag();
        disabledItems.forEach(item -> disabled.add(StringTag.valueOf(item.toString())));
        tag.put("DisabledExpectedItems", disabled);
        ListTag output = new ListTag();
        pending.forEach(stack -> output.add(stack.save(new CompoundTag())));
        tag.put(pendingTag, output);
    }

    void load(CompoundTag tag, String equipmentTag, String pendingTag) {
        int ordinal = tag.contains("ConfiguredOutputState", Tag.TAG_INT)
                ? tag.getInt("ConfiguredOutputState") : BaseMinerBlockEntity.OutputState.ITEM_HANDLER.ordinal();
        outputState = ordinal == BaseMinerBlockEntity.OutputState.ME_NETWORK.ordinal()
                ? BaseMinerBlockEntity.OutputState.ME_NETWORK : BaseMinerBlockEntity.OutputState.ITEM_HANDLER;
        outputFaceMask = tag.contains("OutputFaceMask", Tag.TAG_INT)
                ? tag.getInt("OutputFaceMask") & ALL_FACES : ALL_FACES;
        equipmentDismantling = tag.getBoolean(equipmentTag);
        disabledItems.clear();
        for (Tag value : tag.getList("DisabledExpectedItems", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) disabledItems.add(id);
        }
        List<ItemStack> loaded = new ArrayList<>();
        for (Tag value : tag.getList(pendingTag, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.of((CompoundTag) value);
            if (!stack.isEmpty()) loaded.add(stack);
        }
        pending = List.copyOf(loaded);
    }
}
