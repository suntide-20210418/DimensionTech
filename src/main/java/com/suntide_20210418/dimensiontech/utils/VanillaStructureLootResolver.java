package com.suntide_20210418.dimensiontech.utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Authoritative container loot-table map for the vanilla structures whose tables cannot be
 * recovered from the template graph reachable from their structure JSON.
 *
 * <p>1.21.1 ships 34 structure JSON files. Twenty-five of them carry no {@code start_pool} because
 * their placement is driven by code, so {@code StructureLootAnalyzer.findLootTables} cannot reach
 * any template for them and the static template scan necessarily comes back empty. Those structures
 * fix their tables in one of three other places:
 *
 * <ul>
 *   <li><b>Template NBT</b> — {@code ruined_portal}; all 13 of its templates declare {@code
 *       LootTable} directly, but the structure JSON has no pool to reach them through.
 *   <li><b>Data markers</b> — handled by {@code StructurePiece#handleDataMarker}: {@code
 *       shipwreck}, {@code igloo}, {@code ocean_ruin}, {@code woodland_mansion}, {@code end_city}.
 *   <li><b>Direct placement calls</b> — {@code StructurePiece#createChest} in the piece code:
 *       {@code desert_pyramid}, {@code jungle_temple}, {@code stronghold}, {@code mineshaft},
 *       {@code nether_fortress}, {@code buried_treasure}.
 * </ul>
 *
 * <p>Jigsaw structures whose JSON does carry a {@code start_pool} ({@code ancient_city}, {@code
 * bastion_remnant}, {@code pillager_outpost}, {@code village_*}, {@code trial_chambers}) are
 * resolved by the template scan itself; they are still listed here so that the fallback stays
 * complete if that scan ever fails, and so this file is a single readable answer to "what does each
 * vanilla structure contain".
 *
 * <p>Every entry names the vanilla evidence it was derived from. A structure that provably places
 * no loot container resolves to {@link Resolution#hasContainers()} {@code == false} instead of
 * being absent, so the analysis can report an exact zero rather than "unsupported".
 *
 * <p>1.21 的 {@code BuiltInLootTables} 常量已经是 {@code ResourceKey<LootTable>}（战利品表成了注册表），
 * 所以这张表按注册表键保存，对外仍以 {@code ResourceLocation} 暴露（下游按 id 处理）。
 */
public final class VanillaStructureLootResolver {

    /**
     * A closed answer for one vanilla structure: either the fixed table set, or a proof that no
     * container table exists. {@code evidence} is the vanilla location the decision came from.
     */
    public record Resolution(List<ResourceLocation> tables, String evidence) {
        public Resolution {
            tables = List.copyOf(tables);
        }

        public boolean hasContainers() {
            return !tables.isEmpty();
        }
    }

    private static final Map<String, Resolution> RESOLUTIONS =
            Map.ofEntries(
                    // ---- Template-declared tables, reachable through a start_pool. ----
                    Map.entry(
                            "ancient_city",
                            fixed(
                                    "10/58 templates declare LootTable (ancient_city/*.nbt)",
                                    List.of(
                                            BuiltInLootTables.ANCIENT_CITY,
                                            BuiltInLootTables.ANCIENT_CITY_ICE_BOX))),
                    Map.entry(
                            "bastion_remnant",
                            fixed(
                                    "26/167 templates declare LootTable (bastion/*.nbt)",
                                    List.of(
                                            BuiltInLootTables.BASTION_TREASURE,
                                            BuiltInLootTables.BASTION_OTHER,
                                            BuiltInLootTables.BASTION_BRIDGE,
                                            BuiltInLootTables.BASTION_HOGLIN_STABLE))),
                    Map.entry(
                            "pillager_outpost",
                            fixed(
                                    "2/11 templates declare LootTable (pillager_outpost/*.nbt)",
                                    List.of(BuiltInLootTables.PILLAGER_OUTPOST))),
                    Map.entry(
                            "trial_chambers",
                            fixed(
                                    "17/170 templates declare LootTable or vault config.loot_table;"
                                            + " trial spawners eject the default spawner tables",
                                    List.of(
                                            BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR,
                                            BuiltInLootTables.TRIAL_CHAMBERS_ENTRANCE,
                                            BuiltInLootTables.TRIAL_CHAMBERS_INTERSECTION_BARREL,
                                            BuiltInLootTables.TRIAL_CHAMBERS_REWARD,
                                            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS,
                                            BuiltInLootTables.TRIAL_CHAMBERS_SUPPLY,
                                            BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR_DISPENSER,
                                            BuiltInLootTables.TRIAL_CHAMBERS_CHAMBER_DISPENSER,
                                            BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR_POT,
                                            BuiltInLootTables.SPAWNER_TRIAL_CHAMBER_CONSUMABLES,
                                            BuiltInLootTables.SPAWNER_TRIAL_CHAMBER_KEY,
                                            BuiltInLootTables
                                                    .SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES,
                                            BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY))),
                    Map.entry(
                            "village_desert",
                            fixed(
                                    "96 templates declare LootTable (village/desert/*.nbt)",
                                    List.of(
                                            BuiltInLootTables.VILLAGE_DESERT_HOUSE,
                                            BuiltInLootTables.VILLAGE_TEMPLE,
                                            BuiltInLootTables.VILLAGE_TOOLSMITH,
                                            BuiltInLootTables.VILLAGE_WEAPONSMITH))),
                    Map.entry(
                            "village_plains",
                            fixed(
                                    "125 templates declare LootTable (village/plains/*.nbt)",
                                    List.of(
                                            BuiltInLootTables.VILLAGE_CARTOGRAPHER,
                                            BuiltInLootTables.VILLAGE_FISHER,
                                            BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
                                            BuiltInLootTables.VILLAGE_TANNERY,
                                            BuiltInLootTables.VILLAGE_WEAPONSMITH))),
                    Map.entry(
                            "village_savanna",
                            fixed(
                                    "122 templates declare LootTable (village/savanna/*.nbt)",
                                    List.of(
                                            BuiltInLootTables.VILLAGE_BUTCHER,
                                            BuiltInLootTables.VILLAGE_CARTOGRAPHER,
                                            BuiltInLootTables.VILLAGE_MASON,
                                            BuiltInLootTables.VILLAGE_SAVANNA_HOUSE,
                                            BuiltInLootTables.VILLAGE_TANNERY,
                                            BuiltInLootTables.VILLAGE_WEAPONSMITH))),
                    Map.entry(
                            "village_snowy",
                            fixed(
                                    "111 templates declare LootTable (village/snowy/*.nbt)",
                                    List.of(
                                            BuiltInLootTables.VILLAGE_ARMORER,
                                            BuiltInLootTables.VILLAGE_CARTOGRAPHER,
                                            BuiltInLootTables.VILLAGE_SHEPHERD,
                                            BuiltInLootTables.VILLAGE_SNOWY_HOUSE,
                                            BuiltInLootTables.VILLAGE_TANNERY,
                                            BuiltInLootTables.VILLAGE_WEAPONSMITH))),
                    Map.entry(
                            "village_taiga",
                            fixed(
                                    "116 templates declare LootTable (village/taiga/*.nbt)",
                                    List.of(
                                            BuiltInLootTables.VILLAGE_CARTOGRAPHER,
                                            BuiltInLootTables.VILLAGE_FLETCHER,
                                            BuiltInLootTables.VILLAGE_TAIGA_HOUSE,
                                            BuiltInLootTables.VILLAGE_TANNERY,
                                            BuiltInLootTables.VILLAGE_TOOLSMITH,
                                            BuiltInLootTables.VILLAGE_WEAPONSMITH))),

                    // ---- Template-declared tables, but the structure JSON has no start_pool. ----
                    Map.entry(
                            "ruined_portal",
                            fixed(
                                    "13/13 templates declare LootTable; RuinedPortalPiece:181"
                                            + " leaves handleDataMarker empty",
                                    List.of(BuiltInLootTables.RUINED_PORTAL))),
                    Map.entry(
                            "ruined_portal_desert",
                            fixed(
                                    "same templates as ruined_portal",
                                    List.of(BuiltInLootTables.RUINED_PORTAL))),
                    Map.entry(
                            "ruined_portal_jungle",
                            fixed(
                                    "same templates as ruined_portal",
                                    List.of(BuiltInLootTables.RUINED_PORTAL))),
                    Map.entry(
                            "ruined_portal_swamp",
                            fixed(
                                    "same templates as ruined_portal",
                                    List.of(BuiltInLootTables.RUINED_PORTAL))),
                    Map.entry(
                            "ruined_portal_mountain",
                            fixed(
                                    "same templates as ruined_portal",
                                    List.of(BuiltInLootTables.RUINED_PORTAL))),
                    Map.entry(
                            "ruined_portal_ocean",
                            fixed(
                                    "same templates as ruined_portal",
                                    List.of(BuiltInLootTables.RUINED_PORTAL))),
                    Map.entry(
                            "ruined_portal_nether",
                            fixed(
                                    "same templates as ruined_portal",
                                    List.of(BuiltInLootTables.RUINED_PORTAL))),

                    // ---- Data markers resolved by StructurePiece#handleDataMarker. ----
                    Map.entry(
                            "shipwreck",
                            fixed(
                                    "ShipwreckPieces:69-76 MARKERS_TO_LOOT, applied at"
                                            + " ShipwreckPieces:118-123",
                                    List.of(
                                            BuiltInLootTables.SHIPWRECK_MAP,
                                            BuiltInLootTables.SHIPWRECK_SUPPLY,
                                            BuiltInLootTables.SHIPWRECK_TREASURE))),
                    Map.entry(
                            "shipwreck_beached",
                            fixed(
                                    "same piece code as shipwreck; only the template pool differs",
                                    List.of(
                                            BuiltInLootTables.SHIPWRECK_MAP,
                                            BuiltInLootTables.SHIPWRECK_SUPPLY,
                                            BuiltInLootTables.SHIPWRECK_TREASURE))),
                    Map.entry(
                            "igloo",
                            fixed(
                                    "IglooPieces:97-105 marker \"chest\" sets IGLOO_CHEST",
                                    List.of(BuiltInLootTables.IGLOO_CHEST))),
                    Map.entry(
                            "mansion",
                            fixed(
                                    "WoodlandMansionPieces:1306-1320 markers starting with"
                                            + " \"Chest\"",
                                    List.of(BuiltInLootTables.WOODLAND_MANSION))),
                    Map.entry(
                            "end_city",
                            fixed(
                                    "EndCityPieces:384-389 markers starting with \"Chest\"",
                                    List.of(BuiltInLootTables.END_CITY_TREASURE))),
                    Map.entry(
                            "ocean_ruin_cold",
                            fixed(
                                    "OceanRuinPieces:309-320 marker \"chest\", table chosen by"
                                            + " OceanRuinPiece.isLarge",
                                    List.of(
                                            BuiltInLootTables.UNDERWATER_RUIN_SMALL,
                                            BuiltInLootTables.UNDERWATER_RUIN_BIG))),
                    Map.entry(
                            "ocean_ruin_warm",
                            fixed(
                                    "same piece code as ocean_ruin_cold; only the template group"
                                            + " differs",
                                    List.of(
                                            BuiltInLootTables.UNDERWATER_RUIN_SMALL,
                                            BuiltInLootTables.UNDERWATER_RUIN_BIG))),

                    // ---- Tables injected by direct createChest calls in the piece code. ----
                    Map.entry(
                            "buried_treasure",
                            fixed(
                                    "BuriedTreasurePieces:72 createChest",
                                    List.of(BuiltInLootTables.BURIED_TREASURE))),
                    Map.entry(
                            "desert_pyramid",
                            fixed(
                                    "DesertPyramidPiece:394 createChest; four chests per pyramid",
                                    List.of(BuiltInLootTables.DESERT_PYRAMID))),
                    Map.entry(
                            "jungle_pyramid",
                            fixed(
                                    "JungleTemplePiece:252/350 createDispenser and 356/400"
                                            + " createChest",
                                    List.of(
                                            BuiltInLootTables.JUNGLE_TEMPLE,
                                            BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER))),
                    Map.entry(
                            "mineshaft",
                            fixed(
                                    "MineshaftPieces:415/419 createChest",
                                    List.of(BuiltInLootTables.ABANDONED_MINESHAFT))),
                    Map.entry(
                            "mineshaft_mesa",
                            fixed(
                                    "same piece code as mineshaft; only the biome/template style"
                                            + " differs",
                                    List.of(BuiltInLootTables.ABANDONED_MINESHAFT))),
                    Map.entry(
                            "fortress",
                            fixed(
                                    "NetherFortressPieces:885/1024 createChest",
                                    List.of(BuiltInLootTables.NETHER_BRIDGE))),
                    Map.entry(
                            "stronghold",
                            fixed(
                                    "StrongholdPieces:299 corridor, 779/782 library, 1276 crossing",
                                    List.of(
                                            BuiltInLootTables.STRONGHOLD_LIBRARY,
                                            BuiltInLootTables.STRONGHOLD_CROSSING,
                                            BuiltInLootTables.STRONGHOLD_CORRIDOR))),

                    // ---- Structures that provably place no loot container. ----
                    Map.entry(
                            "monument",
                            noContainers(
                                    "OceanMonumentPieces never references BuiltInLootTables; the"
                                        + " monument places sponge rooms and dark prismarine, no"
                                        + " container")),
                    Map.entry(
                            "swamp_hut",
                            noContainers(
                                    "SwampHutPiece builds planks, stairs, a cauldron and a crafting"
                                        + " table only; it spawns a witch and a cat and places no"
                                        + " chest")),
                    Map.entry(
                            "nether_fossil",
                            noContainers(
                                    "NetherFossilPieces:67 handleDataMarker is an empty override;"
                                            + " no container is placed")),
                    Map.entry(
                            "trail_ruins",
                            noContainers(
                                    "84 templates declare no LootTable and TrailRuinsPieces has no"
                                        + " handleDataMarker or createChest; the structure yields"
                                        + " archaeology, not containers")));

    private VanillaStructureLootResolver() {}

    /**
     * Resolves one structure id. {@link Optional#empty()} means "not a vanilla structure this
     * module has an authoritative answer for" (modded namespaces and structures added by data
     * packs), which is deliberately distinct from a {@link Resolution} whose table list is empty.
     */
    public static Optional<Resolution> resolve(ResourceLocation structureId) {
        if (!"minecraft".equals(structureId.getNamespace())) return Optional.empty();
        return Optional.ofNullable(RESOLUTIONS.get(structureId.getPath()));
    }

    private static Resolution fixed(String evidence, List<ResourceKey<LootTable>> keys) {
        return new Resolution(keys.stream().map(ResourceKey::location).toList(), evidence);
    }

    private static Resolution noContainers(String evidence) {
        return new Resolution(List.of(), evidence);
    }
}
