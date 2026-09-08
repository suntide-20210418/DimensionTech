package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.suntide_20210418.dimensiontech.loot.expectation.FrozenJson;
import org.junit.jupiter.api.Test;

class FrozenJsonTest {
    @Test
    void mutationsOfOriginalAndReturnedJsonCannotChangeSnapshot() {
        var original = JsonParser.parseString("{\"pools\":[{\"rolls\":3}]}").getAsJsonObject();
        FrozenJson frozen = FrozenJson.freeze(original);
        String fingerprint = frozen.fingerprint();
        original.getAsJsonArray("pools").get(0).getAsJsonObject().addProperty("rolls", 99);
        frozen.toJson().getAsJsonObject().getAsJsonArray("pools").remove(0);
        assertEquals(fingerprint, frozen.fingerprint());
        assertEquals(3, frozen.toJson().getAsJsonObject().getAsJsonArray("pools")
                .get(0).getAsJsonObject().get("rolls").getAsInt());
    }

    @Test
    void nestedConstructorsCopyTheirCollectionsAndExposeNoMutableData() {
        var elements = new ArrayList<FrozenJson>(List.of(new FrozenJson.ScalarValue("1")));
        var array = new FrozenJson.ArrayValue(elements);
        var fields = new HashMap<String, FrozenJson>();
        fields.put("values", array);
        var object = new FrozenJson.ObjectValue(fields);
        elements.clear();
        fields.clear();
        assertEquals("{\"values\":[1]}", object.toJson().toString());
        assertThrows(UnsupportedOperationException.class, () -> array.elements().clear());
        assertThrows(UnsupportedOperationException.class, () -> object.fields().clear());
    }

    @Test
    void contentFingerprintIgnoresObjectOrderButPreservesArrayOrderAndExactNumbers() {
        FrozenJson first = FrozenJson.freeze(JsonParser.parseString("{\"a\":1,\"b\":[2,3]}"));
        FrozenJson reordered = FrozenJson.freeze(JsonParser.parseString("{\"b\":[2,3],\"a\":1}"));
        assertEquals(first.fingerprint(), reordered.fingerprint());
        assertNotEquals(first.fingerprint(), FrozenJson.freeze(
                JsonParser.parseString("{\"a\":1,\"b\":[3,2]}")).fingerprint());
        assertNotEquals(new FrozenJson.ScalarValue("9007199254740992").fingerprint(),
                new FrozenJson.ScalarValue("9007199254740993").fingerprint());
    }
}
