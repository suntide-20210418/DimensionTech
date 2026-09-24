package com.suntide_20210418.dimensiontech.loot;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Adds the dimension core roll and player-scoped 20-chest pity to structure loot. */
@EventBusSubscriber(modid = DimensionTechMod.MOD_ID)
public final class DimensionCoreChestLoot {
    private static final String MISSES_TAG =
            DimensionTechMod.MOD_ID + ":dimension_core_chest_misses";

    /** Consecutive failed chests before the next one is guaranteed to hold a core. */
    public static final int PITY_CHEST = 20;

    /** Base per-chest core chance. */
    public static final float DROP_CHANCE = 0.05F;

    private DimensionCoreChestLoot() {}

    @SubscribeEvent
    public static void onOpenContainer(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getLevel().isClientSide()) {
            return;
        }
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (!(blockEntity instanceof RandomizableContainerBlockEntity container)) {
            return;
        }
        if (!container.canOpen(player)) {
            return;
        }

        // 1.21 的战利品表指针不再是根 tag 里的 "LootTable" 字符串：
        // RandomizableContainerBlockEntity 把它存在 DataComponents.CONTAINER_LOOT
        // （SeededContainerLoot）里，saveAdditional 还会显式移除该 NBT 键
        // （见 RandomizableContainerBlockEntity#saveAdditional / #setComponents），所以这里改读注册表键。
        ResourceKey<LootTable> lootTable = container.getLootTable();
        if (lootTable == null || !lootTable.location().getPath().startsWith("chests/")) {
            return;
        }

        // Expanding here makes this event the single point that counts and modifies the chest.
        container.unpackLootTable(player);
        CompoundTag persistentData = player.getPersistentData();
        int misses = Math.max(0, persistentData.getInt(MISSES_TAG));
        boolean guaranteed = misses >= PITY_CHEST - 1;
        boolean generated = guaranteed || player.getRandom().nextFloat() < DROP_CHANCE;
        if (generated) {
            insertCore(container, player);
            persistentData.putInt(MISSES_TAG, 0);
        } else {
            persistentData.putInt(MISSES_TAG, misses + 1);
        }
        container.setChanged();
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag originalData = event.getOriginal().getPersistentData();
        if (originalData.contains(MISSES_TAG, CompoundTag.TAG_INT)) {
            event.getEntity()
                    .getPersistentData()
                    .putInt(MISSES_TAG, originalData.getInt(MISSES_TAG));
        }
    }

    private static void insertCore(
            RandomizableContainerBlockEntity container, ServerPlayer player) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(
                        slot, new ItemStack(ModItems.DIMENSION_DECONSTRUCTION_CORE.get()));
                return;
            }
        }

        int slot = player.getRandom().nextInt(container.getContainerSize());
        ItemStack displaced = container.getItem(slot).copy();
        container.setItem(slot, new ItemStack(ModItems.DIMENSION_DECONSTRUCTION_CORE.get()));
        Containers.dropItemStack(
                player.serverLevel(),
                container.getBlockPos().getX() + 0.5D,
                container.getBlockPos().getY() + 1.0D,
                container.getBlockPos().getZ() + 0.5D,
                displaced);
    }
}
