package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

/**
 * Formation changes are finite blockstate updates, never a rendering tick loop. Stable slot ids
 * also let a loaded orphan recover without loading its controller chunk.
 */
@Mod.EventBusSubscriber(modid = DimensionTechMod.MOD_ID)
public final class StructureMinerVisuals {
    private StructureMinerVisuals() {}

    private static final class Pattern {
        static final List<StructureMinerMultiblock.ProjectionBlock> BLOCKS =
                StructureMinerMultiblock.projection().stream()
                        .sorted(
                                Comparator.comparingInt(
                                                (StructureMinerMultiblock.ProjectionBlock b) ->
                                                        b.offset().getY())
                                        .thenComparingInt(b -> b.offset().getX())
                                        .thenComparingInt(b -> b.offset().getZ()))
                        .toList();
    }

    public static BlockPos offset(int slot) {
        if (slot < 1 || slot > Pattern.BLOCKS.size())
            throw new IllegalArgumentException("Assembly slot: " + slot);
        return Pattern.BLOCKS.get(slot - 1).offset();
    }

    public static int row(int slot) {
        return slot == 0 ? 0 : -offset(slot).getY();
    }

    public static void markNearbyControllers(Level level, BlockPos changed) {
        if (!(level instanceof ServerLevel server)) return;
        for (var part : Pattern.BLOCKS) {
            BlockPos owner = changed.subtract(part.offset());
            if (server.hasChunkAt(owner)
                    && server.getBlockEntity(owner) instanceof BaseMinerBlockEntity miner) {
                miner.requestVisualRefresh();
            }
        }
    }

    /** Returns false when observation is incomplete; no chunk is force-loaded. */
    public static boolean refresh(ServerLevel level, BlockPos owner, boolean formed) {
        for (var part : Pattern.BLOCKS) {
            if (!level.hasChunkAt(owner.offset(part.offset()))) return false;
        }
        BlockState head = level.getBlockState(owner);
        if (head.getBlock() instanceof BaseMinerBlock
                && head.getValue(BaseMinerBlock.ASSEMBLED) != formed) {
            level.setBlock(
                    owner, head.setValue(BaseMinerBlock.ASSEMBLED, formed), Block.UPDATE_CLIENTS);
        }
        for (int i = 0; i < Pattern.BLOCKS.size(); i++) {
            var projected = Pattern.BLOCKS.get(i);
            BlockPos pos = owner.offset(projected.offset());
            BlockState state = level.getBlockState(pos);
            if (!state.hasProperty(AssemblyPanelBlock.ASSEMBLY_SLOT)) continue;
            int current = state.getValue(AssemblyPanelBlock.ASSEMBLY_SLOT);
            int desired = formed && StructureMinerMultiblock.isFilled(projected, state) ? i + 1 : 0;
            if (desired == 0 && current != 0 && !pos.subtract(offset(current)).equals(owner))
                continue;
            if (current != desired)
                level.setBlock(
                        pos,
                        state.setValue(AssemblyPanelBlock.ASSEMBLY_SLOT, desired),
                        Block.UPDATE_CLIENTS);
        }
        return true;
    }

    public static void clearLoaded(ServerLevel level, BlockPos owner) {
        for (int i = 0; i < Pattern.BLOCKS.size(); i++) {
            BlockPos pos = owner.offset(Pattern.BLOCKS.get(i).offset());
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(AssemblyPanelBlock.ASSEMBLY_SLOT)
                    && state.getValue(AssemblyPanelBlock.ASSEMBLY_SLOT) == i + 1) {
                level.setBlock(
                        pos,
                        state.setValue(AssemblyPanelBlock.ASSEMBLY_SLOT, 0),
                        Block.UPDATE_CLIENTS);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) return;
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        // Run after chunk installation. Only already-loaded chunks are inspected.
        // A palette predicate avoids scanning ordinary world sections entirely.
        level.getServer()
                .execute(
                        () -> {
                            for (int x = cx - 1; x <= cx + 1; x++) {
                                for (int z = cz - 1; z <= cz + 1; z++) {
                                    LevelChunk loaded = level.getChunkSource().getChunkNow(x, z);
                                    if (loaded != null) repairOrphans(level, loaded);
                                }
                            }
                        });
    }

    private static void repairOrphans(ServerLevel level, LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (!section.getStates()
                    .maybeHas(
                            s ->
                                    s.hasProperty(AssemblyPanelBlock.ASSEMBLY_SLOT)
                                            && s.getValue(AssemblyPanelBlock.ASSEMBLY_SLOT) != 0))
                continue;
            int baseY = chunk.getMinBuildHeight() + sectionIndex * 16;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!state.hasProperty(AssemblyPanelBlock.ASSEMBLY_SLOT)) continue;
                        int slot = state.getValue(AssemblyPanelBlock.ASSEMBLY_SLOT);
                        if (slot == 0) continue;
                        pos.set(
                                chunk.getPos().getMinBlockX() + x,
                                baseY + y,
                                chunk.getPos().getMinBlockZ() + z);
                        BlockPos owner = pos.subtract(offset(slot));
                        if (!level.hasChunkAt(owner)) continue;
                        BlockState head = level.getBlockState(owner);
                        if (!(head.getBlock() instanceof BaseMinerBlock)
                                || !head.getValue(BaseMinerBlock.ASSEMBLED)) {
                            level.setBlock(
                                    pos,
                                    state.setValue(AssemblyPanelBlock.ASSEMBLY_SLOT, 0),
                                    Block.UPDATE_CLIENTS);
                        } else if (level.getBlockEntity(owner)
                                instanceof BaseMinerBlockEntity miner) {
                            miner.requestVisualRefresh();
                        }
                    }
        }
    }
}
