package com.suntide_20210418.dimensiontech.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class LootTableLottery {

    private LootTableLottery() {
    }

    public static List<ItemStack> draw(
            ServerLevel level,
            Vec3 origin,
            Collection<ResourceLocation> lootTables,
            @Nullable Entity entity,
            float luck,
            int parallel) {
        if (lootTables.isEmpty() || parallel <= 0) {
            return List.of();
        }
        LootParams lootParams =
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, origin)
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, entity)
                        .withLuck(luck)
                        .create(LootContextParamSets.CHEST);

        List<ItemStack> results = new ArrayList<>();
        for (ResourceLocation lootTableId : lootTables) {
            LootTable lootTable = level.getServer().getLootData().getLootTable(lootTableId);
            for (int i = 0; i < parallel; i++) {
                lootTable.getRandomItems(lootParams, results::add);
            }
        }
        return List.copyOf(results);
    }
}
