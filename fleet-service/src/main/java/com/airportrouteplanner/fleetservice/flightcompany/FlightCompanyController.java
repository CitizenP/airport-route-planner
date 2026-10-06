package com.airportrouteplanner.fleetservice.flightcompany;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flight-companies")
public class FlightCompanyController {

    private final FlightCompanyService flightCompanyService;

    public FlightCompanyController(FlightCompanyService flightCompanyService) {
        this.flightCompanyService = flightCompanyService;
    }

    @GetMapping
    public List<FlightCompanyResponse> getAllFlightCompanies() {
        return flightCompanyService.getAllFlightCompanies();
    }

    @GetMapping("/{code}")
    public ResponseEntity<FlightCompanyResponse> getFlightCompany(@PathVariable String code) {
        return flightCompanyService.getFlightCompany(code)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
