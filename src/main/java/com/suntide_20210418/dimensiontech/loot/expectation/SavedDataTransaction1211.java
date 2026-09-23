package com.suntide_20210418.dimensiontech.loot.expectation;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.storage.DimensionDataStorage;

/** Server-thread transaction used to predict map SavedData writes and then restore them. */
final class SavedDataTransaction1211 {
    private static final String MAP_INDEX_KEY = "idcounts";
    private static final Field CACHE_FIELD = findCacheField();
    private static final ThreadLocal<ServerLevel> ACTIVE_LEVEL = new ThreadLocal<>();

    private SavedDataTransaction1211() {}

    private static Field findCacheField() {
        for (String name : new String[] {"cache", "f_78144_"}) {
            try {
                Field field = DimensionDataStorage.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Try the other obfuscated runtime naming domain.
            }
        }
        throw new IllegalStateException("Cannot find the DimensionDataStorage cache field");
    }

    static <T> T run(ServerLevel level, Supplier<T> action) {
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("SavedData transaction requires the server thread");
        }
        ServerLevel active = ACTIVE_LEVEL.get();
        if (active != null) {
            if (active != level) {
                throw new IllegalStateException("Nested SavedData transaction changed dimensions");
            }
            return action.get();
        }
        DimensionDataStorage storage = level.getDataStorage();
        synchronized (storage) {
            Map<String, SavedData> cache = cache(storage);
            Map<String, SavedData> snapshot = new HashMap<>(cache);
            SavedData mapIndex = snapshot.get(MAP_INDEX_KEY);
            // 1.21 的 MapIndex 读写都需要 HolderLookup.Provider。
            CompoundTag mapIndexTag =
                    mapIndex instanceof MapIndex
                            ? mapIndex.save(new CompoundTag(), level.registryAccess()).copy()
                            : null;
            boolean mapIndexDirty = mapIndex != null && mapIndex.isDirty();
            ACTIVE_LEVEL.set(level);
            try {
                return action.get();
            } finally {
                try {
                    cache.clear();
                    cache.putAll(snapshot);
                    if (mapIndexTag != null) {
                        MapIndex restored = MapIndex.load(mapIndexTag, level.registryAccess());
                        restored.setDirty(mapIndexDirty);
                        cache.put(MAP_INDEX_KEY, restored);
                    }
                } finally {
                    ACTIVE_LEVEL.remove();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, SavedData> cache(DimensionDataStorage storage) {
        try {
            return (Map<String, SavedData>) CACHE_FIELD.get(storage);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access DimensionDataStorage cache", exception);
        }
    }
}
