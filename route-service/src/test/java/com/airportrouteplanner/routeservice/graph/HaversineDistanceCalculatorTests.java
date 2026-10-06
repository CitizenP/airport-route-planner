package com.airportrouteplanner.routeservice.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HaversineDistanceCalculatorTests {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void identicalCoordinatesHaveZeroDistance() {
        assertThat(calculator.calculateKilometres(38.7742, -9.1342, 38.7742, -9.1342)).isZero();
    }

    @Test
    void oneDegreeOfLongitudeAtEquatorIsApproximately111Point195Kilometres() {
        assertThat(calculator.calculateKilometres(0, 0, 0, 1)).isCloseTo(111.195, within(0.001));
    }

    @Test
    void distanceIsSymmetric() {
        double lisbonToPorto = calculator.calculateKilometres(38.7742, -9.1342, 41.2420, -8.6780);
        double portoToLisbon = calculator.calculateKilometres(41.2420, -8.6780, 38.7742, -9.1342);

        assertThat(lisbonToPorto).isEqualTo(portoToLisbon);
    }

    @Test
    void representativeJfkToHeathrowDistanceIsApproximately5540Kilometres() {
        double distance = calculator.calculateKilometres(40.6413, -73.7781, 51.4700, -0.4543);

        assertThat(distance).isCloseTo(5540.0, within(10.0));
    }

    @Test
    void rejectsNonFiniteAndOutOfRangeCoordinates() {
        assertThatThrownBy(() -> calculator.calculateKilometres(Double.NaN, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("origin latitude");
        assertThatThrownBy(() -> calculator.calculateKilometres(0, 181, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("origin longitude");
        assertThatThrownBy(() -> calculator.calculateKilometres(0, 0, -91, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destination latitude");
    }

    private static org.assertj.core.data.Offset<Double> within(double tolerance) {
        return org.assertj.core.data.Offset.offset(tolerance);
    }
}
