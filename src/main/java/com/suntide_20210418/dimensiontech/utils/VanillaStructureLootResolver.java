package com.suntide_20210418.dimensiontech.utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Resolves the fixed chest tables used by vanilla structures whose templates do not carry a
 * LootTable tag. This intentionally applies only to the minecraft namespace; mod structures must
 * be discovered from their templates or sampled in the virtual generator.
 */
public final class VanillaStructureLootResolver {
    private static final Map<String, List<ResourceLocation>> TABLES = Map.ofEntries(
            Map.entry("ancient_city", List.of(BuiltInLootTables.ANCIENT_CITY)),
            Map.entry("bastion_remnant", List.of(BuiltInLootTables.BASTION_TREASURE,
                    BuiltInLootTables.BASTION_OTHER, BuiltInLootTables.BASTION_BRIDGE,
                    BuiltInLootTables.BASTION_HOGLIN_STABLE)),
            Map.entry("buried_treasure", List.of(BuiltInLootTables.BURIED_TREASURE)),
            Map.entry("desert_pyramid", List.of(BuiltInLootTables.DESERT_PYRAMID)),
            Map.entry("end_city", List.of(BuiltInLootTables.END_CITY_TREASURE)),
            Map.entry("igloo", List.of(BuiltInLootTables.IGLOO_CHEST)),
            Map.entry("jungle_pyramid", List.of(BuiltInLootTables.JUNGLE_TEMPLE,
                    BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER)),
            Map.entry("mineshaft", List.of(BuiltInLootTables.ABANDONED_MINESHAFT)),
            Map.entry("mineshaft_mesa", List.of(BuiltInLootTables.ABANDONED_MINESHAFT)),
            Map.entry("fortress", List.of(BuiltInLootTables.NETHER_BRIDGE)),
            Map.entry("pillager_outpost", List.of(BuiltInLootTables.PILLAGER_OUTPOST)),
            Map.entry("ruined_portal", List.of(BuiltInLootTables.RUINED_PORTAL)),
            Map.entry("ruined_portal_desert", List.of(BuiltInLootTables.RUINED_PORTAL)),
            Map.entry("ruined_portal_jungle", List.of(BuiltInLootTables.RUINED_PORTAL)),
            Map.entry("ruined_portal_swamp", List.of(BuiltInLootTables.RUINED_PORTAL)),
            Map.entry("ruined_portal_mountain", List.of(BuiltInLootTables.RUINED_PORTAL)),
            Map.entry("ruined_portal_ocean", List.of(BuiltInLootTables.RUINED_PORTAL)),
            Map.entry("ruined_portal_nether", List.of(BuiltInLootTables.RUINED_PORTAL)),
            Map.entry("shipwreck", List.of(BuiltInLootTables.SHIPWRECK_MAP,
                    BuiltInLootTables.SHIPWRECK_SUPPLY, BuiltInLootTables.SHIPWRECK_TREASURE)),
            Map.entry("shipwreck_beached", List.of(BuiltInLootTables.SHIPWRECK_MAP,
                    BuiltInLootTables.SHIPWRECK_SUPPLY, BuiltInLootTables.SHIPWRECK_TREASURE)),
            Map.entry("stronghold", List.of(BuiltInLootTables.STRONGHOLD_LIBRARY,
                    BuiltInLootTables.STRONGHOLD_CROSSING, BuiltInLootTables.STRONGHOLD_CORRIDOR)),
            Map.entry("ocean_ruin_cold", List.of(BuiltInLootTables.UNDERWATER_RUIN_SMALL,
                    BuiltInLootTables.UNDERWATER_RUIN_BIG)),
            Map.entry("ocean_ruin_warm", List.of(BuiltInLootTables.UNDERWATER_RUIN_SMALL,
                    BuiltInLootTables.UNDERWATER_RUIN_BIG)),
            Map.entry("mansion", List.of(BuiltInLootTables.WOODLAND_MANSION)));

    private VanillaStructureLootResolver() {}

    public static Optional<List<ResourceLocation>> resolve(ResourceLocation structureId) {
        if (!"minecraft".equals(structureId.getNamespace())) return Optional.empty();
        return Optional.ofNullable(TABLES.get(structureId.getPath()));
    }
}
