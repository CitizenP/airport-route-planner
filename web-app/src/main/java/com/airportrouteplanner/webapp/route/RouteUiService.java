package com.airportrouteplanner.webapp.route;

import org.springframework.stereotype.Service;

@Service
public class RouteUiService {

    private final RouteDataProvider routeDataProvider;

    public RouteUiService(RouteDataProvider routeDataProvider) {
        this.routeDataProvider = routeDataProvider;
    }

    public RouteCalculationResponse calculate(RouteCalculationRequest request) {
        if (request != null && request.routeType() != RouteType.FASTEST) {
            request = new RouteCalculationRequest(
                    request.originAirportCode(),
                    request.destinationAirportCode(),
                    request.routeType(),
                    null);
        }
        return routeDataProvider.calculate(request);
    }
}
