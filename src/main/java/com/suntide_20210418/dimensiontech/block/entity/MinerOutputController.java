package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.integration.MinerIntegrationHooks;
import com.suntide_20210418.dimensiontech.integration.ae2.Ae2Integration;
import com.suntide_20210418.dimensiontech.structureminer.output.ExpectationRewardGenerator;
import com.suntide_20210418.dimensiontech.structureminer.output.StructureMinerOutputRouter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.ModList;

/** Owns reward generation, output hooks, and routing for completed miner cycles. */
final class MinerOutputController {
    private static final int ALL_FACES = (1 << Direction.values().length) - 1;
    private List<ItemStack> pending = List.of();
    private BaseMinerBlockEntity.OutputState outputState =
            BaseMinerBlockEntity.OutputState.ITEM_HANDLER;
    private int outputFaceMask = ALL_FACES;
    private boolean equipmentDismantling;

    /**
     * Disabled expected items, keyed by thread slot.
     *
     * <p>Per slot rather than one machine-wide set: each thread produces from its own marker, so a
     * player disabling a drop on one thread must not silently disable it on the others. A slot with
     * no entry disables nothing.
     */
    private final Map<Integer, Set<ResourceLocation>> disabledBySlot = new HashMap<>();

    private static final boolean AE2_LOADED = ModList.get().isLoaded("ae2");

