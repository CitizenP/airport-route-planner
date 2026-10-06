package com.airportrouteplanner.fleetservice.aircrafttype;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AircraftTypeRepository extends JpaRepository<AircraftType, Long> {

    List<AircraftType> findAllByOrderByManufacturerAscModelAsc();
}
