package com.airportrouteplanner.flightservice.airport;

import java.util.Map;

public interface AirportRunwayCapacityProvider {

    Map<String, Integer> getRunwayCapacities();
}
