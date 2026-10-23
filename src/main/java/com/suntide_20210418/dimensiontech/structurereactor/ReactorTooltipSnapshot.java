package com.suntide_20210418.dimensiontech.structurereactor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Server-authoritative recipe details needed by the reactor resource tooltips. */
public record ReactorTooltipSnapshot(
        List<ItemStack> fragmentCandidates,
        int fragmentCandidateTotal,
        int fragmentRequiredCount,
        List<ItemStack> operationCandidates,
        int operationCandidateTotal,
        boolean operationRequirementKnown,
        int operationStepIndex,
        int operationStepCount,
        int inputRequiredAmount,
        int outputExpectedAmount,
        Fluid expectedInputFluid,
        Fluid expectedOutputFluid,
        List<StructureReactorCycle.Resolution> stateOutcomes,
        int revision) {
    public static final int MAX_CANDIDATES = 6;
    public static final int MAX_CANDIDATE_TOTAL = 4096;

    public ReactorTooltipSnapshot {
        fragmentCandidates = copyCandidates(fragmentCandidates);
        operationCandidates = copyCandidates(operationCandidates);
        fragmentCandidateTotal = boundedTotal(fragmentCandidates, fragmentCandidateTotal);
        operationCandidateTotal = boundedTotal(operationCandidates, operationCandidateTotal);
        fragmentRequiredCount = Math.max(0, fragmentRequiredCount);
        operationStepIndex = Math.max(0, operationStepIndex);
        operationStepCount = Math.max(0, operationStepCount);
        inputRequiredAmount = Math.max(0, inputRequiredAmount);
        outputExpectedAmount = Math.max(0, outputExpectedAmount);
        expectedInputFluid = expectedInputFluid == null ? Fluids.EMPTY : expectedInputFluid;
        expectedOutputFluid = expectedOutputFluid == null ? Fluids.EMPTY : expectedOutputFluid;
        stateOutcomes = stateOutcomes == null ? List.of() : List.copyOf(stateOutcomes);
    }

    public static ReactorTooltipSnapshot empty() {
        return new ReactorTooltipSnapshot(
                List.of(),
                0,
                0,
                List.of(),
                0,
                false,
                0,
                0,
                0,
                0,
                Fluids.EMPTY,
                Fluids.EMPTY,
                List.of(),
                -1);
    }

    /** Compares displayed data while deliberately ignoring the transport revision. */
    public boolean contentEquals(ReactorTooltipSnapshot other) {
        if (other == null) return false;
        return fragmentRequiredCount == other.fragmentRequiredCount
                && fragmentCandidateTotal == other.fragmentCandidateTotal
                && operationCandidateTotal == other.operationCandidateTotal
                && operationRequirementKnown == other.operationRequirementKnown
                && operationStepIndex == other.operationStepIndex
                && operationStepCount == other.operationStepCount
                && inputRequiredAmount == other.inputRequiredAmount
                && outputExpectedAmount == other.outputExpectedAmount
                && expectedInputFluid == other.expectedInputFluid
                && expectedOutputFluid == other.expectedOutputFluid
                && stateOutcomes.equals(other.stateOutcomes)
                && sameStacks(fragmentCandidates, other.fragmentCandidates)
                && sameStacks(operationCandidates, other.operationCandidates);
    }

    public ReactorTooltipSnapshot withRevision(int value) {
        return new ReactorTooltipSnapshot(
                fragmentCandidates,
                fragmentCandidateTotal,
                fragmentRequiredCount,
                operationCandidates,
                operationCandidateTotal,
                operationRequirementKnown,
                operationStepIndex,
                operationStepCount,
                inputRequiredAmount,
                outputExpectedAmount,
                expectedInputFluid,
                expectedOutputFluid,
                stateOutcomes,
                value);
    }

    private static List<ItemStack> copyCandidates(List<ItemStack> source) {
        List<ItemStack> result = new ArrayList<>();
        if (source != null) {
            for (ItemStack stack : source) {
                if (stack != null && !stack.isEmpty() && result.size() < MAX_CANDIDATES)
                    result.add(stack.copy());
            }
        }
        return List.copyOf(result);
    }

    private static int boundedTotal(List<ItemStack> candidates, int total) {
        return Math.max(candidates.size(), Math.min(MAX_CANDIDATE_TOTAL, Math.max(0, total)));
    }

    private static boolean sameStacks(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) return false;
        for (int index = 0; index < first.size(); index++) {
            if (!ItemStack.isSameItemSameTags(first.get(index), second.get(index))) return false;
        }
        return true;
    }
}
