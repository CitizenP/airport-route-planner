package com.airportrouteplanner.fleetservice.aircrafttype;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aircraft-types")
public class AircraftTypeController {

    private final AircraftTypeService aircraftTypeService;

    public AircraftTypeController(AircraftTypeService aircraftTypeService) {
        this.aircraftTypeService = aircraftTypeService;
    }

    @GetMapping
    public List<AircraftTypeResponse> getAllAircraftTypes() {
        return aircraftTypeService.getAllAircraftTypes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AircraftTypeResponse> getAircraftType(@PathVariable Long id) {
        return aircraftTypeService.getAircraftType(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
