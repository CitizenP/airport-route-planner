package com.airportrouteplanner.routeservice.integration;

import java.util.List;

public interface FlightCompanyDataProvider {

    List<FlightCompanyClientResponse> getFlightCompanies();
}
