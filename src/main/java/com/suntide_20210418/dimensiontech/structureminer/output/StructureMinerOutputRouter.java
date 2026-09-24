package com.suntide_20210418.dimensiontech.structureminer.output;

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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Routes generated stacks to the miner's enabled adjacent outputs. */
public final class StructureMinerOutputRouter {
    private static final boolean AE2_LOADED = ModList.get().isLoaded("ae2");

    private StructureMinerOutputRouter() {}

    public static List<ItemStack> output(
            ServerLevel level,
            BlockPos position,
            boolean useMeNetwork,
            Predicate<Direction> outputFaceEnabled,
            List<ItemStack> stacks) {
        Targets targets = findTargets(level, position, outputFaceEnabled);
        List<ItemStack> remainders = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack remainder = FullDurabilityLoot.normalize(stack);
            if (useMeNetwork) {
                for (BlockEntity interfaceBlock : targets.meInterfaces()) {
                    remainder =
                            Ae2Integration.insertIntoInterfaceNetwork(interfaceBlock, remainder);
                    if (remainder.isEmpty()) break;
                }
            }
            if (!useMeNetwork) {
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
            IItemHandler handler =
                    level.getCapability(
                            Capabilities.ItemHandler.BLOCK,
                            position.relative(direction),
                            direction.getOpposite());
            if (handler != null) handlers.add(handler);
        }
        return new Targets(List.copyOf(meInterfaces), List.copyOf(handlers));
    }

    private record Targets(List<BlockEntity> meInterfaces, List<IItemHandler> itemHandlers) {}
}
