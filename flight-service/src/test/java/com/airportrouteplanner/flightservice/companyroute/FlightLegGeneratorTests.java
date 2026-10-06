package com.airportrouteplanner.flightservice.companyroute;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlightLegGeneratorTests {

    private final FlightLegGenerator generator = new FlightLegGenerator();

    @Test
    void generatesOutboundAndReversedReturnLegsInDeterministicOrder() {
        CompanyRoute route = new CompanyRoute(3, "TP", "LIS", "JFK", 9L, LocalTime.of(8, 30));

        List<FlightLeg> legs = generator.generate(route);

        assertThat(legs).hasSize(2);
        assertThat(legs).extracting(FlightLeg::direction)
                .containsExactly(FlightDirection.OUTBOUND, FlightDirection.RETURN);

        FlightLeg outbound = legs.get(0);
        assertThat(outbound.originAirportCode()).isEqualTo("LIS");
        assertThat(outbound.destinationAirportCode()).isEqualTo("JFK");
        assertThat(outbound.scheduledDepartureUtc()).isEqualTo(LocalTime.of(8, 30));

        FlightLeg returnLeg = legs.get(1);
        assertThat(returnLeg.originAirportCode()).isEqualTo("JFK");
        assertThat(returnLeg.destinationAirportCode()).isEqualTo("LIS");
        assertThat(returnLeg.scheduledDepartureUtc()).isEqualTo(LocalTime.of(20, 30));
    }

    @Test
    void returnDepartureWrapsAcrossMidnight() {
        CompanyRoute route = new CompanyRoute(1, "QR", "DOH", "KBL", 10L, LocalTime.of(18, 45));

        FlightLeg returnLeg = generator.generate(route).get(1);

        assertThat(returnLeg.scheduledDepartureUtc()).isEqualTo(LocalTime.of(6, 45));
    }
}
