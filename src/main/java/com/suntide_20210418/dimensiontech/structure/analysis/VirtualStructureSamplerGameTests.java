package com.suntide_20210418.dimensiontech.structure.analysis;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime proof that generated containers are observed without loading a real structure chunk. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VirtualStructureSamplerGameTests {
    private VirtualStructureSamplerGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void endCityContainerLootIsReadFromDetachedChunks(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        if (end == null) {
            helper.fail("The End is not available to virtual structure analysis");
            return;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", "end_city");
        Structure structure = end.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        if (structure == null) {
            helper.fail("Missing vanilla end_city structure");
            return;
        }
        for (int sample = 0; sample < 64; sample++) {
            BlockPos origin = StructureAnalysisService.sampleOrigin(end, id, sample);
            Map<ResourceLocation, Integer> tables =
                    VirtualStructureSampler.sample(end, structure, origin).lootTables();
            if (!tables.isEmpty()) {
                helper.succeed();
                return;
            }
        }
        helper.fail("No End City container LootTable was discovered in 64 detached samples");
    }
}
