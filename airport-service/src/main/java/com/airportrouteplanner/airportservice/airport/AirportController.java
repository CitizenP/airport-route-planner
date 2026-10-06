package com.airportrouteplanner.airportservice.airport;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/airports")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @GetMapping
    public List<AirportResponse> getAllAirports() {
        return airportService.getAllAirports();
    }

    @GetMapping("/{iataCode}")
    public ResponseEntity<AirportResponse> getAirport(@PathVariable String iataCode) {
        return airportService.getAirport(iataCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
