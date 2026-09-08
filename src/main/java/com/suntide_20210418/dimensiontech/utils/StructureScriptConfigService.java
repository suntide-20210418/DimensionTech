package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.loot.expectation.FrozenJson;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;

/** Server-script overrides for structure analysis and value calculation. */
public final class StructureScriptConfigService {
    private static final Map<ResourceLocation, Double> DIMENSION_VALUES = new ConcurrentHashMap<>();
    private static final Map<String, Double> ITEM_MULTIPLIERS = new ConcurrentHashMap<>();
    private static volatile List<String> dimensionWhitelist = List.of();
    private static volatile List<String> dimensionBlacklist = List.of();
    private static volatile List<String> structureWhitelist = List.of();
    private static volatile List<String> structureBlacklist = List.of();
    private static volatile List<String> itemWhitelist = List.of();
    private static volatile List<String> itemBlacklist = List.of();
    private static volatile Double commonMultiplier,
            uncommonMultiplier,
            rareMultiplier,
            epicMultiplier;
    private static volatile String expectationMethod;
    private static volatile Integer samplingCount, virtualSamples, stepsPerTick;

    private StructureScriptConfigService() {}

    public static synchronized void clear() {
        DIMENSION_VALUES.clear();
        ITEM_MULTIPLIERS.clear();
        dimensionWhitelist =
                dimensionBlacklist =
                        structureWhitelist =
                                structureBlacklist = itemWhitelist = itemBlacklist = List.of();
        commonMultiplier = uncommonMultiplier = rareMultiplier = epicMultiplier = null;
        expectationMethod = null;
        samplingCount = virtualSamples = stepsPerTick = null;
    }

    public static void dimensionValue(ResourceLocation id, double value) {
        DIMENSION_VALUES.put(id, value);
    }

    public static void itemMultiplier(String regex, double value) {
        Pattern.compile(regex);
        ITEM_MULTIPLIERS.put(regex, value);
    }

    public static void whitelist(String kind, List<String> values) {
        setList(kind, values, true);
    }

    public static void blacklist(String kind, List<String> values) {
        setList(kind, values, false);
    }

    private static void setList(String kind, List<String> values, boolean white) {
        List<String> copy = List.copyOf(values);
        copy.forEach(value -> Pattern.compile(value));
        switch (kind) {
            case "dimension" -> {
                if (white) dimensionWhitelist = copy;
                else dimensionBlacklist = copy;
            }
            case "structure" -> {
                if (white) structureWhitelist = copy;
                else structureBlacklist = copy;
            }
            case "item" -> {
                if (white) itemWhitelist = copy;
                else itemBlacklist = copy;
            }
            default -> throw new IllegalArgumentException("Unknown filter kind: " + kind);
        }
    }

    public static void rarity(String rarity, double value) {
        switch (rarity.toLowerCase(java.util.Locale.ROOT)) {
            case "common" -> commonMultiplier = value;
            case "uncommon" -> uncommonMultiplier = value;
            case "rare" -> rareMultiplier = value;
            case "epic" -> epicMultiplier = value;
            default -> throw new IllegalArgumentException("Unknown rarity: " + rarity);
        }
    }

    public static void expectationMethod(String value) {
        expectationMethod = value.toUpperCase(java.util.Locale.ROOT);
    }

    public static void samplingCount(int value) {
        samplingCount = value;
    }

    public static void virtualSamples(int value) {
        virtualSamples = value;
    }

    public static void stepsPerTick(int value) {
        stepsPerTick = value;
    }

    public static Double dimensionValue(ResourceLocation id) {
        return DIMENSION_VALUES.get(id);
    }

    public static Double itemMultiplier(String id) {
        for (var e : ITEM_MULTIPLIERS.entrySet())
            if (Pattern.compile(e.getKey()).matcher(id).find()) return e.getValue();
        return null;
    }

    public static List<String> filters(String kind, boolean white) {
        return switch (kind) {
            case "dimension" -> white ? dimensionWhitelist : dimensionBlacklist;
            case "structure" -> white ? structureWhitelist : structureBlacklist;
            case "item" -> white ? itemWhitelist : itemBlacklist;
            default -> List.of();
        };
    }

    public static Double rarityMultiplier(String rarity) {
        return switch (rarity) {
            case "COMMON" -> commonMultiplier;
            case "UNCOMMON" -> uncommonMultiplier;
            case "RARE" -> rareMultiplier;
            case "EPIC" -> epicMultiplier;
            default -> null;
        };
    }

    public static String expectationMethod() {
        return expectationMethod;
    }

    public static Integer samplingCount() {
        return samplingCount;
    }

    public static Integer virtualSamples() {
        return virtualSamples;
    }

    public static Integer stepsPerTick() {
        return stepsPerTick;
    }

    public static String fingerprint() {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        com.google.gson.JsonObject dimensions = new com.google.gson.JsonObject();
        DIMENSION_VALUES.forEach(
                (id, value) -> dimensions.addProperty(id.toString(), Double.toHexString(value)));
        root.add("dimensions", dimensions);
        com.google.gson.JsonObject items = new com.google.gson.JsonObject();
        ITEM_MULTIPLIERS.forEach(
                (regex, value) -> items.addProperty(regex, Double.toHexString(value)));
        root.add("items", items);
        addList(root, "dimensionWhitelist", dimensionWhitelist);
        addList(root, "dimensionBlacklist", dimensionBlacklist);
        addList(root, "structureWhitelist", structureWhitelist);
        addList(root, "structureBlacklist", structureBlacklist);
        addList(root, "itemWhitelist", itemWhitelist);
        addList(root, "itemBlacklist", itemBlacklist);
        addNullable(root, "expectationMethod", expectationMethod);
        addNullable(root, "samplingCount", samplingCount);
        addNullable(root, "virtualSamples", virtualSamples);
        addNullable(root, "stepsPerTick", stepsPerTick);
        addNullable(root, "commonMultiplier", commonMultiplier);
        addNullable(root, "uncommonMultiplier", uncommonMultiplier);
        addNullable(root, "rareMultiplier", rareMultiplier);
        addNullable(root, "epicMultiplier", epicMultiplier);
        return FrozenJson.freeze(root).fingerprint();
    }

    private static void addList(com.google.gson.JsonObject root, String name, List<String> values) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        values.forEach(array::add);
        root.add(name, array);
    }

    private static void addNullable(com.google.gson.JsonObject root, String name, Object value) {
        if (value == null) root.add(name, com.google.gson.JsonNull.INSTANCE);
        else if (value instanceof Number number) root.addProperty(name, number);
        else root.addProperty(name, value.toString());
    }
}