    void extractFluid(
            BaseMinerBlockEntity miner,
            ServerLevel level,
            BlockPos position,
            FluidTank tank,
            net.minecraft.world.level.material.Fluid required,
            boolean enabled,
            Function<Direction, BaseMinerBlockEntity.FluidFaceMode> faceMode) {
        if (!enabled || required == null || tank.getFluidAmount() >= tank.getCapacity()) return;
        int remaining = tank.getCapacity() - tank.getFluidAmount();
        for (Direction direction : Direction.values()) {
            if (remaining <= 0
                    || faceMode.apply(direction) != BaseMinerBlockEntity.FluidFaceMode.INPUT)
                continue;
            BlockEntity adjacent = level.getBlockEntity(position.relative(direction));
            if (adjacent == null) continue;
            if (AE2_LOADED && Ae2Integration.isOnlineInterface(adjacent)) {
                if (outputState == BaseMinerBlockEntity.OutputState.ME_NETWORK) {
                    remaining -=
                            Ae2Integration.extractFluidFromInterfaceNetwork(
                                    adjacent, required, remaining, tank);
                }
                continue;
            }
            IFluidHandler handler =
                    adjacent.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite())
                            .orElse(null);
            if (handler == null) continue;
            FluidStack simulated =
                    handler.drain(
                            new FluidStack(required, remaining),
                            IFluidHandler.FluidAction.SIMULATE);
            int accepted = tank.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            FluidStack drained = handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            int filled = tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            if (filled < drained.getAmount()) miner.setChanged();
            remaining -= filled;
        }
    }

    boolean blocked() {
        return !pending.isEmpty();
    }

    int pendingCount() {
        return pending.stream().mapToInt(ItemStack::getCount).sum();
    }

    BaseMinerBlockEntity.OutputState outputState() {
        return outputState;
    }

    void cycleOutputState() {
        outputState =
                outputState == BaseMinerBlockEntity.OutputState.ITEM_HANDLER
                        ? BaseMinerBlockEntity.OutputState.ME_NETWORK
                        : BaseMinerBlockEntity.OutputState.ITEM_HANDLER;
    }

    int outputFaceMask() {
        return outputFaceMask;
    }

    boolean outputFaceEnabled(Direction worldDirection) {
        return (outputFaceMask & (1 << worldDirection.ordinal())) != 0;
    }

    void toggleOutputFace(Direction worldDirection) {
        outputFaceMask ^= 1 << worldDirection.ordinal();
    }

    boolean equipmentDismantling() {
        return equipmentDismantling;
    }

    void toggleEquipmentDismantling() {
        equipmentDismantling = !equipmentDismantling;
    }

    void toggleDisabledItem(int slot, ResourceLocation item) {
        Set<ResourceLocation> items = disabledItemsFor(slot);
        if (!items.remove(item)) items.add(item);
        // Drop the slot once it disables nothing, so a slot toggled on then off again leaves no
        // empty entry behind for the save to write.
        if (items.isEmpty()) disabledBySlot.remove(slot);
    }

    boolean disabled(int slot, ResourceLocation item) {
        return disabledBySlot.getOrDefault(slot, Set.of()).contains(item);
    }

    Set<ResourceLocation> disabledItems(int slot) {
        return Set.copyOf(disabledBySlot.getOrDefault(slot, Set.of()));
    }

    /**
     * Sets one thread's whole disabled set at once, for the select-all and deselect-all gestures.
     *
     * <p>An explicit replace rather than a loop of {@link #toggleDisabledItem}: a toggle is defined
     * against the current membership, so applying the same desired state twice would undo it. The
     * GUI sends one packet and the server applies one state, which is idempotent and cannot be
     * reordered into the wrong answer by a retransmit.
     *
     * @return true when the set actually changed, so the caller can skip the analysis refresh.
     */
    boolean replaceDisabledItems(int slot, Collection<ResourceLocation> items) {
        Set<ResourceLocation> next = new HashSet<>(items);
        if (next.equals(disabledBySlot.getOrDefault(slot, Set.of()))) return false;
        if (next.isEmpty()) {
            disabledBySlot.remove(slot);
        } else {
            disabledBySlot.put(slot, next);
        }
        return true;
    }

    /** The slot's mutable set, created on first use so untouched slots cost no allocation. */
    private Set<ResourceLocation> disabledItemsFor(int slot) {
        return disabledBySlot.computeIfAbsent(slot, ignored -> new HashSet<>());
    }

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
                // The disabled set travels with the cycle: each thread's marker owns its own set,
                // so
                // the generator must not see one machine-wide answer.
                cycles.add(
                        new ExpectationRewardGenerator.Cycle(
                                loot,
                                cycle.parallel(),
                                drawsForSlot.apply(loot.slot()),
                                disabledItems(loot.slot())));
            }
        }
        List<ItemStack> generated =
                ExpectationRewardGenerator.generate(server, cycles, equipmentDismantling, tier);
        MinerIntegrationHooks.OutputResult hook =
                MinerIntegrationHooks.postOutput(miner, level, generated);
        return hook.cancelled()
                ? List.of()
                : StructureMinerOutputRouter.output(
                        level,
                        position,
                        outputState == BaseMinerBlockEntity.OutputState.ME_NETWORK,
                        outputFaceEnabled,
                        hook.outputs());
    }

    List<ItemStack> retry(
            ServerLevel level, BlockPos position, Predicate<Direction> outputFaceEnabled) {
        return StructureMinerOutputRouter.output(
                level,
                position,
                outputState == BaseMinerBlockEntity.OutputState.ME_NETWORK,
                outputFaceEnabled,
                pending);
    }

    void setPending(List<ItemStack> output) {
        pending = List.copyOf(output);
    }

    /**
     * Hands over everything still waiting to be routed and clears the buffer. Used when the block
     * is removed: the reward was already paid for by a completed cycle, so it must reach the world
     * instead of vanishing with the block entity.
     */
    List<ItemStack> drainPending() {
        List<ItemStack> output = pending;
        pending = List.of();
        return output;
    }

    void save(CompoundTag tag, String equipmentTag, String pendingTag) {
        tag.putInt("ConfiguredOutputState", outputState.ordinal());
        tag.putInt("OutputFaceMask", outputFaceMask);
        tag.putBoolean(equipmentTag, equipmentDismantling);
        // One sub-list per slot, in slot order, so the shape stays dense regardless of which slots
        // the player touched.
        int maxSlot = disabledBySlot.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        ListTag disabled = new ListTag();
        for (int slot = 0; slot <= maxSlot; slot++) {
            ListTag items = new ListTag();
            for (ResourceLocation item : disabledBySlot.getOrDefault(slot, Set.of())) {
                items.add(StringTag.valueOf(item.toString()));
            }
            disabled.add(items);
        }
        tag.put("DisabledExpectedItems", disabled);
        ListTag output = new ListTag();
        pending.forEach(stack -> output.add(stack.save(new CompoundTag())));
        tag.put(pendingTag, output);
    }

    void load(CompoundTag tag, String equipmentTag, String pendingTag, int slotCount) {
        int ordinal =
                tag.contains("ConfiguredOutputState", Tag.TAG_INT)
                        ? tag.getInt("ConfiguredOutputState")
                        : BaseMinerBlockEntity.OutputState.ITEM_HANDLER.ordinal();
        outputState =
                ordinal == BaseMinerBlockEntity.OutputState.ME_NETWORK.ordinal()
                        ? BaseMinerBlockEntity.OutputState.ME_NETWORK
                        : BaseMinerBlockEntity.OutputState.ITEM_HANDLER;
        outputFaceMask =
                tag.contains("OutputFaceMask", Tag.TAG_INT)
                        ? tag.getInt("OutputFaceMask") & ALL_FACES
                        : ALL_FACES;
        equipmentDismantling = tag.getBoolean(equipmentTag);
        loadDisabledItems(tag, slotCount);
        List<ItemStack> loaded = new ArrayList<>();
        for (Tag value : tag.getList(pendingTag, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.of((CompoundTag) value);
            if (!stack.isEmpty()) loaded.add(stack);
        }
        pending = List.copyOf(loaded);
    }

    /**
     * Reads the per-thread disabled sets, seeding every thread from a pre-per-thread save's flat
     * list.
     *
     * <p>The old payload was a single machine-wide {@code ListTag} of item ids; the new one is a
     * list of per-slot lists. The two shapes have different element types, which is what lets the
     * reader pick the legacy branch without a version tag: {@code getList} with the flat element
     * type only returns the old payload, and the nested one only matches a list of lists.
     */
    private void loadDisabledItems(CompoundTag tag, int slotCount) {
        disabledBySlot.clear();
        ListTag legacy = tag.getList("DisabledExpectedItems", Tag.TAG_STRING);
        if (!legacy.isEmpty()) {
            // The old set was global, so every thread inherits it: that reproduces the pre-change
            // behaviour rather than silently re-enabling everything the player had turned off.
            Set<ResourceLocation> items = parseDisabledItems(legacy);
            for (int slot = 0; slot < slotCount; slot++) {
                disabledBySlot.put(slot, new HashSet<>(items));
            }
            return;
        }
        ListTag perSlot = tag.getList("DisabledExpectedItems", Tag.TAG_LIST);
        for (int slot = 0; slot < perSlot.size(); slot++) {
            Set<ResourceLocation> items = parseDisabledItems(perSlot.getList(slot));
            if (!items.isEmpty()) disabledBySlot.put(slot, new HashSet<>(items));
        }
    }

    private static Set<ResourceLocation> parseDisabledItems(ListTag tag) {
        Set<ResourceLocation> items = new HashSet<>();
        for (Tag value : tag) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) items.add(id);
        }
        return items;
    }
}
