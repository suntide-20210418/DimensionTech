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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
                    sampleFully(end, structure, origin).lootTables();
            if (!tables.isEmpty()) {
                helper.succeed();
                return;
            }
        }
        helper.fail("No End City container LootTable was discovered in 64 detached samples");
    }

    /** 采样在正式路径上按时间片让出，测试里不等 tick：把时间片开到无限，一次推到底（仍然受采样预算 约束）。 */
    private static VirtualStructureSampler.Sample sampleFully(
            ServerLevel level, Structure structure, BlockPos origin) {
        VirtualStructureSampler.Session session = begin(level, structure, origin);
        while (!session.advance(Long.MAX_VALUE)) {
            // 时间片无限时不会走到这里；留作防御，避免测试静默卡死。
        }
        return session.result();
    }

    private static VirtualStructureSampler.Session begin(
            ServerLevel level, Structure structure, BlockPos origin) {
        return VirtualStructureSampler.begin(
                level, structure, origin, new VirtualStructureSampler.SamplingControl(60_000));
    }

    /** 采样必须能按时间片让出、能被取消，而且分片推进得到的结果与一次推到底完全一致。 */
    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void samplingYieldsResumesAndCancels(GameTestHelper helper) {
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

        // 时间片已过期时一个 chunk 都不放；续上时间片后结果与一次推到底一致。
        for (int sample = 0; sample < 64; sample++) {
            BlockPos origin = StructureAnalysisService.sampleOrigin(end, id, sample);
            VirtualStructureSampler.Session session = begin(end, structure, origin);
            if (session.advance(System.nanoTime())) {
                // 候选点生成不了，result 是 invalid；换下一个候选。
                continue;
            }

            // 中断：标志置位后下一次推进立刻中止，并带上取消诊断键。
            VirtualStructureSampler.Session interrupted = begin(end, structure, origin);
            interrupted.interrupt();
            try {
                interrupted.advance(Long.MAX_VALUE);
                helper.fail("An interrupted sample must not run to completion");
                return;
            } catch (VirtualStructureSampler.SampleAbortedException aborted) {
                if (!"VIRTUAL_SAMPLE_CANCELLED".equals(aborted.code())) {
                    helper.fail("Unexpected abort code: " + aborted.code());
                    return;
                }
            }

            while (!session.advance(Long.MAX_VALUE)) {
                // 时间片无限时不会走到这里。
            }
            Map<ResourceLocation, Integer> sliced = session.result().lootTables();
            Map<ResourceLocation, Integer> whole = sampleFully(end, structure, origin).lootTables();
            if (!whole.equals(sliced)) {
                helper.fail("Sliced sampling diverged from a single pass at " + origin);
                return;
            }
            if (!sliced.isEmpty()) {
                helper.succeed();
                return;
            }
        }
        helper.fail("No generatable End City candidate produced container loot in 64 samples");
    }
}
