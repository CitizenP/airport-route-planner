package com.airportrouteplanner.flightservice.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.airportrouteplanner.flightservice.companyroute.FlightDirection;
import com.airportrouteplanner.flightservice.companyroute.FlightLeg;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunwaySchedulerTests {

    private final RunwayScheduler scheduler = new RunwayScheduler();

    @Test
    void noConflictLeavesDeparturesUnchanged() {
        List<AdjustedFlightLeg> result = scheduler.schedule(
                List.of(leg(1, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                        leg(2, "BB", 1, FlightDirection.OUTBOUND, "LIS", 9, 5)),
                Map.of("LIS", 1));

        assertThat(result).extracting(AdjustedFlightLeg::adjustedDepartureUtc)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 5));
        assertThat(result).extracting(AdjustedFlightLeg::delayMinutes).containsOnly(0L);
    }

    @Test
    void departuresEqualToRunwayCountRemainUnchanged() {
        List<AdjustedFlightLeg> result = scheduler.schedule(
                List.of(leg(1, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                        leg(2, "BB", 1, FlightDirection.OUTBOUND, "LIS", 9, 0)),
                Map.of("LIS", 2));

        assertThat(result).extracting(AdjustedFlightLeg::adjustedDepartureUtc)
                .containsOnly(LocalTime.of(9, 0));
        assertThat(result).extracting(AdjustedFlightLeg::delayMinutes).containsOnly(0L);
    }

    @Test
    void excessDeparturesMoveThroughCascadingFiveMinuteSlots() {
        List<AdjustedFlightLeg> result = scheduler.schedule(
                List.of(leg(1, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                        leg(2, "BB", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                        leg(3, "CC", 1, FlightDirection.OUTBOUND, "LIS", 9, 5)),
                Map.of("LIS", 1));

        assertThat(result).extracting(AdjustedFlightLeg::adjustedDepartureUtc)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 5), LocalTime.of(9, 10));
        assertThat(result).extracting(AdjustedFlightLeg::delayMinutes).containsExactly(0L, 5L, 5L);
    }

    @Test
    void twoRunwaysAllocateFiveFlightsAcrossTwoTwoOneSlots() {
        List<FlightLeg> legs = List.of(
                leg(1, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                leg(2, "BB", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                leg(3, "CC", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                leg(4, "DD", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                leg(5, "EE", 1, FlightDirection.OUTBOUND, "LIS", 9, 0));

        assertThat(scheduler.schedule(legs, Map.of("LIS", 2)))
                .extracting(AdjustedFlightLeg::adjustedDepartureUtc)
                .containsExactly(
                        LocalTime.of(9, 0), LocalTime.of(9, 0),
                        LocalTime.of(9, 5), LocalTime.of(9, 5),
                        LocalTime.of(9, 10));
    }

    @Test
    void companyCodeDeterminesPriorityWhenTimesTie() {
        List<AdjustedFlightLeg> result = scheduler.schedule(
                List.of(leg(1, "ZZ", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                        leg(2, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0)),
                Map.of("LIS", 1));

        assertThat(result).extracting(AdjustedFlightLeg::companyCode).containsExactly("AA", "ZZ");
        assertThat(result).extracting(AdjustedFlightLeg::delayMinutes).containsExactly(0L, 5L);
    }

    @Test
    void routeNumberDeterminesPriorityWhenTimeAndCompanyTie() {
        List<AdjustedFlightLeg> result = scheduler.schedule(
                List.of(leg(1, "AA", 2, FlightDirection.OUTBOUND, "LIS", 9, 0),
                        leg(2, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0)),
                Map.of("LIS", 1));

        assertThat(result).extracting(AdjustedFlightLeg::routeNumber).containsExactly(1, 2);
    }

    @Test
    void outboundPrecedesReturnWhenAllEarlierKeysTie() {
        List<AdjustedFlightLeg> result = scheduler.schedule(
                List.of(leg(1, "AA", 1, FlightDirection.RETURN, "LIS", 9, 0),
                        leg(2, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0)),
                Map.of("LIS", 1));

        assertThat(result).extracting(AdjustedFlightLeg::direction)
                .containsExactly(FlightDirection.OUTBOUND, FlightDirection.RETURN);
    }

    @Test
    void independentAirportsDoNotShareCapacity() {
        List<AdjustedFlightLeg> result = scheduler.schedule(
                List.of(leg(1, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0),
                        leg(2, "BB", 1, FlightDirection.OUTBOUND, "OPO", 9, 0)),
                Map.of("LIS", 1, "OPO", 1));

        assertThat(result).extracting(AdjustedFlightLeg::delayMinutes).containsOnly(0L);
    }

    @Test
    void missingAirportCapacityFailsClearly() {
        assertThatThrownBy(() -> scheduler.schedule(
                        List.of(leg(1, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0)),
                        Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("missing runway capacity for origin airport LIS");
    }

    @Test
    void nonPositiveAirportCapacityFailsClearly() {
        FlightLeg leg = leg(1, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0);

        assertThatThrownBy(() -> scheduler.schedule(List.of(leg), Map.of("LIS", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
        assertThatThrownBy(() -> scheduler.schedule(List.of(leg), Map.of("LIS", -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void delayAcrossMidnightRetainsDayOffset() {
        List<AdjustedFlightLeg> result = scheduler.schedule(
                List.of(leg(1, "AA", 1, FlightDirection.OUTBOUND, "LIS", 23, 55),
                        leg(2, "BB", 1, FlightDirection.OUTBOUND, "LIS", 23, 55)),
                Map.of("LIS", 1));

        AdjustedFlightLeg delayed = result.get(1);
        assertThat(delayed.adjustedDepartureUtc()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(delayed.adjustedDepartureDayOffset()).isEqualTo(1);
        assertThat(delayed.delayMinutes()).isEqualTo(5);
    }

    @Test
    void inputOrderDoesNotAffectOutput() {
        List<FlightLeg> original = List.of(
                leg(1, "CC", 1, FlightDirection.RETURN, "LIS", 9, 5),
                leg(2, "AA", 2, FlightDirection.OUTBOUND, "LIS", 9, 0),
                leg(3, "AA", 1, FlightDirection.OUTBOUND, "LIS", 9, 0));
        List<FlightLeg> reversed = new ArrayList<>(original);
        Collections.reverse(reversed);

        assertThat(scheduler.schedule(original, Map.of("LIS", 1)))
                .containsExactlyElementsOf(scheduler.schedule(reversed, Map.of("LIS", 1)));
    }

    private FlightLeg leg(
            long id,
            String companyCode,
            int routeNumber,
            FlightDirection direction,
            String origin,
            int hour,
            int minute) {
        return new FlightLeg(
                id,
                routeNumber,
                direction,
                companyCode,
                1L,
                origin,
                "JFK",
                LocalTime.of(hour, minute));
    }
}
