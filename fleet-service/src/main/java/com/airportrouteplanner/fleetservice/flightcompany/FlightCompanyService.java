package com.airportrouteplanner.fleetservice.flightcompany;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FlightCompanyService {

    private final FlightCompanyRepository flightCompanyRepository;

    public FlightCompanyService(FlightCompanyRepository flightCompanyRepository) {
        this.flightCompanyRepository = flightCompanyRepository;
    }

    public List<FlightCompanyResponse> getAllFlightCompanies() {
        return flightCompanyRepository.findAllByOrderByCodeAsc().stream()
                .map(FlightCompanyResponse::from)
                .toList();
    }

    public Optional<FlightCompanyResponse> getFlightCompany(String code) {
        String normalizedCode = code.toUpperCase(Locale.ROOT);
        return flightCompanyRepository.findById(normalizedCode).map(FlightCompanyResponse::from);
    }
}
