package com.suntide_20210418.dimensiontech.loot;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the dimension core roll and player-scoped 20-chest pity to structure loot. */
@Mod.EventBusSubscriber(modid = DimensionTechMod.MOD_ID)
public final class DimensionCoreChestLoot {
    private static final String MISSES_TAG =
            DimensionTechMod.MOD_ID + ":dimension_core_chest_misses";
    private static final int PITY_CHEST = 20;
    private static final float DROP_CHANCE = 0.05F;

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

        CompoundTag unopenedData = container.saveWithoutMetadata();
        ResourceLocation lootTable = ResourceLocation.tryParse(unopenedData.getString("LootTable"));
        if (lootTable == null || !lootTable.getPath().startsWith("chests/")) {
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
