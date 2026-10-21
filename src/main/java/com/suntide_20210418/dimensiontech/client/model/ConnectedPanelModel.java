package com.suntide_20210418.dimensiontech.client.model;

import com.suntide_20210418.dimensiontech.block.AssemblyPanelBlock;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.StructureMinerStructureBlock;
import com.suntide_20210418.dimensiontech.block.StructureMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.block.StructureMinerVisuals;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.List;

import javax.annotation.Nullable;

/**
 * One cached quad per exposed shell/glass face, two for a functional upgrade. Neighbour reads occur
 * only in getModelData during chunk compilation.
 */
final class ConnectedPanelModel extends BakedModelWrapper<BakedModel> {
    enum Kind {
        CASING,
        GLASS,
        STRUCTURE,
        MODULE
    }

    private static final ModelProperty<Long> MASKS = new ModelProperty<>();
    // Texture-space up/right, matched to vanilla cube UV orientation.
    private static final Direction[] UP = {
        Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.UP, Direction.UP, Direction.UP
    };
    private static final Direction[] RIGHT = {
        Direction.EAST,
        Direction.EAST,
        Direction.WEST,
        Direction.EAST,
        Direction.SOUTH,
        Direction.NORTH
    };
    private final Kind kind;
    private final PanelMaterialBank bank;
    private final List<BakedQuad>[][] connected;
    private final List<BakedQuad>[][] assembled;
    private final RenderType pass;
    private final ChunkRenderTypeSet passes;

    @SuppressWarnings("unchecked")
    ConnectedPanelModel(
            BakedModel original, Kind kind, String module, int tier, PanelMaterialBank bank) {
        super(original);
        this.kind = kind;
        this.bank = bank;
        this.pass =
                kind == Kind.GLASS || kind == Kind.MODULE
                        ? RenderType.cutout()
                        : RenderType.solid();
        this.passes = ChunkRenderTypeSet.of(pass);
        connected = new List[6][256];
        assembled = new List[67][6];
        String material =
                kind == Kind.CASING ? "casing" : kind == Kind.GLASS ? "glass" : "structure";
        for (Direction side : Direction.values()) {
            for (int mask : ConnectionMask.validMasks()) {
                List<BakedQuad> base = bank.face(material + "_" + mask, side);
                connected[side.ordinal()][mask] =
                        kind == Kind.MODULE
                                ? List.of(
                                        base.get(0),
                                        bank.face("module_" + module + "_" + tier, side).get(0))
                                : base;
            }
            for (int mask = 0; mask < 256; mask++)
                connected[side.ordinal()][mask] =
                        connected[side.ordinal()][ConnectionMask.normalize(mask)];
        }
        if (kind != Kind.GLASS) {
            for (int slot = 1; slot <= 66; slot++) {
                for (Direction face : Direction.values())
                    assembled[slot][face.ordinal()] = assembledFace(slot, face);
            }
        }
    }

    private boolean sameGroup(BlockState state) {
        return switch (kind) {
            case CASING -> state.is(ModBlocks.STRUCTURE_MINER_CASING.get());
            case GLASS -> state.is(ModBlocks.STRUCTURE_MINER_GLASS.get());
            case STRUCTURE, MODULE ->
                    state.getBlock() instanceof StructureMinerStructureBlock
                            || state.getBlock() instanceof StructureMinerUpgradeBlock;
        };
    }

    private boolean connects(BlockAndTintGetter world, BlockPos pos, Direction face) {
        BlockState adjacent = world.getBlockState(pos);
        return sameGroup(adjacent)
                && Block.shouldRenderFace(adjacent, world, pos, face, pos.relative(face));
    }

    @Override
    public ModelData getModelData(
            BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData data) {
        ModelData inherited = originalModel.getModelData(world, pos, state, data);
        long packed = 0;
        for (Direction face : Direction.values()) {
            if (!Block.shouldRenderFace(state, world, pos, face, pos.relative(face))) continue;
            Direction up = UP[face.ordinal()], right = RIGHT[face.ordinal()];
            Direction[] edges = {up, right, up.getOpposite(), right.getOpposite()};
            int mask = 0;
            for (int i = 0; i < 4; i++) {
                if (connects(world, pos.relative(edges[i]), face)) mask |= 1 << i;
            }
            for (int i = 0; i < 4; i++) {
                int pair = (1 << i) | (1 << ((i + 1) & 3));
                if ((mask & pair) == pair
                        && connects(
                                world, pos.relative(edges[i]).relative(edges[(i + 1) & 3]), face))
                    mask |= 16 << i;
            }
            packed |= (long) mask << (face.ordinal() * 8);
        }
        return inherited.derive().with(MASKS, packed).build();
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource random,
            ModelData data,
            @Nullable RenderType renderType) {
        if (state == null) return originalModel.getQuads(null, side, random, data, renderType);
        if (side == null || renderType != null && renderType != pass) return List.of();
        int slot =
                state.hasProperty(AssemblyPanelBlock.ASSEMBLY_SLOT)
                        ? state.getValue(AssemblyPanelBlock.ASSEMBLY_SLOT)
                        : 0;
        if (slot != 0 && assembled[slot][side.ordinal()] != null)
            return assembled[slot][side.ordinal()];
        Long packed = data.get(MASKS);
        int mask = packed == null ? 0 : (int) (packed >>> (side.ordinal() * 8)) & 255;
        return connected[side.ordinal()][mask];
    }

    private List<BakedQuad> assembledFace(int slot, Direction side) {
        BlockPos offset = StructureMinerVisuals.offset(slot);
        int row = -offset.getY(), x = offset.getX(), z = offset.getZ();
        if ((row == 1 || row == 5) && side == Direction.UP)
            return bank.face("deck_" + (x + 2) + "_" + (z + 2), side);
        if (row == 1 || row == 5) {
            int strip =
                    switch (side) {
                        case NORTH -> z == -2 ? 2 - x : -1;
                        case SOUTH -> z == 2 ? x + 2 : -1;
                        case EAST -> x == 2 ? 2 - z : -1;
                        case WEST -> x == -2 ? z + 2 : -1;
                        default -> -1;
                    };
            if (strip >= 0) return bank.face((row == 1 ? "cap_top_" : "cap_bottom_") + strip, side);
        }
        if ((kind == Kind.STRUCTURE || kind == Kind.MODULE)
                && row >= 2
                && row <= 4
                && side.getAxis().isHorizontal()) {
            return bank.face("spine_" + (row - 2), side);
        }
        return null;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(
            BlockState state, RandomSource random, ModelData data) {
        return passes;
    }
}
