package com.airportrouteplanner.webapp.airport;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AirportUiService {

    private final AirportDataProvider airportDataProvider;
    private final UtcOffsetFormatter utcOffsetFormatter;

    public AirportUiService(
            AirportDataProvider airportDataProvider,
            UtcOffsetFormatter utcOffsetFormatter) {
        this.airportDataProvider = airportDataProvider;
        this.utcOffsetFormatter = utcOffsetFormatter;
    }

    public List<AirportUiResponse> getAirports() {
        return airportDataProvider.getAirports().stream()
                .map(airport -> AirportUiResponse.from(airport, utcOffsetFormatter))
                .sorted(Comparator.comparing(AirportUiResponse::iataCode))
                .toList();
    }
}
