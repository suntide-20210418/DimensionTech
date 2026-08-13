package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequence;

/** Side-effect-free snapshot of a ServerLevel persistent random sequence. */
public final class PersistentRandomSequenceSnapshot1201 {
    private PersistentRandomSequenceSnapshot1201() {}

    public static XoroshiroState1201 snapshot(ServerLevel level, ResourceLocation id) {
        CompoundTag saved = level.getRandomSequences().save(new CompoundTag());
        Tag encoded = saved.get(id.toString());
        if (encoded == null) {
            DataResult<Tag> initial = RandomSequence.CODEC.encodeStart(
                    NbtOps.INSTANCE, new RandomSequence(level.getSeed(), id));
            encoded = initial.result().orElseThrow(() -> new IllegalStateException(
                    initial.error().map(DataResult.PartialResult::message).orElse(
                            "Could not encode initial random sequence " + id)));
        }
        return decode(encoded, id);
    }

    static XoroshiroState1201 decode(Tag encoded, ResourceLocation id) {
        if (!(encoded instanceof CompoundTag sequence)
                || !sequence.contains("source", Tag.TAG_LONG_ARRAY)) {
            throw new IllegalArgumentException("Invalid random sequence state for " + id);
        }
        long[] state = sequence.getLongArray("source");
        if (state.length != 2) {
            throw new IllegalArgumentException(
                    "Expected two Xoroshiro state longs for " + id + ", got " + state.length);
        }
        return new XoroshiroState1201(state[0], state[1]);
    }
}
