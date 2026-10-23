package com.suntide_20210418.dimensiontech.utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Authoritative container loot-table map for the vanilla structures whose tables cannot be
 * recovered from the template graph reachable from their structure JSON.
 *
 * <p>1.20.1 ships 33 structure JSON files. Twenty-four of them carry no {@code start_pool} because
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
 * bastion_remnant}, {@code pillager_outpost}, {@code village_*}) are resolved by the template scan
 * itself; they are still listed here so that the fallback stays complete if that scan ever fails,
 * and so this file is a single readable answer to "what does each vanilla structure contain".
 *
 * <p>Every entry names the vanilla evidence it was derived from. A structure that provably places
 * no loot container resolves to {@link Resolution#hasContainers()} {@code == false} instead of
 * being absent, so the analysis can report an exact zero rather than "unsupported".
 *
 * <p>Unlike 1.21, {@code BuiltInLootTables} constants here are plain {@link ResourceLocation}
 * values rather than {@code ResourceKey<LootTable>}, so this table stores ids directly and needs no
 * registry-key unwrapping.
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
                                    "13/13 templates declare LootTable; RuinedPortalPiece leaves"
                                            + " handleDataMarker empty",
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
                                    "ShipwreckPieces MARKERS_TO_LOOT applied when a chest marker"
                                            + " is handled",
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
                                    "IglooPieces marker \"chest\" sets IGLOO_CHEST",
                                    List.of(BuiltInLootTables.IGLOO_CHEST))),
                    Map.entry(
                            "mansion",
                            fixed(
                                    "WoodlandMansionPieces markers starting with \"Chest\"",
                                    List.of(BuiltInLootTables.WOODLAND_MANSION))),
                    Map.entry(
                            "end_city",
                            fixed(
                                    "EndCityPieces markers starting with \"Chest\"",
                                    List.of(BuiltInLootTables.END_CITY_TREASURE))),
                    Map.entry(
                            "ocean_ruin_cold",
                            fixed(
                                    "OceanRuinPieces marker \"chest\", table chosen by"
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
                                    "BuriedTreasurePieces createChest",
                                    List.of(BuiltInLootTables.BURIED_TREASURE))),
                    Map.entry(
                            "desert_pyramid",
                            fixed(
                                    "DesertPyramidPiece createChest; four chests per pyramid",
                                    List.of(BuiltInLootTables.DESERT_PYRAMID))),
                    Map.entry(
                            "jungle_pyramid",
                            fixed(
                                    "JungleTemplePiece createDispenser and createChest",
                                    List.of(
                                            BuiltInLootTables.JUNGLE_TEMPLE,
                                            BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER))),
                    Map.entry(
                            "mineshaft",
                            fixed(
                                    "MineshaftPieces createChest",
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
                                    "NetherFortressPieces createChest",
                                    List.of(BuiltInLootTables.NETHER_BRIDGE))),
                    Map.entry(
                            "stronghold",
                            fixed(
                                    "StrongholdPieces corridor, library and crossing createChest",
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
                                    "NetherFossilPieces handleDataMarker is an empty override; no"
                                            + " container is placed")),
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

    private static Resolution fixed(String evidence, List<ResourceLocation> tables) {
        return new Resolution(tables, evidence);
    }

    private static Resolution noContainers(String evidence) {
        return new Resolution(List.of(), evidence);
    }
}
