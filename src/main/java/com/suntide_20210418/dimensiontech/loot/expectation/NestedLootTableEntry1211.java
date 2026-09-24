package com.suntide_20210418.dimensiontech.loot.expectation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * JSON shape of the {@code minecraft:loot_table} pool entry, as defined by Minecraft 1.21.1.
 *
 * <p>1.21 replaced the 1.20.1 {@code LootTableReference} with {@code NestedLootTable} and renamed
 * the payload field from {@code name} to {@code value}:
 *
 * <pre>{@code
 * Codec.either(ResourceKey.codec(Registries.LOOT_TABLE), LootTable.DIRECT_CODEC).fieldOf("value")
 * }</pre>
 *
 * <p>{@code value} is an {@code Either}: a table id such as {@code
 * minecraft:chests/simple_dungeon}, or a whole inline table object. Vanilla 1.21.1 data (1178
 * tables in the default pack) uses the reference form 31 times and the inline form 3 times ({@code
 * equipment/trial_chamber}), and uses {@code name} zero times — a 1.20.1-style {@code "name": ...}
 * is rejected by the vanilla loader, so a table written that way never reaches this engine. The
 * entry type id itself did not change.
 *
 * <p>An inline table is not a registry entry and therefore has no id to recurse into; the engine
 * reports it as unsupported instead of quietly treating it as an empty table.
 */
public final class NestedLootTableEntry1211 {
    /** Entry type id; unchanged between 1.20.1 and 1.21. */
    public static final String TYPE = "minecraft:loot_table";

    /** 1.21 field carrying either the table reference or an inline table. */
    public static final String FIELD = "value";

    public static final String MISSING_REFERENCE_MESSAGE =
            "Nested loot table entry needs a table id in \"value\"";

    public static final String INLINE_TABLE_MESSAGE =
            "Inline nested loot table entries are not supported";

    private NestedLootTableEntry1211() {}

    public static boolean isNestedTableEntry(JsonObject entry) {
        return TYPE.equals(stringField(entry, "type"));
    }

    /**
     * The referenced table id, or {@code null} when {@code entry} is not a nested loot table entry,
     * carries an inline table, or is malformed. Self-guarding on the entry type so call sites
     * cannot read the field from an unrelated entry.
     */
    public static ResourceLocation reference(JsonObject entry) {
        if (!isNestedTableEntry(entry)) return null;
        String raw = stringField(entry, FIELD);
        return raw == null || raw.isEmpty() ? null : ResourceLocation.tryParse(raw);
    }

    /**
     * True for a nested loot table entry that carries a whole table object instead of a reference.
     */
    public static boolean isInline(JsonObject entry) {
        if (!isNestedTableEntry(entry)) return false;
        JsonElement value = entry.get(FIELD);
        return value != null && value.isJsonObject();
    }

    private static String stringField(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : null;
    }
}
