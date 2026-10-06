package com.airportrouteplanner.fleetservice.flightcompany;

public record FlightCompanyResponse(
        String code,
        String name,
        String country,
        String baseAirportCode) {

    public static FlightCompanyResponse from(FlightCompany flightCompany) {
        return new FlightCompanyResponse(
                flightCompany.getCode(),
                flightCompany.getName(),
                flightCompany.getCountry(),
                flightCompany.getBaseAirportCode());
    }
}
