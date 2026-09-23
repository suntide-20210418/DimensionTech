package com.suntide_20210418.dimensiontech.loot.expectation;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequence;

/** Side-effect-free snapshot of a ServerLevel persistent random sequence. */
public final class PersistentRandomSequenceSnapshot1211 {
    private PersistentRandomSequenceSnapshot1211() {}

    public static XoroshiroState1211 snapshot(ServerLevel level, ResourceLocation id) {
        // 1.21 的 SavedData 序列化统一带 HolderLookup.Provider（RandomSequences#save）。
        CompoundTag saved = level.getRandomSequences().save(new CompoundTag(), level.registryAccess());
        Tag encoded = saved.get(id.toString());
        if (encoded == null) {
            DataResult<Tag> initial =
                    RandomSequence.CODEC.encodeStart(
                            NbtOps.INSTANCE, new RandomSequence(level.getSeed(), id));
            encoded =
                    initial.result()
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    initial.error()
                                                            .map(DataResult.Error::message)
                                                            .orElse(
                                                                    "Could not encode initial"
                                                                            + " random sequence "
                                                                            + id)));
        }
        return decode(encoded, id);
    }

    static XoroshiroState1211 decode(Tag encoded, ResourceLocation id) {
        if (!(encoded instanceof CompoundTag sequence)
                || !sequence.contains("source", Tag.TAG_LONG_ARRAY)) {
            throw new IllegalArgumentException("Invalid random sequence state for " + id);
        }
        long[] state = sequence.getLongArray("source");
        if (state.length != 2) {
            throw new IllegalArgumentException(
                    "Expected two Xoroshiro state longs for " + id + ", got " + state.length);
        }
        return new XoroshiroState1211(state[0], state[1]);
    }
}
