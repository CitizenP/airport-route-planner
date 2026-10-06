package com.airportrouteplanner.flightservice.schedule;

import com.airportrouteplanner.flightservice.companyroute.FlightDirection;
import com.airportrouteplanner.flightservice.companyroute.FlightLeg;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RunwayScheduler {

    private static final int SECONDS_PER_DAY = 24 * 60 * 60;
    private static final int SLOT_SECONDS = 5 * 60;

    private static final Comparator<FlightLeg> ALLOCATION_ORDER = Comparator
            .comparing(FlightLeg::scheduledDepartureUtc)
            .thenComparing(FlightLeg::companyCode)
            .thenComparingInt(FlightLeg::routeNumber)
            .thenComparingInt(leg -> directionOrder(leg.direction()));

    public List<AdjustedFlightLeg> schedule(
            List<FlightLeg> flightLegs,
            Map<String, Integer> runwayCapacities) {
        validateCapacities(flightLegs, runwayCapacities);

        List<FlightLeg> orderedLegs = flightLegs.stream().sorted(ALLOCATION_ORDER).toList();
        Map<DepartureSlot, Integer> assignedDepartures = new HashMap<>();
        List<AdjustedFlightLeg> adjustedSchedule = new ArrayList<>(orderedLegs.size());

        for (FlightLeg leg : orderedLegs) {
            int runwayCount = runwayCapacities.get(leg.originAirportCode());
            int scheduledSecond = leg.scheduledDepartureUtc().toSecondOfDay();
            int candidateSecond = scheduledSecond;

            while (assignedDepartures.getOrDefault(
                    new DepartureSlot(leg.originAirportCode(), candidateSecond), 0) >= runwayCount) {
                candidateSecond += SLOT_SECONDS;
            }

            DepartureSlot assignedSlot = new DepartureSlot(leg.originAirportCode(), candidateSecond);
            assignedDepartures.merge(assignedSlot, 1, Integer::sum);
            long delayMinutes = (candidateSecond - scheduledSecond) / 60L;

            adjustedSchedule.add(new AdjustedFlightLeg(
                    leg.companyRouteId(),
                    leg.routeNumber(),
                    leg.direction(),
                    leg.companyCode(),
                    leg.aircraftTypeId(),
                    leg.originAirportCode(),
                    leg.destinationAirportCode(),
                    leg.scheduledDepartureUtc(),
                    LocalTime.ofSecondOfDay(candidateSecond % SECONDS_PER_DAY),
                    candidateSecond / SECONDS_PER_DAY,
                    delayMinutes));
        }

        return List.copyOf(adjustedSchedule);
    }

    private static void validateCapacities(
            List<FlightLeg> flightLegs,
            Map<String, Integer> runwayCapacities) {
        for (FlightLeg leg : flightLegs) {
            Integer runwayCount = runwayCapacities.get(leg.originAirportCode());
            if (runwayCount == null) {
                throw new IllegalArgumentException(
                        "missing runway capacity for origin airport " + leg.originAirportCode());
            }
            if (runwayCount <= 0) {
                throw new IllegalArgumentException(
                        "runway capacity must be greater than zero for origin airport "
                                + leg.originAirportCode());
            }
        }
    }

    private static int directionOrder(FlightDirection direction) {
        return switch (direction) {
            case OUTBOUND -> 0;
            case RETURN -> 1;
        };
    }

    private record DepartureSlot(String airportCode, int absoluteSecond) {
    }
}
