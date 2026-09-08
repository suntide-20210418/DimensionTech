package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BaseMinerArchitectureTest {
    @Test
    void lifecycleCoordinatorDoesNotDependOnBusinessImplementations() throws Exception {
        Path source = Path.of("src/main/java/com/suntide_20210418/dimensiontech/block/entity/BaseMinerBlockEntity.java");
        String text = Files.readString(source);
        for (String forbidden : new String[] {
            "LootAnalysisFingerprint", "ExpectationRewardGenerator", "MythicMinerOutputRouter",
            "MythicMinerUpgradeResolver", "MythicMinerExternalTickAcceleration", "LootTable"}) {
            assertFalse(text.contains(forbidden), "BaseMinerBlockEntity must not reference " + forbidden);
        }
    }

    @Test
    void structureQueryDoesNotScanOrMutateWorld() throws Exception {
        Path source = Path.of("src/main/java/com/suntide_20210418/dimensiontech/block/entity/BaseMinerBlockEntity.java");
        String text = Files.readString(source);
        int start = text.indexOf("public boolean isStructureComplete()");
        int end = text.indexOf("private boolean isStructureComplete(ServerLevel", start);
        String method = text.substring(start, end);
        assertFalse(method.contains("MythicMinerMultiblock"));
        assertFalse(method.contains("structureComplete ="));
    }

    @Test
    void controllerModulesRemainPackagePrivateAndOwnMutableBusinessState() throws Exception {
        Path root = Path.of("src/main/java/com/suntide_20210418/dimensiontech/block/entity");
        for (String name : new String[] {
            "MinerAnalysisController.java", "MinerOutputController.java",
            "MinerUpgradeController.java", "MinerAccelerationController.java"}) {
            String source = Files.readString(root.resolve(name));
            assertFalse(source.contains("public class"));
            assertFalse(source.contains("public final class"));
            org.junit.jupiter.api.Assertions.assertTrue(
                    source.contains("final class"), name + " must be a package-private module");
        }
        String base = Files.readString(root.resolve("BaseMinerBlockEntity.java"));
        assertFalse(base.contains("private int[] slotProcessingTimes"));
        assertFalse(base.contains("private int[] slotParallelHundredths"));
        assertFalse(base.contains("private MythicMinerExternalTickAcceleration"));
        assertFalse(base.contains("private List<ItemStack> pendingOutput"));
    }
}
