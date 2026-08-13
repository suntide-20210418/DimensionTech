package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RandomStateExecutor1201Test {
    @Test
    void repeatedRollsUseThePreviousRollContinuationState() {
        XoroshiroState1201 initial = new XoroshiroState1201(17L, 29L);
        var result = RandomStateExecutor1201.repeat(3, initial, state -> {
            var draw = state.nextInt(137);
            return new RandomStateExecutor1201.StepResult<>(draw.value(), draw.state());
        });

        var first = initial.nextInt(137);
        var second = first.state().nextInt(137);
        var third = second.state().nextInt(137);
        assertEquals(List.of(first.value(), second.value(), third.value()), result.values());
        assertEquals(third.state(), result.randomState());
    }
}
