package com.airportrouteplanner.airportservice.airport;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AirportRepository extends JpaRepository<Airport, String> {

    List<Airport> findAllByOrderByIataCodeAsc();
}
