package com.suntide_20210418.dimensiontech.structure.analysis;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.utils.VanillaStructureLootResolver;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Pins the "which vanilla structure yields which loot tables" contract.
 *
 * <p>1.21.1 ships 34 structure JSON files. Only 9 of them reach a template pool through {@code
 * start_pool}; the other 25 are placed by code, so the template scan cannot see them and {@link
 * VanillaStructureLootResolver} answers instead. These tests cover both resolution paths plus the
 * "no container at all" case, so a future change cannot silently drop a structure back to
 * "unsupported".
 *
 * <p>The template and resolver calls are synchronous static entry points on purpose: {@code
 * StructureAnalysisService.discover} admits at most {@code virtualStructureStepsPerTick}
 * submissions per tick ({@code MainThreadTaskCache.submit} fails outright once the budget is gone),
 * so tests that need a live server call it at most once.
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VanillaStructureLootGameTests {
    private VanillaStructureLootGameTests() {}

    /**
     * Every vanilla structure in the registry must have an authoritative answer. This is the guard
     * against a new vanilla structure silently falling through to UNSUPPORTED.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 200)
    public static void everyRegisteredVanillaStructureHasAResolution(GameTestHelper helper) {
        Set<ResourceLocation> vanilla =
                helper
                        .getLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.STRUCTURE)
                        .keySet()
                        .stream()
                        .filter(id -> "minecraft".equals(id.getNamespace()))
                        .collect(Collectors.toSet());
        if (vanilla.isEmpty()) {
            helper.fail("No vanilla structures are registered; the assertion would be vacuous");
        }
        List<String> unresolved =
                vanilla.stream()
                        .filter(id -> VanillaStructureLootResolver.resolve(id).isEmpty())
                        .map(ResourceLocation::toString)
                        .sorted()
                        .toList();
        if (!unresolved.isEmpty()) {
            helper.fail("Vanilla structures without a loot-table resolution: " + unresolved);
        }
        helper.succeed();
    }

    /**
     * Jigsaw structures resolve through the template scan. Trial chambers is the demanding case:
     * its tables come from three field shapes at once — plain {@code LootTable} on containers and
     * pots, {@code config.loot_table} on vaults, and {@code loot_tables_to_eject} on trial
     * spawners. Dropping any of them silently understates the payout, and the ominous vault in
     * particular vanishes without the {@code config.loot_table} branch.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void templateScanResolvesJigsawStructureTables(GameTestHelper helper) {
        requireTemplateTables(
                helper,
                "ancient_city",
                "minecraft:chests/ancient_city",
                "minecraft:chests/ancient_city_ice_box");
        requireTemplateTables(
                helper,
                "trial_chambers",
                "minecraft:chests/trial_chambers/corridor",
                "minecraft:chests/trial_chambers/entrance",
                "minecraft:chests/trial_chambers/intersection_barrel",
                "minecraft:chests/trial_chambers/reward",
                "minecraft:chests/trial_chambers/reward_ominous",
                "minecraft:chests/trial_chambers/supply",
                "minecraft:dispensers/trial_chambers/chamber",
                "minecraft:dispensers/trial_chambers/corridor",
                "minecraft:pots/trial_chambers/corridor",
                "minecraft:spawners/ominous/trial_chamber/consumables",
                "minecraft:spawners/ominous/trial_chamber/key",
                "minecraft:spawners/trial_chamber/consumables",
                "minecraft:spawners/trial_chamber/key");
        requireTemplateTables(
                helper,
                "village_plains",
                "minecraft:chests/village/village_cartographer",
                "minecraft:chests/village/village_fisher",
                "minecraft:chests/village/village_plains_house",
                "minecraft:chests/village/village_tannery",
                "minecraft:chests/village/village_weaponsmith");
        helper.succeed();
    }

    /**
     * Code-placed structures resolve through the fixed map: template-declared ({@code
     * ruined_portal}), data marker ({@code shipwreck}) and direct placement ({@code stronghold},
     * {@code desert_pyramid}).
     */
    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void fixedMapResolvesCodePlacedStructureTables(GameTestHelper helper) {
        requireFixedTables(helper, "desert_pyramid", "minecraft:chests/desert_pyramid");
        requireFixedTables(helper, "ruined_portal", "minecraft:chests/ruined_portal");
        requireFixedTables(
                helper,
                "shipwreck",
                "minecraft:chests/shipwreck_map",
                "minecraft:chests/shipwreck_supply",
                "minecraft:chests/shipwreck_treasure");
        requireFixedTables(
                helper,
                "stronghold",
                "minecraft:chests/stronghold_corridor",
                "minecraft:chests/stronghold_crossing",
                "minecraft:chests/stronghold_library");
        helper.succeed();
    }

    /**
     * A structure that provably places no container must resolve through the service to EXACT with
     * zero tables. Anything else would surface in game as "the machine cannot read this structure"
     * for a structure that simply has nothing to read.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void containerlessStructureResolvesToExactEmpty(GameTestHelper helper) {
        ResourceLocation structure =
                ResourceLocation.fromNamespaceAndPath("minecraft", "swamp_hut");
        CompletableFuture<DiscoveryResult> future =
                StructureAnalysisService.forServer(helper.getLevel().getServer())
                        .discover(helper.getLevel(), structure);
        helper.succeedWhen(
                () -> {
                    if (!future.isDone()) {
                        helper.fail("Discovery for swamp_hut has not completed");
                    }
                    if (future.isCompletedExceptionally()) {
                        helper.fail("Discovery for swamp_hut failed: " + unwrap(future));
                    }
                    DiscoveryResult result = future.join();
                    if (result.status() != AnalysisStatus.EXACT) {
                        helper.fail(
                                "swamp_hut returned "
                                        + result.status()
                                        + " with diagnostics "
                                        + result.diagnostics());
                    }
                    Set<String> actual = tableIds(result);
                    if (!actual.isEmpty()) {
                        helper.fail("swamp_hut resolved tables " + actual + ", expected none");
                    }
                });
    }

    private static void requireTemplateTables(
            GameTestHelper helper, String structurePath, String... expectedTables) {
        DiscoveryResult result =
                StructureLootAnalyzer.discoverTemplateForValue(
                        helper.getLevel(),
                        ResourceLocation.fromNamespaceAndPath("minecraft", structurePath),
                        AnalysisStatus.EXACT,
                        List.of());
        requireExactTables(helper, structurePath, result, expectedTables);
    }

    private static void requireFixedTables(
            GameTestHelper helper, String structurePath, String... expectedTables) {
        ResourceLocation structure =
                ResourceLocation.fromNamespaceAndPath("minecraft", structurePath);
        var resolution = VanillaStructureLootResolver.resolve(structure);
        if (resolution.isEmpty()) {
            helper.fail(structurePath + " has no fixed loot-table resolution");
        }
        DiscoveryResult result =
                StructureLootAnalyzer.discoverFixedForValue(
                        helper.getLevel(), structure, resolution.get().tables(), List.of());
        requireExactTables(helper, structurePath, result, expectedTables);
    }

    private static void requireExactTables(
            GameTestHelper helper,
            String structurePath,
            DiscoveryResult result,
            String... expectedTables) {
        if (result.status() != AnalysisStatus.EXACT) {
            helper.fail(
                    "Discovery for "
                            + structurePath
                            + " returned "
                            + result.status()
                            + " with diagnostics "
                            + result.diagnostics());
        }
        Set<String> actual = tableIds(result);
        Set<String> expected = Set.of(expectedTables);
        if (!actual.equals(expected)) {
            helper.fail(
                    "Discovery for "
                            + structurePath
                            + " resolved "
                            + actual
                            + ", expected "
                            + expected);
        }
    }

    private static Set<String> tableIds(DiscoveryResult result) {
        return result.structures().stream()
                .flatMap(value -> value.lootTables().stream())
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }

    private static Throwable unwrap(CompletableFuture<?> future) {
        try {
            future.join();
            return null;
        } catch (java.util.concurrent.CompletionException exception) {
            return exception.getCause() == null ? exception : exception.getCause();
        }
    }
}
