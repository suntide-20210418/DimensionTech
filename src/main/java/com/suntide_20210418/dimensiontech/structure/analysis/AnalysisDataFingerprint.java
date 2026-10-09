package com.suntide_20210418.dimensiontech.structure.analysis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.suntide_20210418.dimensiontech.loot.expectation.FrozenJson;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;

/**
 * Session-stable fingerprint of the data sources an analysis result was derived from.
 *
 * <p>A persisted marker analysis is reused only while this fingerprint is unchanged, so adding,
 * removing or updating a mod or data pack invalidates it. Without this, a world reload after
 * installing a data pack that rewrites loot tables would silently reuse a stale analysis, because
 * {@code LootAnalysisFingerprint} only covers the marker, luck and the structure-value config.
 *
 * <p>It is memoized per server: the pack list and mod list cannot change while a server runs, and
 * computing it walks every loaded pack once.
 *
 * <p>Known limitation: it hashes each pack's identity and known-pack version, not the bytes inside
 * it. Editing a file in an already-present world data pack keeps the same pack id and therefore
 * does not invalidate persisted analyses; such an edit must ship as a new or renamed pack instead.
 */
public final class AnalysisDataFingerprint {
    private static final Map<MinecraftServer, String> CACHE = new WeakHashMap<>();

    private AnalysisDataFingerprint() {}

    public static synchronized String current(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, AnalysisDataFingerprint::compute);
    }

    private static String compute(MinecraftServer server) {
        JsonObject root = new JsonObject();

        JsonArray mods = new JsonArray();
        ModList.get().getMods().stream()
                .map(info -> info.getModId() + "@" + info.getVersion())
                .sorted()
                .forEach(mods::add);
        root.add("mods", mods);

        JsonArray packs = new JsonArray();
        server.getResourceManager()
                .listPacks()
                .map(
                        pack ->
                                pack.packId()
                                        + ":"
                                        + pack.knownPackInfo().map(Object::toString).orElse(""))
                .sorted()
                .forEach(packs::add);
        root.add("packs", packs);

        return FrozenJson.freeze(root).fingerprint();
    }
}
