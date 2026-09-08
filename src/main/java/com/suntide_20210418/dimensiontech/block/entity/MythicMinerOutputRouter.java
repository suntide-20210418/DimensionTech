package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.integration.ae2.Ae2Integration;
import com.suntide_20210418.dimensiontech.utils.FullDurabilityLoot;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/** Routes generated stacks to the miner's enabled adjacent outputs. */
final class MythicMinerOutputRouter {
    private static final boolean AE2_LOADED = ModList.get().isLoaded("ae2");

    private MythicMinerOutputRouter() {}

    static List<ItemStack> output(
            ServerLevel level,
            BlockPos position,
            BaseMinerBlockEntity.OutputState outputState,
            Predicate<Direction> outputFaceEnabled,
            List<ItemStack> stacks) {
        Targets targets = findTargets(level, position, outputFaceEnabled);
        List<ItemStack> remainders = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack remainder = FullDurabilityLoot.normalize(stack);
            if (outputState == BaseMinerBlockEntity.OutputState.ME_NETWORK) {
                for (BlockEntity interfaceBlock : targets.meInterfaces()) {
                    remainder =
                            Ae2Integration.insertIntoInterfaceNetwork(interfaceBlock, remainder);
                    if (remainder.isEmpty()) break;
                }
            }
            if (outputState == BaseMinerBlockEntity.OutputState.ITEM_HANDLER) {
                for (IItemHandler handler : targets.itemHandlers()) {
                    if (remainder.isEmpty()) break;
                    remainder = ItemHandlerHelper.insertItemStacked(handler, remainder, false);
                }
            }
            if (!remainder.isEmpty()) remainders.add(remainder);
        }
        return List.copyOf(remainders);
    }

    private static Targets findTargets(
            ServerLevel level, BlockPos position, Predicate<Direction> outputFaceEnabled) {
        List<BlockEntity> meInterfaces = new ArrayList<>();
        List<IItemHandler> handlers = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (!outputFaceEnabled.test(direction)) continue;
            BlockEntity adjacent = level.getBlockEntity(position.relative(direction));
            if (adjacent == null) continue;
            if (AE2_LOADED && Ae2Integration.isOnlineInterface(adjacent)) {
                meInterfaces.add(adjacent);
                continue;
            }
            adjacent.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                    .resolve()
                    .ifPresent(handlers::add);
        }
        return new Targets(List.copyOf(meInterfaces), List.copyOf(handlers));
    }

    private record Targets(List<BlockEntity> meInterfaces, List<IItemHandler> itemHandlers) {}
}
