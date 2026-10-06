package com.airportrouteplanner.webapp.airport;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ui/airports")
public class AirportUiController {

    private final AirportUiService airportUiService;

    public AirportUiController(AirportUiService airportUiService) {
        this.airportUiService = airportUiService;
    }

    @GetMapping
    public List<AirportUiResponse> getAirports() {
        return airportUiService.getAirports();
    }
}
