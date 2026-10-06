package com.airportrouteplanner.webapp.airport;

import java.util.List;

public interface AirportDataProvider {

    List<AirportClientResponse> getAirports();
}
