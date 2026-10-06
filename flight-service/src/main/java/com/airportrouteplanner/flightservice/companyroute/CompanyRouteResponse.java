package com.airportrouteplanner.flightservice.companyroute;

import java.time.LocalTime;

public record CompanyRouteResponse(
        Long id,
        int routeNumber,
        String companyCode,
        String baseAirportCode,
        String destinationAirportCode,
        Long aircraftTypeId,
        LocalTime scheduledOutboundDepartureUtc) {

    public static CompanyRouteResponse from(CompanyRoute companyRoute) {
        return new CompanyRouteResponse(
                companyRoute.getId(),
                companyRoute.getRouteNumber(),
                companyRoute.getCompanyCode(),
                companyRoute.getBaseAirportCode(),
                companyRoute.getDestinationAirportCode(),
                companyRoute.getAircraftTypeId(),
                companyRoute.getScheduledOutboundDepartureUtc());
    }
}
