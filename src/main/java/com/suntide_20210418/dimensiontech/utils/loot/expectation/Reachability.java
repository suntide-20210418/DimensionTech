package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.List;
import java.util.Objects;

/** Shared lazy reachability rule: zero mass is the only reason not to interpret a node. */
public final class Reachability {
    private Reachability() {}

    public static <T> void visitPositiveMass(
            FiniteDistribution<T> distribution, PositiveMassVisitor<T> visitor) {
        Objects.requireNonNull(distribution, "distribution");
        Objects.requireNonNull(visitor, "visitor");
        distribution
                .masses()
                .forEach(
                        (state, mass) -> {
                            if (!mass.isZero()) visitor.visit(state, mass);
                        });
    }

    public static UnsupportedMechanism unsupported(String type, ExactProbability inboundMass) {
        if (inboundMass.isZero()) {
            throw new IllegalArgumentException("zero-mass nodes must not be interpreted");
        }
        return new UnsupportedMechanism(type, inboundMass);
    }

    @FunctionalInterface
    public interface PositiveMassVisitor<T> {
        void visit(T state, ExactProbability mass);
    }

    public record UnsupportedMechanism(String type, ExactProbability inboundMass) {
        public UnsupportedMechanism {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(inboundMass, "inboundMass");
            if (inboundMass.isZero()) throw new IllegalArgumentException("zero inbound mass");
        }

        public Diagnostic diagnostic() {
            return new Diagnostic(
                    "UNSUPPORTED_TYPE",
                    "Unsupported reachable mechanism " + type + " with mass " + inboundMass,
                    null,
                    "",
                    List.of());
        }
    }
}
