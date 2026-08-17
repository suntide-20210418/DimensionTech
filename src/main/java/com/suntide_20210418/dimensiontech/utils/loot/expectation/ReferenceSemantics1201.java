package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Missing and recursive reference behavior verified against Minecraft 1.20.1. */
public final class ReferenceSemantics1201 {
    private ReferenceSemantics1201() {}

    public static Diagnostic missingTable(ResourceLocation id) {
        return warning("TABLE", "MISSING_REFERENCE", id, "EMPTY table output", id, "", List.of(id.toString()));
    }

    public static Diagnostic recursiveTable(ResourceLocation id) {
        return warning("TABLE", "RECURSIVE_REFERENCE", id, "EMPTY table output", id, "", List.of(id.toString()));
    }

    public static Diagnostic missingPredicate(ResourceLocation id) {
        return warning("PREDICATE", "MISSING_REFERENCE", id, "false", id, "", List.of(id.toString()));
    }

    public static Diagnostic recursivePredicate(ResourceLocation id) {
        return warning("PREDICATE", "RECURSIVE_REFERENCE", id, "false", id, "", List.of(id.toString()));
    }

    public static Diagnostic missingFunction(ResourceLocation id) {
        return warning("FUNCTION", "MISSING_REFERENCE", id, "identity", id, "", List.of(id.toString()));
    }

    public static Diagnostic recursiveFunction(ResourceLocation id) {
        return warning("FUNCTION", "RECURSIVE_REFERENCE", id, "identity", id, "", List.of(id.toString()));
    }

    public static Diagnostic missingTable(
            ResourceLocation referencedId,
            ResourceLocation ownerTableId,
            String jsonPointer,
            List<String> callPath) {
        return warning(
                "TABLE",
                "MISSING_REFERENCE",
                referencedId,
                "EMPTY table output",
                ownerTableId,
                jsonPointer,
                callPath);
    }

    public static Diagnostic recursiveTable(
            ResourceLocation referencedId,
            ResourceLocation ownerTableId,
            String jsonPointer,
            List<String> callPath) {
        return warning(
                "TABLE",
                "RECURSIVE_REFERENCE",
                referencedId,
                "EMPTY table output",
                ownerTableId,
                jsonPointer,
                callPath);
    }

    public static Diagnostic missingPredicate(
            ResourceLocation referencedId,
            ResourceLocation ownerTableId,
            String jsonPointer,
            List<String> callPath) {
        return warning(
                "PREDICATE",
                "MISSING_REFERENCE",
                referencedId,
                "false",
                ownerTableId,
                jsonPointer,
                callPath);
    }

    public static Diagnostic recursivePredicate(
            ResourceLocation referencedId,
            ResourceLocation ownerTableId,
            String jsonPointer,
            List<String> callPath) {
        return warning(
                "PREDICATE",
                "RECURSIVE_REFERENCE",
                referencedId,
                "false",
                ownerTableId,
                jsonPointer,
                callPath);
    }

    public static Diagnostic missingFunction(
            ResourceLocation referencedId,
            ResourceLocation ownerTableId,
            String jsonPointer,
            List<String> callPath) {
        return warning(
                "FUNCTION",
                "MISSING_REFERENCE",
                referencedId,
                "identity",
                ownerTableId,
                jsonPointer,
                callPath);
    }

    public static Diagnostic recursiveFunction(
            ResourceLocation referencedId,
            ResourceLocation ownerTableId,
            String jsonPointer,
            List<String> callPath) {
        return warning(
                "FUNCTION",
                "RECURSIVE_REFERENCE",
                referencedId,
                "identity",
                ownerTableId,
                jsonPointer,
                callPath);
    }

    public static LootExpectationResult emptyTable(Diagnostic diagnostic) {
        return new LootExpectationResult(
                AnalysisStatus.EXACT, new StackMeasure(), List.of(diagnostic));
    }

    private static Diagnostic warning(
            String kind,
            String code,
            ResourceLocation referencedId,
            String behavior,
            ResourceLocation ownerTableId,
            String jsonPointer,
            List<String> callPath) {
        return new Diagnostic(
                code,
                kind + " " + referencedId + " resolved as " + behavior + " (1.20.1)",
                ownerTableId,
                jsonPointer,
                callPath);
    }
}
