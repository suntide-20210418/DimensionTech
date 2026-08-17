package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Immutable runtime snapshot required by context-sensitive loot semantics. */
public record LootAnalysisContext(
        Level level,
        Vec3 origin,
        Entity entity,
        float luck,
        Map<String, Integer> scores,
        Boolean killedByPlayer,
        Float explosionRadius,
        ItemStack tool,
        Map<LootContext.EntityTarget, Entity> entityTargets,
        int lootingModifier,
        boolean fullDurability) {
    public LootAnalysisContext {
        scores = Map.copyOf(scores == null ? Map.of() : scores);
        entityTargets = Map.copyOf(entityTargets == null ? Map.of() : entityTargets);
    }

    public LootAnalysisContext(
            Level level,
            Vec3 origin,
            Entity entity,
            float luck,
            Map<String, Integer> scores,
            Boolean killedByPlayer,
            Float explosionRadius,
            ItemStack tool,
            Map<LootContext.EntityTarget, Entity> entityTargets) {
        this(
                level,
                origin,
                entity,
                luck,
                scores,
                killedByPlayer,
                explosionRadius,
                tool,
                entityTargets,
                0,
                false);
    }

    public LootAnalysisContext(
            Level level,
            Vec3 origin,
            Entity entity,
            float luck,
            Map<String, Integer> scores,
            Boolean killedByPlayer,
            Float explosionRadius,
            ItemStack tool,
            Map<LootContext.EntityTarget, Entity> entityTargets,
            int lootingModifier) {
        this(
                level,
                origin,
                entity,
                luck,
                scores,
                killedByPlayer,
                explosionRadius,
                tool,
                entityTargets,
                lootingModifier,
                false);
    }

    public static LootAnalysisContext at(Level level, BlockPos origin, float luck) {
        return new LootAnalysisContext(
                level,
                Vec3.atCenterOf(origin),
                null,
                luck,
                Map.of(),
                null,
                null,
                ItemStack.EMPTY,
                Map.of(),
                0,
                false);
    }

    public LootAnalysisContext withLootingModifier(int value) {
        return new LootAnalysisContext(
                level,
                origin,
                entity,
                luck,
                scores,
                killedByPlayer,
                explosionRadius,
                tool,
                entityTargets,
                value,
                fullDurability);
    }

    public LootAnalysisContext withFullDurability() {
        return new LootAnalysisContext(
                level,
                origin,
                entity,
                luck,
                scores,
                killedByPlayer,
                explosionRadius,
                tool,
                entityTargets,
                lootingModifier,
                true);
    }

    public Entity entity(LootContext.EntityTarget target) {
        Entity resolved = entityTargets.get(target);
        return resolved != null || target != LootContext.EntityTarget.THIS ? resolved : entity;
    }
}
