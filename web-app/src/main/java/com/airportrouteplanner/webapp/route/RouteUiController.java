package com.airportrouteplanner.webapp.route;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ui/routes")
public class RouteUiController {

    private final RouteUiService routeUiService;

    public RouteUiController(RouteUiService routeUiService) {
        this.routeUiService = routeUiService;
    }

    @PostMapping("/calculate")
    public RouteCalculationResponse calculate(@RequestBody RouteCalculationRequest request) {
        return routeUiService.calculate(request);
    }
}
