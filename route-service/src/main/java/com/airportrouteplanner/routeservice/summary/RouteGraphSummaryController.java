package com.airportrouteplanner.routeservice.summary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route-graph/summary")
public class RouteGraphSummaryController {

    private final RouteGraphSummaryService routeGraphSummaryService;

    public RouteGraphSummaryController(RouteGraphSummaryService routeGraphSummaryService) {
        this.routeGraphSummaryService = routeGraphSummaryService;
    }

    @GetMapping
    public RouteGraphSummary getSummary() {
        return routeGraphSummaryService.getSummary();
    }
}
