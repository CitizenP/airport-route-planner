package com.airportrouteplanner.fleetservice.flightcompany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class FlightCompanyTests {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "AB", "ABCD", "lhr", "A1C"})
    void rejectsInvalidBaseAirportCodes(String baseAirportCode) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FlightCompany(
                        "TC", "Test Company", "Test Country", baseAirportCode))
                .withMessage("baseAirportCode must contain exactly three uppercase letters");
    }

    @Test
    void acceptsThreeUppercaseLettersAsBaseAirportCode() {
        FlightCompany flightCompany = new FlightCompany(
                "TC", "Test Company", "Test Country", "LHR");

        assertThat(flightCompany.getBaseAirportCode()).isEqualTo("LHR");
    }
}
