package com.airportrouteplanner.routeservice.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class FlightDurationCalculatorTests {

    private final FlightDurationCalculator calculator = new FlightDurationCalculator();

    @Test
    void usesDistanceDividedByCruiseSpeedAtNanosecondPrecision() {
        Duration duration = calculator.calculate(465.719326, 880);

        assertThat(FlightDurationCalculator.toMinutes(duration)).isCloseTo(
                465.719326 / 880 * 60,
                org.assertj.core.data.Offset.offset(0.000000001));
    }

    @Test
    void doesNotRoundDurationToWholeMinutes() {
        Duration duration = calculator.calculate(100, 601);

        assertThat(FlightDurationCalculator.toMinutes(duration)).isNotEqualTo(10.0);
        assertThat(duration.toNanos() % 60_000_000_000L).isNotZero();
    }

    @Test
    void rejectsInvalidSpeed() {
        for (double speed : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> calculator.calculate(100, speed))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cruise speed");
        }
    }
}
