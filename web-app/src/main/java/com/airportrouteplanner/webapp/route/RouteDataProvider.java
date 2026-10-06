package com.airportrouteplanner.webapp.route;

public interface RouteDataProvider {

    RouteCalculationResponse calculate(RouteCalculationRequest request);
}
