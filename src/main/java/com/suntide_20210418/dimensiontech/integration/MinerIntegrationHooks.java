package com.suntide_20210418.dimensiontech.integration;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Optional integration seam. The core miner never loads an optional scripting API. */
public final class MinerIntegrationHooks {
    public interface Hooks {
        boolean postWork(BaseMinerBlockEntity miner);

        boolean postCycle(
                BaseMinerBlockEntity miner,
                ServerLevel level,
                int slot,
                ResourceLocation marker,
                int parallel);

        OutputResult postOutput(
                BaseMinerBlockEntity miner, ServerLevel level, List<ItemStack> outputs);
    }

    public record OutputResult(boolean cancelled, List<ItemStack> outputs) {}

    private static final Hooks EMPTY =
            new Hooks() {
                @Override
                public boolean postWork(BaseMinerBlockEntity miner) {
                    return false;
                }

                @Override
                public boolean postCycle(
                        BaseMinerBlockEntity miner,
                        ServerLevel level,
                        int slot,
                        ResourceLocation marker,
                        int parallel) {
                    return false;
                }

                @Override
                public OutputResult postOutput(
                        BaseMinerBlockEntity miner, ServerLevel level, List<ItemStack> outputs) {
                    return new OutputResult(false, outputs);
                }
            };

    private static volatile Hooks hooks = EMPTY;

    private MinerIntegrationHooks() {}

    public static void install(Hooks value) {
        hooks = value == null ? EMPTY : value;
    }

    public static boolean postWork(BaseMinerBlockEntity miner) {
        return hooks.postWork(miner);
    }

    public static boolean postCycle(
            BaseMinerBlockEntity miner,
            ServerLevel level,
            int slot,
            ResourceLocation marker,
            int parallel) {
        return hooks.postCycle(miner, level, slot, marker, parallel);
    }

    public static OutputResult postOutput(
            BaseMinerBlockEntity miner, ServerLevel level, List<ItemStack> outputs) {
        return hooks.postOutput(miner, level, outputs);
    }
}
