package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class DailyFlightSchedule {

    public DepartureOccurrence nextDeparture(RouteGraphEdge edge, Instant earliestDeparture) {
        LocalDate utcDate = earliestDeparture.atOffset(ZoneOffset.UTC).toLocalDate();
        Instant adjustedDeparture = utcDate
                .atTime(edge.adjustedDepartureUtc())
                .toInstant(ZoneOffset.UTC);
        if (adjustedDeparture.isBefore(earliestDeparture)) {
            adjustedDeparture = adjustedDeparture.plusSeconds(24 * 60 * 60);
        }
        Instant scheduledDeparture = adjustedDeparture.minusSeconds(Math.multiplyExact(edge.delayMinutes(), 60));
        return new DepartureOccurrence(scheduledDeparture, adjustedDeparture);
    }

    public record DepartureOccurrence(
            Instant scheduledDepartureInstant,
            Instant adjustedDepartureInstant) {
    }
}
