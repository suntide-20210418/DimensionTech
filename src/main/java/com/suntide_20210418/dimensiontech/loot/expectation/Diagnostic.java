package com.suntide_20210418.dimensiontech.loot.expectation;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record Diagnostic(
        String code,
        String message,
        ResourceLocation lootTableId,
        String jsonPointer,
        List<String> callPath) {
    public Diagnostic {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
        jsonPointer = jsonPointer == null ? "" : jsonPointer;
        callPath = List.copyOf(callPath == null ? List.of() : callPath);
    }

    public Diagnostic(String code, String message) {
        this(code, message, null, "", List.of());
    }

    public static Diagnostic unsupportedType(
            ResourceLocation tableId, String pointer, List<String> callPath, String message) {
        return new Diagnostic("UNSUPPORTED_TYPE", message, tableId, pointer, callPath);
    }

    public static Diagnostic randomSemantics(
            ResourceLocation tableId, String pointer, List<String> callPath, String message) {
        return new Diagnostic("RANDOM_SEMANTICS", message, tableId, pointer, callPath);
    }
}
