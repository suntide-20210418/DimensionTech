package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.integration.MinerIntegrationHooks;
import com.suntide_20210418.dimensiontech.structureminer.output.ExpectationRewardGenerator;
import com.suntide_20210418.dimensiontech.utils.FullDurabilityLoot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Owns reward generation, output hooks and the pending queue for completed miner cycles.
 *
 * <p><b>The queue stays here, the routing moved out.</b> The controller keeps the items a cycle
 * produced until the item output chamber has taken them, because the queue has to survive with the
 * machine and be dropped when it breaks; the chamber is the thing that knows how to leave the
 * building. {@link #retry} is the seam: the chamber ejects what it can and hands the rest back.
 * With no chamber installed the queue simply never drains, which is the machine's hard stop.
 */
final class MinerOutputController {
    private List<ItemStack> pending = List.of();
    private boolean equipmentDismantling;

    /**
     * Disabled expected items, keyed by thread slot.
     *
     * <p>Per slot rather than one machine-wide set: each thread produces from its own marker, so a
     * player disabling a drop on one thread must not silently disable it on the others. A slot with
     * no entry disables nothing.
     */
    private final Map<Integer, Set<ResourceLocation>> disabledBySlot = new HashMap<>();

    boolean blocked() {
        return !pending.isEmpty();
    }

    int pendingCount() {
        return pending.stream().mapToInt(ItemStack::getCount).sum();
    }

    /** The rewards still waiting for an output target, in routing order. */
    List<ItemStack> pendingItems() {
        return pending;
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

    /**
     * Generates this batch's rewards and applies the output hook, returning what has to leave the
     * machine. Nothing is routed here any more — the caller stores the result as the pending queue
     * and hands it to the item output chamber.
     */
    List<ItemStack> emit(
            BaseMinerBlockEntity miner,
            MinecraftServer server,
            ServerLevel level,
            List<BaseMinerBlockEntity.CompletedMarker> completed,
            int tier,
            IntFunction<Integer> drawsForSlot) {
        List<ExpectationRewardGenerator.Cycle> cycles = new ArrayList<>();
        for (BaseMinerBlockEntity.CompletedMarker cycle : completed) {
            var loot = cycle.loot();
            if (Double.isFinite(loot.quantity()) && loot.quantity() > 0.0D) {
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
        // The full-durability policy used to be applied per stack by the output router. With
        // routing
        // delegated to the item output chamber it has to be applied here, before the queue exists,
        // so both the container path and the ME path see the same stacks.
        return hook.cancelled() ? List.of() : FullDurabilityLoot.normalize(hook.outputs());
    }

    /**
     * Offers the pending queue to the item output chamber and keeps whatever it could not take.
     *
     * <p>With no chamber installed the queue is returned unchanged, so the machine stays blocked
     * instead of venting its output into the world.
     */
    List<ItemStack> retry(
            ServerLevel level,
            net.minecraft.core.BlockPos position,
            ItemOutputChamberBlockEntity chamber) {
        if (chamber == null || pending.isEmpty()) return pending;
        return chamber.eject(level, pending);
    }

    void setPending(List<ItemStack> output) {
        pending = List.copyOf(output);
    }

    void save(
            CompoundTag tag,
            String equipmentTag,
            String pendingTag,
            HolderLookup.Provider registries) {
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
        pending.forEach(stack -> output.add(stack.saveOptional(registries)));
        tag.put(pendingTag, output);
    }

    void load(
            CompoundTag tag,
            String equipmentTag,
            String pendingTag,
            HolderLookup.Provider registries,
            int slotCount) {
        equipmentDismantling = tag.getBoolean(equipmentTag);
        loadDisabledItems(tag, slotCount);
        List<ItemStack> loaded = new ArrayList<>();
        for (Tag value : tag.getList(pendingTag, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.parseOptional(registries, (CompoundTag) value);
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
