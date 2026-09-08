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
}
