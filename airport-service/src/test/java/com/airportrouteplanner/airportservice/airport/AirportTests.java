package com.airportrouteplanner.airportservice.airport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AirportTests {

    @ParameterizedTest
    @MethodSource("coordinatesOutsideValidRanges")
    void rejectsCoordinatesOutsideValidRanges(double latitude, double longitude, String message) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> airportAt(latitude, longitude))
                .withMessage(message);
    }

    private static Stream<Arguments> coordinatesOutsideValidRanges() {
        return Stream.of(
                Arguments.of(-90.0001, 0.0, "latitude must be between -90 and 90 inclusive"),
                Arguments.of(90.0001, 0.0, "latitude must be between -90 and 90 inclusive"),
                Arguments.of(0.0, -180.0001, "longitude must be between -180 and 180 inclusive"),
                Arguments.of(0.0, 180.0001, "longitude must be between -180 and 180 inclusive"));
    }

    @Test
    void acceptsInclusiveCoordinateBoundaries() {
        assertThat(airportAt(-90.0, -180.0))
                .extracting(Airport::getLatitude, Airport::getLongitude)
                .containsExactly(-90.0, -180.0);
        assertThat(airportAt(90.0, 180.0))
                .extracting(Airport::getLatitude, Airport::getLongitude)
                .containsExactly(90.0, 180.0);
    }

    private static Airport airportAt(double latitude, double longitude) {
        return new Airport(
                "TST", "Test Airport", "Test City", "Test Country",
                latitude, longitude, 1, 0);
    }
}
