package com.airportrouteplanner.fleetservice.flightcompany;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightCompanyRepository extends JpaRepository<FlightCompany, String> {

    List<FlightCompany> findAllByOrderByCodeAsc();
}
