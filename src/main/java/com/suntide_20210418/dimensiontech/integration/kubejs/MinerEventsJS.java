package com.suntide_20210418.dimensiontech.integration.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Stable event payload base reserved for server-side miner lifecycle hooks. */
public class MinerEventsJS extends EventJS {
    public MinerBlockEntityJS miner;
    public ServerLevel level;
    public BlockPos position;
    public int slot;
    public ResourceLocation marker;
    public List<ItemStack> outputs = new ArrayList<>();
    public int parallel;
    public int progress;
    public int processingTime;

    public MinerEventsJS() {}

    public MinerEventsJS(MinerBlockEntityJS miner, int slot) {
        this.miner = miner;
        this.slot = slot;
        this.position = miner.position();
        this.progress = miner.progress();
        this.processingTime = miner.processingTime();
    }

    public MinerEventsJS(
            MinerBlockEntityJS miner,
            ServerLevel level,
            int slot,
            ResourceLocation marker,
            List<ItemStack> outputs) {
        this(miner, slot);
        this.level = level;
        this.marker = marker;
        this.outputs.addAll(outputs);
        this.progress = miner.progress();
        this.processingTime = miner.processingTime();
    }
}
