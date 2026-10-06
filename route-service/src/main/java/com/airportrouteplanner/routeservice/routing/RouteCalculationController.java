package com.airportrouteplanner.routeservice.routing;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
public class RouteCalculationController {

    private final RouteCalculationService routeCalculationService;

    public RouteCalculationController(RouteCalculationService routeCalculationService) {
        this.routeCalculationService = routeCalculationService;
    }

    @PostMapping("/calculate")
    public RouteCalculationResponse calculate(@RequestBody RouteCalculationRequest request) {
        return routeCalculationService.calculate(request);
    }
}
