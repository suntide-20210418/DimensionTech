package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Missing and recursive reference behavior verified against Minecraft 1.20.1. */
public final class ReferenceSemantics1201 {
    private ReferenceSemantics1201() {}

    public static Diagnostic missingTable(ResourceLocation id) {
        return warning("TABLE", "MISSING_REFERENCE", id, "EMPTY table output");
    }

    public static Diagnostic recursiveTable(ResourceLocation id) {
        return warning("TABLE", "RECURSIVE_REFERENCE", id, "EMPTY table output");
    }

    public static Diagnostic missingPredicate(ResourceLocation id) {
        return warning("PREDICATE", "MISSING_REFERENCE", id, "false");
    }

    public static Diagnostic recursivePredicate(ResourceLocation id) {
        return warning("PREDICATE", "RECURSIVE_REFERENCE", id, "false");
    }

    public static Diagnostic missingFunction(ResourceLocation id) {
        return warning("FUNCTION", "MISSING_REFERENCE", id, "identity");
    }

    public static Diagnostic recursiveFunction(ResourceLocation id) {
        return warning("FUNCTION", "RECURSIVE_REFERENCE", id, "identity");
    }

    public static LootExpectationResult emptyTable(Diagnostic diagnostic) {
        return new LootExpectationResult(
                AnalysisStatus.EXACT, new StackMeasure(), List.of(diagnostic));
    }

    private static Diagnostic warning(
            String kind, String code, ResourceLocation id, String behavior) {
        return new Diagnostic(code, kind + " " + id + " resolved as " + behavior + " (1.20.1)");
    }
}
