package com.airportrouteplanner.routeservice.integration;

import java.util.List;

public interface AdjustedFlightLegDataProvider {

    List<AdjustedFlightLegClientResponse> getAdjustedFlightLegs();
}
