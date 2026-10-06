package com.airportrouteplanner.flightservice.companyroute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CompanyRouteTests {

    @Test
    void acceptsRouteNumberBoundariesAndNormalizesIdentifiers() {
        CompanyRoute firstRoute = route(1, "tp", "lis", "opo", 9L);
        CompanyRoute eighthRoute = route(8, "ba", "lhr", "jfk", 10L);

        assertThat(firstRoute.getRouteNumber()).isEqualTo(1);
        assertThat(eighthRoute.getRouteNumber()).isEqualTo(8);
        assertThat(firstRoute.getCompanyCode()).isEqualTo("TP");
        assertThat(firstRoute.getBaseAirportCode()).isEqualTo("LIS");
        assertThat(firstRoute.getDestinationAirportCode()).isEqualTo("OPO");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9})
    void rejectsRouteNumbersOutsideAllowedRange(int routeNumber) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> route(routeNumber, "TP", "LIS", "OPO", 9L))
                .withMessage("routeNumber must be between 1 and 8 inclusive");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "AB", "ABCD", "A1C"})
    void rejectsInvalidBaseAirportCodes(String airportCode) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> route(1, "TP", airportCode, "OPO", 9L))
                .withMessage("baseAirportCode must contain exactly three letters");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "AB", "ABCD", "A1C"})
    void rejectsInvalidDestinationAirportCodes(String airportCode) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> route(1, "TP", "LIS", airportCode, 9L))
                .withMessage("destinationAirportCode must contain exactly three letters");
    }

    @Test
    void rejectsIdenticalBaseAndDestinationAirportsAfterNormalization() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> route(1, "TP", "LIS", "lis", 9L))
                .withMessage("baseAirportCode and destinationAirportCode must be different");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void rejectsNonPositiveAircraftTypeIds(long aircraftTypeId) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> route(1, "TP", "LIS", "OPO", aircraftTypeId))
                .withMessage("aircraftTypeId must be greater than zero");
    }

    @Test
    void rejectsNullAircraftTypeId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> route(1, "TP", "LIS", "OPO", null))
                .withMessage("aircraftTypeId must be greater than zero");
    }

    private CompanyRoute route(
            int routeNumber,
            String companyCode,
            String baseAirportCode,
            String destinationAirportCode,
            Long aircraftTypeId) {
        return new CompanyRoute(
                routeNumber,
                companyCode,
                baseAirportCode,
                destinationAirportCode,
                aircraftTypeId,
                LocalTime.of(8, 30));
    }
}
