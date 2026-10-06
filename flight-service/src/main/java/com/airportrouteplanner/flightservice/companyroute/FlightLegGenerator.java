package com.airportrouteplanner.flightservice.companyroute;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FlightLegGenerator {

    public List<FlightLeg> generate(CompanyRoute companyRoute) {
        FlightLeg outbound = new FlightLeg(
                companyRoute.getId(),
                companyRoute.getRouteNumber(),
                FlightDirection.OUTBOUND,
                companyRoute.getCompanyCode(),
                companyRoute.getAircraftTypeId(),
                companyRoute.getBaseAirportCode(),
                companyRoute.getDestinationAirportCode(),
                companyRoute.getScheduledOutboundDepartureUtc());

        FlightLeg returnLeg = new FlightLeg(
                companyRoute.getId(),
                companyRoute.getRouteNumber(),
                FlightDirection.RETURN,
                companyRoute.getCompanyCode(),
                companyRoute.getAircraftTypeId(),
                companyRoute.getDestinationAirportCode(),
                companyRoute.getBaseAirportCode(),
                companyRoute.getScheduledOutboundDepartureUtc().plusHours(12));

        return List.of(outbound, returnLeg);
    }
}
