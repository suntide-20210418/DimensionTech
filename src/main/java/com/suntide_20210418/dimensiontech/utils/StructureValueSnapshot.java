package com.suntide_20210418.dimensiontech.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.FrozenJson;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Value-layer data. No world, registry, Item, ItemStack, NBT or request generation is retained. */
public final class StructureValueSnapshot {
    public static final int ALGORITHM_VERSION = 1;

    private StructureValueSnapshot() {}

    public record TerminalItem(String itemId, int count, String rarity) {
        public TerminalItem {
            Objects.requireNonNull(itemId, "itemId");
            Objects.requireNonNull(rarity, "rarity");
        }
    }

    public record Expectation(Map<TerminalItem, ExactProbability> occurrences) {
        public Expectation {
            Map<TerminalItem, ExactProbability> nonzero = new LinkedHashMap<>();
            occurrences.forEach((item, mass) -> {
                Objects.requireNonNull(item, "item");
                if (!mass.isZero()) nonzero.put(item, mass);
            });
            occurrences = Map.copyOf(nonzero);
        }

        public String inputFingerprint() {
            JsonArray values = new JsonArray();
            occurrences.entrySet().stream().sorted(Map.Entry.comparingByKey(
                    Comparator.comparing(TerminalItem::itemId)
                            .thenComparingInt(TerminalItem::count)
                            .thenComparing(TerminalItem::rarity))).forEach(entry -> {
                JsonObject value = new JsonObject();
                value.addProperty("item", entry.getKey().itemId());
                value.addProperty("count", entry.getKey().count());
                value.addProperty("rarity", entry.getKey().rarity());
                value.addProperty("mass", entry.getValue().toString());
                values.add(value);
            });
            return FrozenJson.freeze(values).fingerprint();
        }
    }

    /** Missing item IDs are filtered out; each included rarity has its effective configured value. */
    public record Config(double dimensionMultiplier, Map<String, Map<String, Double>> itemValues) {
        public Config {
            Map<String, Map<String, Double>> copy = new LinkedHashMap<>();
            itemValues.forEach((id, rarities) -> copy.put(id, Map.copyOf(rarities)));
            itemValues = Map.copyOf(copy);
        }

        public String configFingerprint() {
            JsonObject config = new JsonObject();
            config.addProperty("dimensionMultiplier", Double.toHexString(dimensionMultiplier));
            JsonObject items = new JsonObject();
            itemValues.forEach((id, values) -> {
                JsonObject rarities = new JsonObject();
                values.forEach((rarity, value) -> rarities.addProperty(rarity, Double.toHexString(value)));
                items.add(id, rarities);
            });
            config.add("items", items);
            return FrozenJson.freeze(config).fingerprint();
        }
    }

    public record Result(boolean supported, double dimensionValue, double structureValue,
            Expectation expectation, Map<String, ExactProbability> itemCounts, String failure) {
        public Result {
            Objects.requireNonNull(expectation, "expectation");
            itemCounts = Map.copyOf(itemCounts);
            failure = failure == null ? "" : failure;
        }
    }

    /** Uses the same exact IEEE-754 multiplier conversion and final compression as the legacy path. */
    public static Result calculate(Expectation input, Config config) {
        double dimension = config.dimensionMultiplier();
        try {
            ExactProbability dimensionWeight = ExactProbability.fromDouble(dimension);
            ExactProbability weighted = ExactProbability.ZERO;
            Map<TerminalItem, ExactProbability> included = new LinkedHashMap<>();
            Map<String, ExactProbability> counts = new LinkedHashMap<>();
            for (var entry : input.occurrences().entrySet()) {
                TerminalItem item = entry.getKey();
                Map<String, Double> rarityValues = config.itemValues().get(item.itemId());
                if (rarityValues == null) continue;
                Double multiplier = rarityValues.get(item.rarity());
                if (multiplier == null) throw new IllegalArgumentException("Missing frozen rarity " + item.rarity());
                ExactProbability count = entry.getValue().multiply(ExactProbability.of(item.count(), 1));
                weighted = weighted.add(count.multiply(ExactProbability.fromDouble(multiplier)));
                included.put(item, entry.getValue());
                if (!count.isZero()) counts.merge(item.itemId(), count, ExactProbability::add);
            }
            double value = Math.sqrt(weighted.multiply(dimensionWeight).finiteDoubleValue()) * 50;
            return new Result(true, dimension, value, new Expectation(included), counts, "");
        } catch (ArithmeticException | IllegalArgumentException | IllegalStateException error) {
            return new Result(false, Double.isFinite(dimension) && dimension >= 0 ? dimension : 0,
                    0, new Expectation(Map.of()), Map.of(), error.getMessage());
        }
    }
}
