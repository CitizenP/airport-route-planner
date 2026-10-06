package com.airportrouteplanner.airportservice.airport;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AirportService {

    private final AirportRepository airportRepository;

    public AirportService(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    public List<AirportResponse> getAllAirports() {
        return airportRepository.findAllByOrderByIataCodeAsc().stream()
                .map(AirportResponse::from)
                .toList();
    }

    public Optional<AirportResponse> getAirport(String iataCode) {
        String normalizedIataCode = iataCode.toUpperCase(Locale.ROOT);
        return airportRepository.findById(normalizedIataCode).map(AirportResponse::from);
    }
}
