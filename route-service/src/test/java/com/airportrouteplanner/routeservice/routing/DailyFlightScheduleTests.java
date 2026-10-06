package com.airportrouteplanner.routeservice.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.airportrouteplanner.routeservice.graph.FlightDirection;
import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DailyFlightScheduleTests {

    private final DailyFlightSchedule schedule = new DailyFlightSchedule();

    @Test
    void selectsDepartureLaterToday() {
        var occurrence = schedule.nextDeparture(edge("09:00", "09:00", 0, 0), instant("08:30"));
        assertThat(occurrence.adjustedDepartureInstant()).isEqualTo(instant("09:00"));
    }

    @Test
    void exactTimeDepartureIsCatchable() {
        var occurrence = schedule.nextDeparture(edge("09:00", "09:00", 0, 0), instant("09:00"));
        assertThat(occurrence.adjustedDepartureInstant()).isEqualTo(instant("09:00"));
    }

    @Test
    void missedDepartureRollsToTomorrow() {
        var occurrence = schedule.nextDeparture(edge("09:00", "09:00", 0, 0), instant("09:01"));
        assertThat(occurrence.adjustedDepartureInstant()).isEqualTo(Instant.parse("2026-10-21T09:00:00Z"));
    }

    @Test
    void midnightClockNaturallyUsesNextUtcDateWhenMissed() {
        var occurrence = schedule.nextDeparture(edge("00:00", "00:00", 0, 0), instant("23:59"));
        assertThat(occurrence.adjustedDepartureInstant()).isEqualTo(Instant.parse("2026-10-21T00:00:00Z"));
    }

    @Test
    void adjustedDepartureControlsAvailability() {
        var occurrence = schedule.nextDeparture(edge("14:00", "14:05", 0, 5), instant("14:01"));
        assertThat(occurrence.adjustedDepartureInstant()).isEqualTo(instant("14:05"));
        assertThat(occurrence.scheduledDepartureInstant()).isEqualTo(instant("14:00"));
    }

    @Test
    void crossMidnightDelayRetainsPreviousDayScheduledOccurrence() {
        var occurrence = schedule.nextDeparture(edge("23:55", "00:00", 1, 5), instant("23:56"));
        assertThat(occurrence.adjustedDepartureInstant()).isEqualTo(Instant.parse("2026-10-21T00:00:00Z"));
        assertThat(occurrence.scheduledDepartureInstant()).isEqualTo(Instant.parse("2026-10-20T23:55:00Z"));
    }

    private Instant instant(String time) {
        return Instant.parse("2026-10-20T" + time + ":00Z");
    }

    private RouteGraphEdge edge(String scheduled, String adjusted, int dayOffset, long delay) {
        return new RouteGraphEdge(
                1L, 1, FlightDirection.OUTBOUND, "CO", 1L, "AAA", "BBB",
                LocalTime.parse(scheduled), LocalTime.parse(adjusted), dayOffset, delay, 100);
    }
}
