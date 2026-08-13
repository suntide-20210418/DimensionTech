package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Immutable runtime snapshot required by context-sensitive loot semantics. */
public record LootAnalysisContext(
        Level level,
        Vec3 origin,
        Entity entity,
        float luck,
        Map<String, Integer> scores,
        Boolean killedByPlayer,
        Boolean survivesExplosion,
        ItemStack tool) {
    public LootAnalysisContext {
        scores = Map.copyOf(scores == null ? Map.of() : scores);
    }

    public static LootAnalysisContext at(Level level, BlockPos origin, float luck) {
        return new LootAnalysisContext(level, Vec3.atCenterOf(origin), null, luck, Map.of(), null, null, ItemStack.EMPTY);
    }
}
