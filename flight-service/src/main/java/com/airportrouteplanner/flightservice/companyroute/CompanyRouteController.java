package com.airportrouteplanner.flightservice.companyroute;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company-routes")
public class CompanyRouteController {

    private final CompanyRouteService companyRouteService;

    public CompanyRouteController(CompanyRouteService companyRouteService) {
        this.companyRouteService = companyRouteService;
    }

    @GetMapping
    public List<CompanyRouteResponse> getAllCompanyRoutes() {
        return companyRouteService.getAllCompanyRoutes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyRouteResponse> getCompanyRoute(@PathVariable Long id) {
        return companyRouteService.getCompanyRoute(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/flight-legs")
    public ResponseEntity<List<FlightLegResponse>> getFlightLegs(@PathVariable Long id) {
        return companyRouteService.getFlightLegs(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
