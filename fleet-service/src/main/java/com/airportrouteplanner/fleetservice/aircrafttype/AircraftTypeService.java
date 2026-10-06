package com.airportrouteplanner.fleetservice.aircrafttype;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AircraftTypeService {

    private final AircraftTypeRepository aircraftTypeRepository;

    public AircraftTypeService(AircraftTypeRepository aircraftTypeRepository) {
        this.aircraftTypeRepository = aircraftTypeRepository;
    }

    public List<AircraftTypeResponse> getAllAircraftTypes() {
        return aircraftTypeRepository.findAllByOrderByManufacturerAscModelAsc().stream()
                .map(AircraftTypeResponse::from)
                .toList();
    }

    public Optional<AircraftTypeResponse> getAircraftType(Long id) {
        return aircraftTypeRepository.findById(id).map(AircraftTypeResponse::from);
    }
}
