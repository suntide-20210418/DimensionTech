package com.suntide_20210418.dimensiontech.integration.jei;

import net.minecraft.network.chat.Component;

/**
 * The strings the chest-marker recipe entry on the miner JEI page adds beyond what it reuses from
 * {@link StructureMinerJeiText}: the chest-marker input slot and the chest-loot output slot. The
 * shared slot tooltips (product counts, core chance, time algorithm, "not consumed") live in the
 * structure miner's text class and are called directly from here. Both entries share the miner
 * page's title, so no title key lives here.
 */
final class ChestMinerJeiText {
    // Slot display names.
    static final String CHEST_MARKER = "jei.dimension_tech.chest_miner.chest_marker";
    static final String CHEST_LOOT = "jei.dimension_tech.chest_miner.loot";

    // Slot tooltips.
    static final String CHEST_MARKER_TIP = "jei.dimension_tech.chest_miner.chest_marker.tip";
    static final String CHEST_LOOT_TIP = "jei.dimension_tech.chest_miner.loot.tip";

    private ChestMinerJeiText() {}

    static Component chestMarkerName() {
        return Component.translatable(CHEST_MARKER);
    }

    static Component chestMarkerTip() {
        return Component.translatable(CHEST_MARKER_TIP);
    }

    static Component chestLootName() {
        return Component.translatable(CHEST_LOOT);
    }

    static Component chestLootTip() {
        return Component.translatable(CHEST_LOOT_TIP);
    }
}
