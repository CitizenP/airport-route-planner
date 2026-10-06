package com.airportrouteplanner.routeservice.integration;

import java.util.List;

public interface AirportDataProvider {

    List<AirportClientResponse> getAirports();
}
