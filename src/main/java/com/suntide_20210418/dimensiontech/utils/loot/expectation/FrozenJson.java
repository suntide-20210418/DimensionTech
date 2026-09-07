package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** A JSON value with no mutable JSON nodes or caller-owned collections across thread boundaries. */
public sealed interface FrozenJson {
    JsonElement toJson();

    default String fingerprint() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(toJson().toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    static FrozenJson freeze(JsonElement json) {
        Objects.requireNonNull(json, "json");
        if (json.isJsonObject()) {
            Map<String, FrozenJson> fields = new TreeMap<>();
            json.getAsJsonObject().entrySet().forEach(entry ->
                    fields.put(entry.getKey(), freeze(entry.getValue())));
            return new ObjectValue(fields);
        }
        if (json.isJsonArray()) {
            java.util.ArrayList<FrozenJson> elements = new java.util.ArrayList<>();
            json.getAsJsonArray().forEach(element -> elements.add(freeze(element)));
            return new ArrayValue(elements);
        }
        return new ScalarValue(json.toString());
    }

    record ObjectValue(Map<String, FrozenJson> fields) implements FrozenJson {
        public ObjectValue { fields = Map.copyOf(fields); }

        @Override
        public JsonObject toJson() {
            JsonObject object = new JsonObject();
            new TreeMap<>(fields).forEach((key, value) -> object.add(key, value.toJson()));
            return object;
        }
    }

    record ArrayValue(List<FrozenJson> elements) implements FrozenJson {
        public ArrayValue { elements = List.copyOf(elements); }

        @Override
        public JsonArray toJson() {
            JsonArray array = new JsonArray();
            elements.forEach(element -> array.add(element.toJson()));
            return array;
        }
    }

    /** Retains the exact serialized number spelling; numbers are never rounded through double. */
    record ScalarValue(String json) implements FrozenJson {
        public ScalarValue {
            JsonElement value = JsonParser.parseString(Objects.requireNonNull(json, "json"));
            if (!value.isJsonPrimitive() && !value.isJsonNull()) {
                throw new IllegalArgumentException("Expected a JSON scalar");
            }
            json = value.toString();
        }

        @Override
        public JsonElement toJson() { return JsonParser.parseString(json); }
    }
}
