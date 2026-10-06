package com.airportrouteplanner.fleetservice.aircrafttype;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AircraftTypeTests {

    @ParameterizedTest
    @MethodSource("invalidDoubleValues")
    void rejectsInvalidDoubleValues(String fieldName, double value) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aircraftTypeWith(fieldName, value))
                .withMessageContaining(fieldName);
    }

    private static Stream<Arguments> invalidDoubleValues() {
        return Stream.of(
                Arguments.of("cruiseSpeedKmH", 0.0),
                Arguments.of("cruiseSpeedKmH", Double.NaN),
                Arguments.of("cruiseSpeedKmH", Double.POSITIVE_INFINITY),
                Arguments.of("maximumRangeKm", 0.0),
                Arguments.of("maximumRangeKm", Double.NaN),
                Arguments.of("fuelCapacityLitres", 0.0),
                Arguments.of("fuelCapacityLitres", Double.NaN),
                Arguments.of("fuelConsumptionLitresPerKm", 0.0),
                Arguments.of("fuelConsumptionLitresPerKm", Double.NaN),
                Arguments.of("fuelConsumptionPerPassenger", 0.0),
                Arguments.of("fuelConsumptionPerPassenger", Double.NaN));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositivePassengerCapacity(int passengerCapacity) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AircraftType(
                        "Test", "Model", 800.0, 5000.0, 20000.0,
                        3.5, passengerCapacity, 2.0))
                .withMessage("passengerCapacity must be greater than zero");
    }

    private static AircraftType aircraftTypeWith(String fieldName, double value) {
        double cruiseSpeedKmH = 800.0;
        double maximumRangeKm = 5000.0;
        double fuelCapacityLitres = 20000.0;
        double fuelConsumptionLitresPerKm = 3.5;
        double fuelConsumptionPerPassenger = 2.0;

        switch (fieldName) {
            case "cruiseSpeedKmH" -> cruiseSpeedKmH = value;
            case "maximumRangeKm" -> maximumRangeKm = value;
            case "fuelCapacityLitres" -> fuelCapacityLitres = value;
            case "fuelConsumptionLitresPerKm" -> fuelConsumptionLitresPerKm = value;
            case "fuelConsumptionPerPassenger" -> fuelConsumptionPerPassenger = value;
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        }

        return new AircraftType(
                "Test", "Model", cruiseSpeedKmH, maximumRangeKm, fuelCapacityLitres,
                fuelConsumptionLitresPerKm, 100, fuelConsumptionPerPassenger);
    }
}
